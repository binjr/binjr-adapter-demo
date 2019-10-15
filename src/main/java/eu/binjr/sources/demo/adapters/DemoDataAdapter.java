/*
 *    Copyright 2019 Frederic Thevenet
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

import eu.binjr.core.data.adapters.BaseDataAdapter;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.timeseries.DoubleTimeSeriesProcessor;
import eu.binjr.core.data.timeseries.TimeSeriesProcessor;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.TimeSeriesInfo;
import eu.binjr.core.data.workspace.UnitPrefixes;
import eu.binjr.sources.demo.jrds.JrdsDiskImage;
import eu.binjr.sources.demo.jrds.ReadOnlyProbeClassResolver;
import eu.binjr.sources.demo.jrds.SeriesBindings;
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
public class DemoDataAdapter extends BaseDataAdapter {
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
            this.archivePath = FileSystems.newFileSystem(Path.of(jarFileUri), null).getPath("/eu/binjr/demo/data/demoJrdsImg/");
        } catch (IOException | URISyntaxException e) {
            throw new DataAdapterException("Cannot open jrds image: " + e.getMessage(), e);
        } catch (FileSystemNotFoundException e) {
            throw new DataAdapterException("Cannot open jrds image: " + e.getMessage(), e);
            //this.archivePath = getClass().getResource("/eu/binjr)
        }
        //   this.archivePath = Path.of("C:\\Users\\ftt2\\sources\\fthevenet\\binjr-adapter-demo\\data\\demoJrdsImg\\demoJrdsImg.zip");
    }

    /**
     * Initializes a new instance of the {@link DemoDataAdapter} class from the provided {@link Path}
     *
     * @param archivePath the {@link Path} from which to load the archive.
     * @throws DataAdapterException if an error occurs while loading the archive.
     */
    public DemoDataAdapter(Path archivePath) throws DataAdapterException {
        super();
    }
//        this.
////        try {
////            URI archiveUri =
////            this.archivePath = Path.of(archiveUri);
////        } catch (URISyntaxException e) {
////            throw new DataAdapterException("Cannot open jrds image: " + e.getMessage(), e);
////        } catch (FileSystemNotFoundException e) {
////            try {
////                URI uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
////                this.archivePath = FileSystems.newFileSystem(Path.of(uri), null).getPath("/eu/binjr/demo/data/demoJrdsImg.zip");
////            } catch (IOException | URISyntaxException ex) {
////                throw new DataAdapterException("Cannot open jrds image: " + e.getMessage(), e);
////            }
////        }
////        Map<String, String> params = new HashMap<>();
////        params.put("archivePath", this.archivePath.toString());
////
////        loadParams(params);
//    }

    @Override
    public Map<String, String> getParams() {
        return new HashMap<>();
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
    }

    @Override
    public FilterableTreeItem<TimeSeriesBinding> getBindingTree() throws DataAdapterException {
        this.hostsList = new HostsList();
        this.hostsList.setProbeClassResolver(new ReadOnlyProbeClassResolver(propertiesManager.extensionClassLoader))
                .configure(propertiesManager);
        var tree = new FilterableTreeItem<>(
                new TimeSeriesBinding(
                        "",
                        "/",
                        null,
                        getSourceName(),
                        UnitPrefixes.METRIC,
                        ChartType.STACKED,
                        "-",
                        "/" + getSourceName(), this));
        for (GraphTree child : hostsList.getGraphTreeByHost().getChildsMap().values()) {
            attachNode(tree, child, child.getChildsMap());
        }
        return tree;
    }

    @Override
    public Map<TimeSeriesInfo, TimeSeriesProcessor> fetchData(String path,
                                                              Instant start,
                                                              Instant end,
                                                              List<TimeSeriesInfo> seriesInfos,
                                                              boolean bypassCache) throws DataAdapterException {
        int sepPos = path.indexOf("?");
        Map<TimeSeriesInfo, TimeSeriesProcessor> result = new HashMap<>();
        if (sepPos >= 0) {
            String graphTreePath = path.substring(0, sepPos);
            String graphNodeKey = path.substring(sepPos + 1);
            var graphTree = hostsList.getGraphTreeByHost().getById(graphTreePath.hashCode());
            var graphNode = graphTree.getGraphsSet().get(graphNodeKey);
            ExtractInfo ei = ExtractInfo.get().make(ConsolFun.AVERAGE).make(Date.from(start), Date.from(end));
            try (Extractor ex = graphNode.getProbe().fetchData()) {
                DataProcessor dp = ei.getDataProcessor();
                for (GraphDesc.DsDesc ds : graphNode.getGraphDesc().getGraphElements()) {
                    if (ds.graphType != GraphDesc.GraphType.COMMENT && ds.graphType != GraphDesc.GraphType.LEGEND) {
                        if (ds.rpn != null) {
                            dp.addDatasource(ds.name, ds.rpn);
                        } else if (ds.dsName != null) {
                            ex.addSource(ds.name, ds.dsName);
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

    private void attachNode(FilterableTreeItem<TimeSeriesBinding> tree, GraphTree
            node, Map<String, GraphTree> nodes) {
        String currentPath = node.getPath();
        FilterableTreeItem<TimeSeriesBinding> branch = new FilterableTreeItem<>(
                SeriesBindings.of(
                        tree.getValue().getTreeHierarchy(),
                        node.getName(),
                        currentPath,
                        this));
        for (var child : node.getChildsMap().values()) {
            attachNode(branch, child, nodes);
        }
        if (node.getGraphsSet() != null) {
            for (var gn : node.getGraphsSet().entrySet()) {
                var leaf = new FilterableTreeItem<>(
                        SeriesBindings.of(
                                branch.getValue().getTreeHierarchy(),
                                gn.getKey(),
                                gn.getValue().getGraphDesc(),
                                currentPath + "?" + gn.getKey(),
                                this));
                branch.getInternalChildren().add(leaf);
                for (var ds : gn.getValue().getGraphDesc().getGraphElements()) {
                    if (ds.graphType != GraphDesc.GraphType.COMMENT &&
                            ds.graphType != GraphDesc.GraphType.NONE &&
                            ds.legend != null) {
                        leaf.getInternalChildren().add(new FilterableTreeItem<>(
                                SeriesBindings.of(
                                        leaf.getValue().getTreeHierarchy(),
                                        gn.getValue().getGraphDesc(),
                                        ds,
                                        currentPath + "?" + gn.getKey(),
                                        this)));
                    }
                }
            }
        }
        tree.getInternalChildren().add(branch);
    }
}