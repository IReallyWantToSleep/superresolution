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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.graphics.nanovg.NanoVG;
import io.homo.superresolution.core.graphics.nanovg.NanoVGContext;
import io.homo.superresolution.core.graphics.nanovg.NanoVGFont;
import io.homo.superresolution.core.graphics.nanovg.NanoVGFontLoader;
import io.homo.superresolution.core.graphics.nanovg.renderer.NanoVGTextRenderer;
import io.homo.superresolution.core.utils.Color;
import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;


public class UIDrawContext {
    private final NanoVGContext nvgContext;
    private final TransformStackWrapper transformStack;

    public UIDrawContext(
            NanoVGContext nvgContext
    ) {
        this.nvgContext = nvgContext;
        this.transformStack = new TransformStackWrapper(new TransformStack());
    }

    public TransformStackWrapper transform() {
        return this.transformStack;
    }

    public NanoVGContext nvg() {
        return this.nvgContext;
    }

    public void save() {
        nvgContext.save();
    }

    public void restore() {
        nvgContext.restore();
    }

    public void line(float x1, float y1, float x2, float y2, float lineWidth) {
        nvgContext.line(x1, y1, x2, y2, lineWidth);
    }

    public void rect(float x, float y, float width, float height) {
        nvgContext.rect(x, y, width, height);
    }

    public void roundedRect(float x, float y, float width, float height, float radius) {
        nvgContext.roundedRect(x, y, width, height, radius);
    }

    public void beginPath() {
        nvgContext.beginPath();
    }

    public void endPath(boolean fill) {
        nvgContext.endPath(fill);
    }

    public void endPath() {
        nvgContext.endPath();
    }

    public void drawLine(float x1, float y1, float x2, float y2, Color color, float lineWidth) {
        nvgContext.drawLine(x1, y1, x2, y2, color, lineWidth);
    }

    public void drawRect(float x, float y, float width, float height, Color color, boolean fill) {
        nvgContext.drawRect(x, y, width, height, color, fill);
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, Color color, boolean fill) {
        nvgContext.drawRoundedRect(x, y, width, height, radius, color, fill);
    }

    public void imagePattern(float ox, float oy, float ex, float ey, float width, float height, float angle, float alpha, int image) {
        nvgContext.imagePattern(ox, oy, ex, ey, width, height, angle, alpha, image);
    }

    public float globalAlpha() {
        return nvgContext.globalAlpha();
    }

    public void globalAlpha(float alpha) {
        nvgContext.globalAlpha(alpha);
    }

    public void resetScissor() {
        nvgContext.resetScissor();
    }

    public void scissor(float x, float y, float width, float height) {
        nvgContext.scissor(x, y, width, height);
    }

    public NVGPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to) {
        return nvgContext.linearGradient(startX, startY, endX, endY, from, to);
    }

    public NVGPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to, NVGPaint srcPaint) {
        return nvgContext.linearGradient(startX, startY, endX, endY, from, to, srcPaint);
    }

    public void paint(NVGPaint paint) {
        nvgFillPaint(nvgContext.contextPtr, paint);
    }

    public NVGPaint radialGradient(
            float centerX,
            float centerY,
            float radius,
            Color beginColor,
            Color endColor
    ) {
        return radialGradient(
                centerX,
                centerY,
                0,
                radius,
                beginColor,
                endColor
        );
    }

    public NVGPaint radialGradient(
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            Color beginColor,
            Color endColor
    ) {
        return nvgRadialGradient(
                nvgContext.contextPtr,
                centerX,
                centerY,
                innerRadius,
                outerRadius,
                beginColor.nvg(),
                endColor.nvg(),
                NVGPaint.calloc()
        );
    }

    public void strokeWidth(float width) {
        nvgContext.strokeWidth(width);
    }

    public void strokeColor(Color color) {
        nvgContext.strokeColor(color);
    }

    public void fillColor(Color color) {
        nvgContext.fillColor(color);
    }

    public void drawColor(boolean fill, Color color) {
        nvgContext.drawColor(fill, color);
    }

    public Color fillColor() {
        return nvgContext.fillColor();
    }

    public Color strokeColor() {
        return nvgContext.strokeColor();
    }

    public float strokeWidth() {
        return nvgContext.strokeWidth();
    }

    public NanoVGFont font() {
        return NanoVGFontLoader.FONT_MAP.get(NanoVGFontLoader.REGULAR_VARIATION);
    }

    public NanoVGTextRenderer text() {
        return NanoVG.RENDERER.TEXT;
    }

    public class TransformStackWrapper {
        private final TransformStack transformStack;

        private TransformStackWrapper(TransformStack transformStack) {
            this.transformStack = transformStack;
        }

        public TransformStack stack() {
            return transformStack;
        }

        public Transform last() {
            return transformStack.last();
        }

        protected void applyTransformStack() {
            nvgContext.transform(transformStack.last());
        }

        public void push() {
            transformStack.push();
            applyTransformStack();
        }

        public void pop() {
            transformStack.pop();
            applyTransformStack();
        }

        public void translate(float x, float y) {
            transformStack.translate(x, y);
            applyTransformStack();
        }

        public void scale(float sx, float sy) {
            transformStack.scale(sx, sy);
            applyTransformStack();
        }

        public void rotate(float radians) {
            transformStack.rotate(radians);
            applyTransformStack();
        }

        public void identity() {
            transformStack.setIdentity();
            applyTransformStack();
        }
    }
}
