/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui.widgets.chart;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialChartStyle extends WidgetStyle<MaterialChartStyle> {
    private float gridLineWidth = 1f;
    private float dataLineWidth = 2.0f;
    private float averageLineWidth = 1.5f;
    private float barWidthRatio = 0.6f;
    private float cornerRadius = 12f;
    private float fontSize = 11f;
    private float labelFontSize = 10f;
    private float padding = 12f;
    private float legendHeight = 24f;
    private boolean showAverage = true;
    private boolean showGrid = true;
    private boolean showLabels = true;
    private boolean showLegend = true;
    private int gridLinesHorizontal = 4;

    public float gridLineWidth() {
        return gridLineWidth;
    }

    public MaterialChartStyle gridLineWidth(float gridLineWidth) {
        this.gridLineWidth = gridLineWidth;
        return this;
    }

    public float dataLineWidth() {
        return dataLineWidth;
    }

    public MaterialChartStyle dataLineWidth(float dataLineWidth) {
        this.dataLineWidth = dataLineWidth;
        return this;
    }

    public float averageLineWidth() {
        return averageLineWidth;
    }

    public MaterialChartStyle averageLineWidth(float averageLineWidth) {
        this.averageLineWidth = averageLineWidth;
        return this;
    }

    public float barWidthRatio() {
        return barWidthRatio;
    }

    public MaterialChartStyle barWidthRatio(float barWidthRatio) {
        this.barWidthRatio = barWidthRatio;
        return this;
    }

    public float cornerRadius() {
        return cornerRadius;
    }

    public MaterialChartStyle cornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public float fontSize() {
        return fontSize;
    }

    public MaterialChartStyle fontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public float labelFontSize() {
        return labelFontSize;
    }

    public MaterialChartStyle labelFontSize(float labelFontSize) {
        this.labelFontSize = labelFontSize;
        return this;
    }

    public float padding() {
        return padding;
    }

    public MaterialChartStyle padding(float padding) {
        this.padding = padding;
        return this;
    }

    public float legendHeight() {
        return legendHeight;
    }

    public MaterialChartStyle legendHeight(float legendHeight) {
        this.legendHeight = legendHeight;
        return this;
    }

    public boolean showAverage() {
        return showAverage;
    }

    public MaterialChartStyle showAverage(boolean showAverage) {
        this.showAverage = showAverage;
        return this;
    }

    public boolean showGrid() {
        return showGrid;
    }

    public MaterialChartStyle showGrid(boolean showGrid) {
        this.showGrid = showGrid;
        return this;
    }

    public boolean showLabels() {
        return showLabels;
    }

    public MaterialChartStyle showLabels(boolean showLabels) {
        this.showLabels = showLabels;
        return this;
    }

    public boolean showLegend() {
        return showLegend;
    }

    public MaterialChartStyle showLegend(boolean showLegend) {
        this.showLegend = showLegend;
        return this;
    }

    public int gridLinesHorizontal() {
        return gridLinesHorizontal;
    }

    public MaterialChartStyle gridLinesHorizontal(int gridLinesHorizontal) {
        this.gridLinesHorizontal = gridLinesHorizontal;
        return this;
    }
}
