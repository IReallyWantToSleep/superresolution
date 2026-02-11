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

import io.homo.superresolution.core.gui.MaterialElevation;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class MaterialChart extends MaterialWidget<MaterialChart> {
    private final List<MaterialChartDataSeries> seriesList = new ArrayList<>();
    private String title = "";
    private float minValue = Float.NaN;
    private float maxValue = Float.NaN;
    private boolean autoRange = true;
    private Consumer<MaterialChart> updateCallback;
    private Function<Float, String> valueFormatter;
    private long lastUpdateTime = 0;
    private long updateIntervalMs = 100;

    public MaterialChart() {
        this.style = new MaterialChartStyle();
        getLayoutNode().setDebugName("MaterialChart");
    }

    public static MaterialChart create() {
        return new MaterialChart();
    }

    @Override
    protected void init() {
    }

    @Override
    public MaterialChartStyle style() {
        return (MaterialChartStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        long now = System.currentTimeMillis();
        if (updateCallback != null && now - lastUpdateTime >= updateIntervalMs) {
            updateCallback.accept(this);
            lastUpdateTime = now;
        }

        Rectangle bounds = getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        MaterialChartStyle s = style();
        float padding = s.padding();

        ctx.beginGroup(s.zIndex());

        Color bgColor = scheme().surfaceContainer();
        MaterialElevation.draw(
                ctx,
                2,
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                s.cornerRadius()
        );

        ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height,
                s.cornerRadius(), bgColor, true);

        float titleHeight = 0;
        if (title != null && !title.isEmpty()) {
            titleHeight = s.fontSize() + 4;
            ctx.drawAlignedText(ctx.font(), s.fontSize(), title,
                    bounds.x + padding, bounds.y + padding,
                    bounds.width - padding * 2, s.fontSize() + 2,
                    scheme().onSurface(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    false);
        }

        float legendHeight = 0;
        if (s.showLegend() && !seriesList.isEmpty()) {
            legendHeight = s.legendHeight();
            renderLegend(ctx, bounds, padding, titleHeight - 6, legendHeight);
        }

        float effMin = computeEffectiveMin();
        float effMax = computeEffectiveMax();
        float labelWidth = s.showLabels() ? computeLabelWidth(ctx, effMin, effMax, s) + 6 : 0;
        float chartX = bounds.x + padding + labelWidth;
        float chartY = bounds.y + padding + titleHeight + legendHeight - 4;
        float chartW = bounds.width - padding * 2 - labelWidth;
        float chartH = bounds.height - padding * 2 - titleHeight - legendHeight;

        if (chartW <= 0 || chartH <= 0) {
            ctx.endGroup();
            return;
        }


        if (effMax <= effMin) {
            effMax = effMin + 1;
        }

        if (s.showGrid()) {
            renderGrid(ctx, chartX, chartY, chartW, chartH, effMin, effMax, s);
        }

        if (s.showLabels()) {
            renderYLabels(ctx, bounds.x + padding, chartY, labelWidth - 4, chartH, effMin, effMax, s);
        }

        ctx.save();
        ctx.scissor(chartX, chartY, chartW, chartH);

        for (MaterialChartDataSeries series : seriesList) {
            if (series.getDataCount() < 1) {
                continue;
            }
            switch (series.getType()) {
                case Bar -> renderBar(ctx, series, chartX, chartY, chartW, chartH, effMin, effMax, s);
                case Line -> renderLine(ctx, series, chartX, chartY, chartW, chartH, effMin, effMax, s, false);
                case Curve -> renderLine(ctx, series, chartX, chartY, chartW, chartH, effMin, effMax, s, true);
            }
        }

        if (s.showAverage()) {
            for (MaterialChartDataSeries series : seriesList) {
                if (series.getDataCount() < 2) {
                    continue;
                }
                renderAverageLine(ctx, series, chartX, chartY, chartW, chartH, effMin, effMax, s);
            }
        }

        ctx.resetScissor();
        ctx.restore();

        ctx.endGroup();
    }

    public MaterialChart title(String title) {
        this.title = title;
        return this;
    }

    public MaterialChart minValue(float minValue) {
        this.minValue = minValue;
        this.autoRange = Float.isNaN(minValue) && Float.isNaN(maxValue);
        return this;
    }

    public MaterialChart maxValue(float maxValue) {
        this.maxValue = maxValue;
        this.autoRange = Float.isNaN(minValue) && Float.isNaN(maxValue);
        return this;
    }

    public MaterialChart range(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        this.autoRange = false;
        return this;
    }

    public MaterialChart autoRange() {
        this.autoRange = true;
        this.minValue = Float.NaN;
        this.maxValue = Float.NaN;
        return this;
    }

    public MaterialChart updateCallback(Consumer<MaterialChart> callback) {
        this.updateCallback = callback;
        return this;
    }

    public MaterialChart updateInterval(long intervalMs) {
        this.updateIntervalMs = intervalMs;
        return this;
    }

    public MaterialChart valueFormatter(Function<Float, String> formatter) {
        this.valueFormatter = formatter;
        return this;
    }

    public MaterialChart addSeries(MaterialChartDataSeries series) {
        seriesList.add(series);
        return this;
    }

    public MaterialChart addSeries(String name, Color color, MaterialChartType type, int maxDataPoints) {
        seriesList.add(new MaterialChartDataSeries(name, color, type, maxDataPoints));
        return this;
    }

    public MaterialChartDataSeries getSeries(int index) {
        return seriesList.get(index);
    }

    public MaterialChartDataSeries getSeries(String name) {
        for (MaterialChartDataSeries s : seriesList) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    public List<MaterialChartDataSeries> getAllSeries() {
        return seriesList;
    }

    public MaterialChart clearAllData() {
        for (MaterialChartDataSeries s : seriesList) {
            s.clear();
        }
        return this;
    }

    private String formatValue(float value) {
        if (valueFormatter != null) {
            return valueFormatter.apply(value);
        }
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000f);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000f);
        } else if (value >= 1f) {
            return String.format("%.1f", value);
        } else {
            return String.format("%.2f", value);
        }
    }

    private float computeEffectiveMin() {
        if (!autoRange && !Float.isNaN(minValue)) {
            return minValue;
        }
        float min = Float.MAX_VALUE;
        for (MaterialChartDataSeries s : seriesList) {
            if (s.getDataCount() > 0) {
                float sMin = s.getMin();
                if (sMin < min) {
                    min = sMin;
                }
            }
        }
        return min == Float.MAX_VALUE ? 0 : Math.max(0, min * 0.9f);
    }

    private float computeEffectiveMax() {
        if (!autoRange && !Float.isNaN(maxValue)) {
            return maxValue;
        }
        float max = Float.MIN_VALUE;
        for (MaterialChartDataSeries s : seriesList) {
            if (s.getDataCount() > 0) {
                float sMax = s.getMax();
                if (sMax > max) {
                    max = sMax;
                }
            }
        }
        return max == Float.MIN_VALUE ? 1 : max * 1.1f;
    }

    private void renderLegend(RenderContext ctx, Rectangle bounds, float padding,
                              float titleHeight, float legendHeight) {
        MaterialChartStyle s = style();
        float x = bounds.x + padding;
        float y = bounds.y + padding + titleHeight;
        float dotSize = 6;
        float gap = 16;
        float textGap = 5;

        for (MaterialChartDataSeries series : seriesList) {
            ctx.beginPath();
            ctx.fillColor(series.getColor());
            ctx.arc(x + dotSize / 2, y + legendHeight / 2, dotSize / 2);
            ctx.endPath(true);

            String label = series.getName();
            float textWidth = ctx.measureTextWidth(label, s.labelFontSize(), s.labelFontSize() + 2);
            ctx.drawAlignedText(ctx.font(), s.labelFontSize(), label,
                    x + dotSize + textGap, y + (legendHeight - s.labelFontSize()) / 2,
                    textWidth + 4, s.labelFontSize() + 2,
                    scheme().onSurfaceVariant(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    false);

            x += dotSize + textGap + textWidth + gap;
        }
    }

    private void renderGrid(RenderContext ctx, float x, float y, float w, float h,
                            float min, float max, MaterialChartStyle s) {
        int lines = s.gridLinesHorizontal();
        ctx.strokeWidth(s.gridLineWidth());
        for (int i = 0; i <= lines; i++) {
            float gy = y + h - (h * i / lines);
            ctx.line(x, gy, x + w, gy, s.gridLineWidth(), scheme().onSurfaceVariant().copy().alpha(155));
        }
    }

    private float computeLabelWidth(RenderContext ctx, float min, float max, MaterialChartStyle s) {
        int lines = s.gridLinesHorizontal();
        float maxW = 0;
        for (int i = 0; i <= lines; i++) {
            float value = min + (max - min) * i / lines;
            String label = formatValue(value);
            float w = ctx.measureTextWidth(label, s.labelFontSize(), s.labelFontSize() + 2);
            if (w > maxW) {
                maxW = w;
            }
        }
        return Math.max(maxW, 30);
    }

    private void renderYLabels(RenderContext ctx, float x, float chartY, float labelWidth,
                               float chartH, float min, float max, MaterialChartStyle s) {
        int lines = s.gridLinesHorizontal();
        for (int i = 0; i <= lines; i++) {
            float value = min + (max - min) * i / lines;
            String label = formatValue(value);
            float ly = chartY + chartH - (chartH * i / lines) - s.labelFontSize() / 2;
            ctx.drawAlignedText(ctx.font(), s.labelFontSize(), label,
                    x, ly, labelWidth, s.labelFontSize() + 2,
                    scheme().onSurfaceVariant(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    false);
        }
    }

    private void renderBar(RenderContext ctx, MaterialChartDataSeries series,
                           float x, float y, float w, float h,
                           float min, float max, MaterialChartStyle s) {
        int count = series.getDataCount();
        if (count < 1) {
            return;
        }
        float[] data = series.getData();

        float barSpacing = w / count;
        float barWidth = barSpacing * s.barWidthRatio();
        float range = max - min;

        Color barColor = series.getColor();
        Color barColorTranslucent = barColor.copy().alpha(180);

        for (int i = 0; i < count; i++) {
            float value = data[i];
            if (value <= 0) {
                continue;
            }

            float norm = Math.max(0, Math.min(1, (value - min) / range));
            float barH = norm * h;
            float bx = x + i * barSpacing + (barSpacing - barWidth) / 2;
            float by = y + h - barH;

            // Gradient fill
            IPaint gradient = ctx.linearGradient(bx, by, bx, by + barH,
                    barColor, barColorTranslucent);
            ctx.beginPath();
            ctx.paint(gradient);
            ctx.roundedRectComplex(bx, by, barWidth, barH, 0, 0, 3, 3);
            ctx.endPath(true);
        }
    }

    private void renderLine(RenderContext ctx, MaterialChartDataSeries series,
                            float x, float y, float w, float h,
                            float min, float max, MaterialChartStyle s, boolean smooth) {
        int count = series.getDataCount();
        if (count < 2) {
            return;
        }
        float[] data = series.getData();
        float range = max - min;

        float[] px = new float[count];
        float[] py = new float[count];
        for (int i = 0; i < count; i++) {
            px[i] = x + (w * i) / (count - 1);
            float norm = Math.max(0, Math.min(1, (data[i] - min) / range));
            py[i] = y + h - norm * h;
        }

        Color fillColor = series.getColor().copy().alpha(30);
        IPaint gradient = ctx.linearGradient(
                x, y,
                x, y + h,
                fillColor.copy().alpha(255),
                fillColor.copy().alpha(10)
        );
        ctx.beginPath();
        ctx.paint(gradient);
        ctx.move(px[0], y + h);
        ctx.lineTo(px[0], py[0]);
        if (smooth && count > 2) {
            for (int i = 1; i < count; i++) {
                float cx1 = px[i - 1] + (px[i] - px[i - 1]) * 0.5f;
                float cy1 = py[i - 1];
                float cx2 = px[i] - (px[i] - px[i - 1]) * 0.5f;
                float cy2 = py[i];
                ctx.bezier(cx1, cy1, cx2, cy2, px[i], py[i]);
            }
        } else {
            for (int i = 1; i < count; i++) {
                ctx.lineTo(px[i], py[i]);
            }
        }
        ctx.lineTo(px[count - 1], y + h);
        ctx.endPath(true);

        // Stroke line
        ctx.beginPath();
        ctx.strokeWidth(s.dataLineWidth());
        ctx.strokeColor(series.getColor());
        ctx.move(px[0], py[0]);
        if (smooth && count > 2) {
            for (int i = 1; i < count; i++) {
                float cx1 = px[i - 1] + (px[i] - px[i - 1]) * 0.5f;
                float cy1 = py[i - 1];
                float cx2 = px[i] - (px[i] - px[i - 1]) * 0.5f;
                float cy2 = py[i];
                ctx.bezier(cx1, cy1, cx2, cy2, px[i], py[i]);
            }
        } else {
            for (int i = 1; i < count; i++) {
                ctx.lineTo(px[i], py[i]);
            }
        }
        ctx.endPath(false);
    }

    private void renderAverageLine(RenderContext ctx, MaterialChartDataSeries series,
                                   float x, float y, float w, float h,
                                   float min, float max, MaterialChartStyle s) {
        float avg = series.getAverage();
        float range = max - min;
        float norm = Math.max(0, Math.min(1, (avg - min) / range));
        float ay = y + h - norm * h;

        Color avgColor = series.getColor().copy().alpha(160);
        float dashLen = 5f;
        float gapLen = 3f;
        float cx = x;
        ctx.strokeWidth(s.averageLineWidth());
        while (cx < x + w) {
            float endX = Math.min(cx + dashLen, x + w);
            ctx.line(cx, ay, endX, ay, s.averageLineWidth(), avgColor);
            cx += dashLen + gapLen;
        }

        String avgLabel = "avg: " + formatValue(avg);
        float labelW = ctx.measureTextWidth(avgLabel, s.labelFontSize(), s.labelFontSize() + 2);
        float labelX = x + w - labelW - 4;
        float labelY = ay - s.labelFontSize() - 3;
        if (labelY < y) {
            labelY = ay + 3;
        }

        ctx.roundedRect(labelX - 3, labelY - 1, labelW + 6, s.labelFontSize() + 4,
                3, avgColor.copy().alpha(50), true);
        ctx.drawAlignedText(ctx.font(), s.labelFontSize(), avgLabel,
                labelX, labelY, labelW + 4, s.labelFontSize() + 2,
                avgColor.copy().alpha(255),
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                false);
    }
}
