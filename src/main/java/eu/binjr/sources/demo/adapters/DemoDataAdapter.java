/*
 *    Copyright 2019-2020 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.demo.adapters;

import eu.binjr.common.javafx.controls.TimeRange;
import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.DataAdapterInfo;
import eu.binjr.core.data.adapters.SourceBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.sources.demo.jrds.JrdsBindingBuilder;
import eu.binjr.sources.demo.jrds.JrdsDiskImage;
import eu.binjr.sources.demo.jrds.ReadOnlyProbeClassResolver;
import javafx.scene.chart.XYChart;
import jrds.GraphDesc;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.store.ExtractInfo;
import jrds.store.Extractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdDb;
import org.rrd4j.data.DataProcessor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.jar.JarFile;

/**
 * A demonstration {@link eu.binjr.core.data.adapters.DataAdapter} implementation.
 *
 * @author Frederic Thevenet
 */
public class DemoDataAdapter extends BaseDataAdapter<Double> {
    public static final String JRDS_IMAGE_PATH = "/eu/binjr/demo/data/demoJrdsImg/";
    private static final Logger logger = LogManager.getLogger(DemoDataAdapter.class);
    private Path archivePath;
    private JrdsDiskImage jrdsImage;
    private PropertiesManager propertiesManager;
    private List<JarURLConnection> loadedJarConnections = new ArrayList<>();
    private HostsList hostsList;

    /**
     * Initializes a new instance of the {@link DemoDataAdapter} class.
     *
     * @throws DataAdapterException if an error occurs while loading the archive.
     */
    public DemoDataAdapter() throws DataAdapterException {
        super();
        try {
            final URI jarFileUri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            if (jarFileUri.toString().endsWith(".jar")) {
                this.archivePath = FileSystems.newFileSystem(Path.of(jarFileUri), (ClassLoader)null).getPath(JRDS_IMAGE_PATH);
            } else {
                this.archivePath = Path.of(getClass().getResource(JRDS_IMAGE_PATH).toURI());
            }
        } catch (IOException e) {
            throw new DataAdapterException("An error occurred while creating path for jrds image: " + e.getMessage(), e);
        } catch (FileSystemNotFoundException e) {
            throw new DataAdapterException("Cannot find jrds image in JAR: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new DataAdapterException("Invalid URI for jrds image: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getParams() {
        return new HashMap<>();
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        this.hostsList = new HostsList();
        this.hostsList.setProbeClassResolverSource(ReadOnlyProbeClassResolver::new).configure(propertiesManager);

        var tree = new FilterableTreeItem<SourceBinding>(
                new JrdsBindingBuilder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        for (GraphTree child : hostsList.getGraphTreeByHost().getChildsMap().values()) {
            attachNode(tree, child, child.getChildsMap());
        }
        return tree;
    }


    @Override
    public TimeRange getInitialTimeRange(String path, List<TimeSeriesInfo<Double>> seriesInfo) throws DataAdapterException {
        ZonedDateTime latest = ZonedDateTime.now();
        int sepPos = path.indexOf("?");
        if (sepPos >= 0) {
            String graphTreePath = path.substring(0, sepPos);
            String graphNodeKey = path.substring(sepPos + 1);
            var graphTree = hostsList.getGraphTreeByHost().getById(graphTreePath.hashCode());
            var graphNode = graphTree.getGraphsSet().get(graphNodeKey);
            latest = ZonedDateTime.ofInstant(graphNode.getProbe().getLastUpdate().toInstant(), getTimeZoneId());
        }
        return TimeRange.of(latest.minusHours(24), latest);
    }


    @Override
    public Map<TimeSeriesInfo<Double> ,TimeSeriesProcessor<Double>> fetchData(String path,
                                                                              Instant start,
                                                                              Instant end,
                                                                              List<TimeSeriesInfo<Double>> seriesInfos,
                                                                              boolean bypassCache) throws DataAdapterException {
        int sepPos = path.indexOf("?");
        Map<TimeSeriesInfo<Double>, TimeSeriesProcessor<Double>> result = new HashMap<>();
        if (sepPos >= 0) {
            String graphTreePath = path.substring(0, sepPos);
            String graphNodeKey = path.substring(sepPos + 1);
            var graphTree = hostsList.getGraphTreeByHost().getById(graphTreePath.hashCode());
            var graphNode = graphTree.getGraphsSet().get(graphNodeKey);
            ExtractInfo ei = ExtractInfo.builder()
                    .cf(ConsolFun.AVERAGE)
                    .start(start)
                    .end(end)
                    .build();
            try (Extractor ex = graphNode.getProbe().fetchData()) {
                try (RrdDb rrd = (RrdDb) graphNode.getProbe().getMainStore().getStoreObject()) {
                    DataProcessor dp = ei.getDataProcessor();
                    for (GraphDesc.DsDesc ds : graphNode.getGraphDesc().getGraphElements()) {
                        if (ds.graphType != GraphDesc.GraphType.COMMENT && ds.graphType != GraphDesc.GraphType.LEGEND) {
                            if (ds.rpn != null) {
                                dp.addDatasource(ds.name, ds.rpn);
                            } else if (ds.dsName != null) {
                                if (isDsPresent(rrd, ds.dsName)) {
                                    ex.addSource(ds.name, ds.dsName);
                                }
                            }
                        }
                    }
                    ex.fill(dp, ei);
                    dp.processData();
                    for (var info : seriesInfos) {
                        var data = dp.getValues(info.getBinding().getLabel());
                        var timestamps = dp.getTimestamps();
                        var samples = new ArrayList<XYChart.Data<ZonedDateTime, Double>>();
                        for (int i = 0; i < data.length - 1; i++) {
                            samples.add(new XYChart.Data<>(ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamps[i]),
                                    this.getTimeZoneId()), data[i]));
                        }
                        var seriesProc = new DoubleTimeSeriesProcessor();
                        seriesProc.setData(samples);
                        result.put(info, seriesProc);
                    }
                }
            } catch (IOException e) {
                throw new DataAdapterException("Error extracting data: " + e.getMessage(), e);
            }
        }
        return result;
    }

    @Override
    public String getEncoding() {
        return "utf-8";
    }

    @Override
    public ZoneId getTimeZoneId() {
        return ZoneId.systemDefault();
    }

    @Override
    public String getSourceName() {
        return "[Demo]";
    }

    @Override
    public void onStart() throws DataAdapterException {
        try {
            this.jrdsImage = JrdsDiskImage.of(archivePath);
            Properties properties = new Properties();
            try (Reader r = new FileReader(jrdsImage.getConfigdir().resolve("jrds.properties").toFile())) {
                properties.load(r);
            }
            properties.setProperty("readonly", "true");
            properties.setProperty("autocreate", "false");
            properties.setProperty("nologging", "true");
            properties.setProperty("loglevel", "info");
            properties.setProperty("configdir", jrdsImage.getConfigdir().toString());
            properties.setProperty("rrddir", jrdsImage.getRrdDir().toString());
            properties.setProperty("tmpdir", jrdsImage.getTempDir().toString());
            properties.setProperty("libspath", jrdsImage.getLibsPath().toString());
            this.propertiesManager = new PropertiesManager();
            this.propertiesManager.join(properties);
            this.propertiesManager.importSystemProps();
            this.propertiesManager.update();

        } catch (Exception e) {
            throw new DataAdapterException("Could not open jrds image from " + archivePath + ": " + e.getMessage(), e);
        }
        super.onStart();
    }

    @Override
    public boolean isSortingRequired() {
        return false;
    }

    @Override
    public void close() {
        loadedJarConnections.forEach((jarURLConnection) -> {
            try {
                JarFile jarFile = jarURLConnection.getJarFile();
                logger.trace(() -> "Attempting to close jar file: " + jarFile.getName());
                jarFile.close();
            } catch (IOException e) {
                logger.error("Could not close JarFile:" + e.getMessage());
                logger.debug(() -> "Stack trace", e);
            }
        });
        loadedJarConnections.clear();

        if (jrdsImage != null) {
            this.jrdsImage.close();
        }
        super.close();
    }

    private void attachNode(FilterableTreeItem<SourceBinding> tree, GraphTree
            node, Map<String, GraphTree> nodes) {
        String currentPath = node.getPath();
        FilterableTreeItem<SourceBinding> branch = new FilterableTreeItem<>(
                new JrdsBindingBuilder()
                        .withLabel(node.getName())
                        .withPath(currentPath)
                        .withParent(tree.getValue())
                        .withAdapter(this)
                        .build());
        for (var child : node.getChildsMap().values()) {
            attachNode(branch, child, nodes);
        }
        if (node.getGraphsSet() != null) {
            for (var gn : node.getGraphsSet().entrySet()) {
                RrdDb rrd = null;
                try {
                    rrd = (RrdDb) gn.getValue().getProbe().getMainStore().getStoreObject();
                } catch (Exception e) {
                    logger.error("Error accessing RRD data store: " + e.getMessage());
                    logger.debug("Call stack", e);
                    continue;
                }
                logger.trace(() -> "Graph desc=" + gn.getValue().getGraphDesc());
                FilterableTreeItem<SourceBinding> leaf = new FilterableTreeItem<>(
                        new JrdsBindingBuilder()
                                .withGraphDesc(gn.getValue().getGraphDesc())
                                .withParent(branch.getValue())
                                .withPath(currentPath + "?" + gn.getKey())
                                .withAdapter(this)
                                .withLabel(gn.getKey())
                                .build());
                branch.getInternalChildren().add(leaf);
                for (var ds : gn.getValue().getGraphDesc().getGraphElements()) {
                    if (ds.graphType != GraphDesc.GraphType.COMMENT &&
                            ds.graphType != GraphDesc.GraphType.NONE &&
                            ds.legend != null && isDsPresent(rrd, ds.dsName)) {
                        leaf.getInternalChildren().add(new FilterableTreeItem<>(
                                new JrdsBindingBuilder()
                                        .withGraphDesc(gn.getValue().getGraphDesc(), ds)
                                        .withParent(leaf.getValue())
                                        .withPath(currentPath + "?" + gn.getKey())
                                        .withAdapter(this)
                                        .build()));
                    }
                }
            }
        }
        tree.getInternalChildren().add(branch);
    }

    private boolean isDsPresent(RrdDb rrd, String dsName) {
        try {
            if (dsName != null && rrd != null && !rrd.containsDs(dsName)) {
                logger.warn("DataSource " + dsName + " doesn't exists in RRD");
                return false;
            }
        } catch (IOException e) {
            logger.error("Error validating DataSource " + dsName + " : " + e.getMessage());
            logger.debug("Call stack", e);
            return false;
        }
        return true;
    }
}
