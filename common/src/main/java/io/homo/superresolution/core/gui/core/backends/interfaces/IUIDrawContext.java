/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import io.homo.superresolution.core.utils.Color;


public interface IUIDrawContext extends ITextRenderer {
    ITransformStack transform();

    void save();

    void restore();

    void line(float x1, float y1, float x2, float y2);

    void rect(float x, float y, float width, float height);

    void arc(float x, float y, float radius, float a0, float a1);

    default void arc(
            float x,
            float y,
            float radius
    ) {
        arc(
                x, y, radius, (float) 0, (float) (2 * Math.PI)
        );
    }

    default void roundedRect(float x, float y, float width, float height, float radius) {
        roundedRectComplex(x, y, width, height, radius, radius, radius, radius);
    }

    void roundedRectComplex(float x, float y, float width, float height, float bottomLeftRadius, float bottomRightRadius, float topLeftRadius, float topRightRadius);

    void beginPath();

    void endPath(boolean fill);

    default void endPath() {
        endPath(true);
    }

    default void drawLine(float x1, float y1, float x2, float y2, float lineWidth, Color color) {
        beginPath();
        drawColor(false, color);
        line(x1, y1, x2, y2);
        endPath(false);
    }

    default void drawRect(float x, float y, float width, float height, Color color, boolean fill) {
        beginPath();
        drawColor(fill, color);
        rect(x, y, width, height);
        endPath(fill);
    }

    default void drawRoundedRect(float x, float y, float width, float height, float radius, Color color, boolean fill) {
        beginPath();
        drawColor(fill, color);
        roundedRect(x, y, width, height, radius);
        endPath(fill);
    }

    default void drawArc(
            float x,
            float y,
            float radius,
            Color color,
            boolean fill
    ) {
        beginPath();
        drawColor(fill, color);
        arc(x, y, radius);
        endPath(fill);
    }


    float globalAlpha();

    void globalAlpha(float alpha);

    void resetScissor();

    void scissor(float x, float y, float width, float height);

    void paint(IPaint paint);

    IPaint createPaint();

    IPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to);

    IPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to, IPaint srcPaint);

    IPaint imagePattern(float ox, float oy, float ex, float ey, float width, float height, float angle, float alpha, int image);

    IPaint radialGradient(float centerX, float centerY, float radius, Color beginColor, Color endColor);

    IPaint radialGradient(float centerX, float centerY, float innerRadius, float outerRadius, Color beginColor, Color endColor);

    IPaint radialGradient(float centerX, float centerY, float radius, Color beginColor, Color endColor, IPaint srcPaint);

    IPaint radialGradient(float centerX, float centerY, float innerRadius, float outerRadius, Color beginColor, Color endColor, IPaint srcPaint);

    void strokeWidth(float width);

    void strokeColor(Color color);

    void fillColor(Color color);

    void immediateScissor(float x, float y, float width, float height);

    void immediateResetScissor();

    void immediateSave();

    void immediateRestore();

    default void drawColor(boolean fill, Color color) {
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
    }

    Color fillColor();

    Color strokeColor();

    float strokeWidth();

    IFont font();

    void beginBatch();

    void endBatch(int zIndex);

    void closeBatch();

    void clearBatches();

    void drawAll();
}
