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

package eu.binjr.sources.demo.jrds;

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;
import jrds.GraphDesc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * This class provides an implementation of {@link TimeSeriesBinding} for bindings targeting JRDS.
 *
 * @author Frederic Thevenet
 */
public final class SeriesBindings {
    private static final Logger logger = LogManager.getLogger(SeriesBindings.class);
    private static final UnitPrefixes DEFAULT_PREFIX = UnitPrefixes.BINARY;

    /**
     * Initializes a new instance of the {@link SeriesBindings} class
     */
    public SeriesBindings() {
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class.
     *
     * @param parentName the name of the parent tree node.
     * @param label      the name of the data store.
     * @param path       the id for the graph/probe
     * @param adapter    the {@link DataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public static TimeSeriesBinding of(String parentName, String label, String path, DataAdapter adapter) {
        return new TimeSeriesBinding(
                label,
                path,
                null,
                label,
                DEFAULT_PREFIX,
                ChartType.STACKED,
                "-",
                parentName + "/" + label, adapter);
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class with the following parameters
     *
     * @param parentName the name of the parent tree node.
     * @param legend     the legend for the timeseries
     * @param graphdesc  the graph description from JRDS
     * @param path       the id of the JRDS graph
     * @param adapter    the {@link DataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public static TimeSeriesBinding of(String parentName, String legend, GraphDesc graphdesc, String path, DataAdapter adapter) {
        final String label;
        final UnitPrefixes prefix;
        final ChartType graphType;
        final String unitName;
        label = isNullOrEmpty(graphdesc.getName()) ?
                (isNullOrEmpty(graphdesc.getGraphName()) ?
                        "???" : graphdesc.getGraphName()) : graphdesc.getName();

        graphType = ChartType.STACKED;
        prefix = graphdesc.isSiUnit()? UnitPrefixes.METRIC:UnitPrefixes.BINARY;
        unitName = graphdesc.getVerticalLabel();
        return new TimeSeriesBinding(label, path, null, legend, prefix, graphType, unitName, parentName + "/" + legend, adapter);
    }

    /**
     * Creates a new instance of the {@link TimeSeriesBinding} class with the following parameters
     *
     * @param parentName the name of the parent tree node.
     * @param graphdesc  the graph description from JRDS
     * @param desc        the index of the series in the graphdesc
     * @param path       the id of the JRDS graph
     * @param adapter    the {@link DataAdapter} for the binding.
     * @return a JRDS series binding
     */
    public static TimeSeriesBinding of(String parentName, GraphDesc graphdesc, GraphDesc.DsDesc desc, String path, DataAdapter adapter) {
        final String label;
        final Color color;
        final String legend;
        final UnitPrefixes prefix;
        final ChartType graphType;
        final String unitName;

     //   GraphDesc.DsDesc desc = graphdesc.getGraphElements().get(idx);
        label = isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name;
        color = convertAwtColorToJfxColor(desc.color);
        legend = isNullOrEmpty(desc.legend) ?
                (isNullOrEmpty(desc.name) ?
                        (isNullOrEmpty(desc.dsName) ?
                                "???" : desc.dsName) : desc.name) : desc.legend;
        switch (desc.graphType) {
            case AREA:
                graphType = ChartType.AREA;
                break;

            case STACK:
                graphType = ChartType.STACKED;
                break;

            case LINE:
                graphType = ChartType.LINE;
                break;

            case NONE:
            default:
                graphType = ChartType.STACKED;
                break;
        }
        prefix =  graphdesc.isSiUnit()? UnitPrefixes.METRIC:UnitPrefixes.BINARY;
        unitName = graphdesc.getVerticalLabel();
        return new TimeSeriesBinding(label, path, color, legend, prefix, graphType, unitName, parentName + "/" + legend, adapter);
    }

    private static javafx.scene.paint.Color convertAwtColorToJfxColor(java.awt.Color awtColor){
        if (awtColor == null){
            return null;
        }
        return Color.color(
                awtColor.getRed() / 255.0,
                awtColor.getGreen() / 255.0,
                awtColor.getBlue() / 255.0,
                awtColor.getAlpha() / 255.0);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
