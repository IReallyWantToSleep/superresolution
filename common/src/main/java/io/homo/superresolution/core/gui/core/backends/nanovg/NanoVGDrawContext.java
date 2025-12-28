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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.gui.core.backends.interfaces.*;
import io.homo.superresolution.core.gui.core.backends.nanovg.commands.*;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;
import org.joml.Vector2f;

public class NanoVGDrawContext implements IUIDrawContext {
    private final NanoVGContext nvgContext;
    private final TransformStackWrapper transformStack;
    private final BatchManager batchManager;

    public NanoVGDrawContext(
            NanoVGContext nvgContext
    ) {
        this.nvgContext = nvgContext;
        this.transformStack = new TransformStackWrapper(new TransformStack());
        this.batchManager = new BatchManager();
    }


    @Override
    public TransformStackWrapper transform() {
        return this.transformStack;
    }

    public NanoVGContext nvg() {
        return this.nvgContext;
    }

    @Override
    public void save() {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();
        if (currentBatch != null) {
            currentBatch.addCommand(new NVGSaveStateCommand(nvgContext));
        }
    }

    @Override
    public void restore() {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();
        if (currentBatch != null) {
            currentBatch.addCommand(new NVGRestoreStateCommand(nvgContext));
        }
    }

    @Override
    public void line(float x1, float y1, float x2, float y2) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();
        if (currentBatch != null) currentBatch.addCommand(new NVGPathLineCommand(nvgContext, x1, y1, x2, y2));

    }

    @Override
    public void rect(float x, float y, float width, float height) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGPathRectCommand(nvgContext, x, y, width, height));
    }

    @Override
    public void arc(float x, float y, float radius, float a0, float a1) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGPathArcCommand(nvgContext, x, y, radius, a0, a1));
    }

    @Override
    public void roundedRectComplex(float x, float y, float width, float height, float bottomLeftRadius, float bottomRightRadius, float topLeftRadius, float topRightRadius) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null)
            currentBatch.addCommand(new NVGPathRoundedRectComplexCommand(nvgContext, x, y, width, height, bottomLeftRadius, bottomRightRadius, topLeftRadius, topRightRadius));
    }

    @Override
    public void beginPath() {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGBeginPathCommand(nvgContext));
    }

    @Override
    public void endPath(boolean fill) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGEndPathCommand(nvgContext, fill));
    }

    @Override
    public void drawAlignedText(IFont font, float fontSize, String text, float startX, float startY, float maxWidth, float lineHeight, Color color, TextAlign align, boolean wrap) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null)
            currentBatch.addCommand(new NVGDrawTextCommand(nvgContext, font, fontSize, text, startX, startY, maxWidth, lineHeight, color, align, wrap));
    }

    @Override
    public NanoVGPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to) {
        return new NanoVGPaint(nvgContext.linearGradient(startX, startY, endX, endY, from, to));
    }


    @Override
    public NanoVGPaint imagePattern(float ox, float oy, float ex, float ey, float width, float height, float angle, float alpha, int image) {
        return new NanoVGPaint(
                nvgContext.imagePattern(
                        ox,
                        oy,
                        ex,
                        ey,
                        width,
                        height,
                        angle,
                        alpha,
                        image)
        );
    }

    @Override
    public IPaint radialGradient(float centerX, float centerY, float radius, Color beginColor, Color endColor) {
        NanoVGColor beginColorNVG = nvgContext.color(beginColor);
        NanoVGColor endColorNVG = nvgContext.color(endColor);
        NanoVGPaint paint = new NanoVGPaint(nvgContext.contextPtr.radialGradient(
                centerX,
                centerY,
                0,
                radius,
                beginColorNVG,
                endColorNVG
        ));
        beginColorNVG.close();
        endColorNVG.close();
        return paint;
    }


    @Override
    public IPaint radialGradient(float centerX, float centerY, float innerRadius, float outerRadius, Color beginColor, Color endColor) {
        NanoVGColor beginColorNVG = nvgContext.color(beginColor);
        NanoVGColor endColorNVG = nvgContext.color(endColor);
        NanoVGPaint paint = new NanoVGPaint(nvgContext.contextPtr.radialGradient(
                centerX,
                centerY,
                innerRadius,
                outerRadius,
                beginColorNVG,
                endColorNVG
        ));
        beginColorNVG.close();
        endColorNVG.close();
        return paint;
    }

    @Override
    public void strokeWidth(float width) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGSetStrokeWidthCommand(nvgContext, width));
    }

    @Override
    public void strokeColor(Color color) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGSetStrokeColorCommand(nvgContext, color));
    }

    @Override
    public void fillColor(Color color) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGSetFillColorCommand(nvgContext, color));
    }

    @Override
    public Color fillColor() {
        return nvgContext.fillColor();
    }

    @Override
    public Color strokeColor() {
        return nvgContext.strokeColor();
    }

    @Override
    public float strokeWidth() {
        return nvgContext.strokeWidth();
    }

    @Override
    public void paint(IPaint paint) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGSetPaintCommand(nvgContext, paint));
    }

    @Override
    public float globalAlpha() {
        return nvgContext.globalAlpha();
    }


    @Override
    public void globalAlpha(float alpha) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGSetGlobalAlphaCommand(nvgContext, alpha));
    }

    @Override
    public void resetScissor() {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGResetScissorCommand(nvgContext));
    }

    @Override
    public void scissor(float x, float y, float width, float height) {
        CommandsBatch currentBatch = batchManager.getCurrentBatch();

        if (currentBatch != null) currentBatch.addCommand(new NVGAddScissorCommand(nvgContext, x, y, width, height));
    }

    @Override
    public float measureTextWidth(String text, float fontSize) {
        return NanoVG.RENDERER.TEXT.measureTextWidth(text, fontSize);
    }

    @Override
    public float measureTextHeight(String text, float fontSize) {
        return NanoVG.RENDERER.TEXT.measureTextHeight(text, fontSize);
    }

    @Override
    public Vector2f measureText(String text, float fontSize) {
        return NanoVG.RENDERER.TEXT.measureText(text, fontSize);
    }

    @Override
    public NanoVGFont font() {
        return NanoVGFontLoader.FONT_MAP.get(NanoVGFontLoader.REGULAR_VARIATION);
    }

    @Override
    public void beginBatch() {
        batchManager.beginBatch();
        save();
    }

    @Override
    public void endBatch(int zIndex) {
        restore();
        batchManager.endBatch(zIndex);
    }

    @Override
    public void closeBatch() {
        batchManager.closeCurrentBatch();
    }

    @Override
    public void clearBatches() {
        batchManager.clearBatches();
    }

    @Override
    public void drawAll() {
        batchManager.executeAll();
    }

    public BatchManager getBatchManager() {
        return batchManager;
    }

    public class TransformStackWrapper implements ITransformStack {
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
            CommandsBatch currentBatch = batchManager.getCurrentBatch();
            if (currentBatch != null)
                currentBatch.addCommand(new NVGApplyTransformCommand(nvgContext, transformStack.last().copy()));
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

        @Override
        public void set(Transform transform) {
            transformStack.set(transform);
            applyTransformStack();
        }

        @Override
        public void apply(Transform transform) {
            transformStack.apply(transform);
            applyTransformStack();
        }
    }
}
