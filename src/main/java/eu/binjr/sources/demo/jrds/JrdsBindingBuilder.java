/*
 *    Copyright 2020 Frederic Thevenet
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

import eu.binjr.core.data.adapters.TimeSeriesBinding;
import eu.binjr.core.data.workspace.ChartType;
import eu.binjr.core.data.workspace.UnitPrefixes;
import javafx.scene.paint.Color;
import jrds.GraphDesc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JrdsBindingBuilder extends TimeSeriesBinding.Builder {
    private static final Logger logger = LogManager.getLogger(JrdsBindingBuilder.class);

    @Override
    protected JrdsBindingBuilder self() {
        return this;
    }

    public JrdsBindingBuilder withGraphDesc(GraphDesc graphdesc){
        withLabel(isNullOrEmpty(graphdesc.getName()) ?
                (isNullOrEmpty(graphdesc.getGraphName()) ?
                        "???" : graphdesc.getGraphName()) : graphdesc.getName());
        withGraphType( getChartType(graphdesc));
        withPrefix(graphdesc.isSiUnit() ? UnitPrefixes.METRIC : UnitPrefixes.BINARY);
        withUnitName(graphdesc.getVerticalLabel());
        return self();
    }

    public JrdsBindingBuilder withGraphDesc(GraphDesc graphdesc, GraphDesc.DsDesc desc) {
        withLabel(isNullOrEmpty(desc.name) ?
                (isNullOrEmpty(desc.dsName) ?
                        (isNullOrEmpty(desc.legend) ?
                                "???" : desc.legend) : desc.dsName) : desc.name);
        withColor(convertAwtColorToJfxColor(desc.color));
        withLegend(isNullOrEmpty(desc.legend) ?
                (isNullOrEmpty(desc.name) ?
                        (isNullOrEmpty(desc.dsName) ?
                                "???" : desc.dsName) : desc.name) : desc.legend);
        withGraphType(getChartType(desc.graphType));
        withPrefix(graphdesc.isSiUnit() ? UnitPrefixes.METRIC : UnitPrefixes.BINARY);
        withUnitName(graphdesc.getVerticalLabel());

        return self();
    }


    private ChartType getChartType(GraphDesc graphdesc) {
        return graphdesc.getGraphElements().stream()
                .map(dsDesc -> dsDesc.graphType)
                .filter(desc -> desc != GraphDesc.GraphType.COMMENT && desc != GraphDesc.GraphType.NONE)
                .reduce((last, n) -> n)
                .map(this::getChartType)
                .orElse(ChartType.AREA);
    }

    private ChartType getChartType(GraphDesc.GraphType type) {
        switch (type) {
            case AREA:
                return ChartType.AREA;
            case LINE:
                return ChartType.LINE;
            case NONE:
            case STACK:
            default:
                return ChartType.STACKED;
        }
    }

    private  static Color convertAwtColorToJfxColor(java.awt.Color awtColor) {
        if (awtColor == null) {
            logger.debug(() -> "Provided color is null");
            return null;
        }
        return Color.color(
                awtColor.getRed() / 255.0,
                awtColor.getGreen() / 255.0,
                awtColor.getBlue() / 255.0,
                awtColor.getAlpha() / 255.0);
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
