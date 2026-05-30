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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.gui.core.backends.interfaces.*;
import io.homo.superresolution.core.gui.core.backends.render.*;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class NanoVGRenderContext implements RenderContext {
    private static final int NVG_IMAGE_NODELETE = 1 << 16;

    private final NanoVGContextWrapper nvg;
    private final RenderTree renderTree;
    private final TransformStack transformStack;

    private final Stack<RenderState> stateStack = new Stack<>();
    private final List<Float> alphaStack = new ArrayList<>();
    private final Stack<List<Float>> alphaStackSnapshots = new Stack<>();
    private final Stack<GroupNode> groupStack = new Stack<>();
    private RenderState currentState = new RenderState();
    private GroupNode currentGroup = null;
    private float guiScale = 1.0f;
    private float dpiScale = 1.0f;
    private float viewportWidth = 0.0f;
    private float viewportHeight = 0.0f;

    private boolean immediateMode = false;

    public NanoVGRenderContext(NanoVGContextWrapper nvg) {
        this.nvg = nvg;
        this.renderTree = new RenderTree();
        this.transformStack = new TransformStack();
        this.alphaStack.add(1.0f);
    }

    private void syncAlphaStack(float alpha) {
        if (alphaStack.isEmpty()) {
            alphaStack.add(alpha);
        } else {
            alphaStack.set(alphaStack.size() - 1, alpha);
        }
    }

    public NanoVGContextWrapper nvg() {
        return nvg;
    }

    @Override
    public float guiScale() {
        return guiScale;
    }

    @Override
    public void setGuiScale(float scale) {
        this.guiScale = scale;
    }

    @Override
    public float dpiScale() {
        return dpiScale;
    }

    @Override
    public void setDpiScale(float dpi) {
        this.dpiScale = dpi;
    }

    @Override
    public void save() {
        stateStack.push(currentState.copy());
        alphaStackSnapshots.push(new ArrayList<>(alphaStack));
        transformStack.push();
        nvg.save();
    }

    @Override
    public void restore() {
        if (!stateStack.isEmpty()) {
            currentState = stateStack.pop();
        }
        if (!alphaStackSnapshots.isEmpty()) {
            alphaStack.clear();
            alphaStack.addAll(alphaStackSnapshots.pop());
            nvg.rawContext.globalAlpha(currentState.alpha());
        }
        transformStack.pop();
        applyTransform();
        nvg.restore();
    }

    @Override
    public void resetState() {
        currentState.reset();
        transformStack.setIdentity();
        syncAlphaStack(currentState.alpha());
        applyCurrentState();
    }

    @Override
    public RenderState currentState() {
        return currentState.copy();
    }

    @Override
    public void applyState(RenderState state) {
        save();
        currentState.copyFrom(state);
        transformStack.set(state.transform());
        syncAlphaStack(currentState.alpha());
        applyCurrentState();
    }

    @Override
    public void restoreState() {
        restore();
    }

    @Override
    public void globalAlpha(float alpha) {
        currentState.alpha(alpha);
        syncAlphaStack(alpha);
        nvg.rawContext.globalAlpha(alpha);
    }

    @Override
    public float globalAlpha() {
        return currentState.alpha();
    }

    @Override
    public void pushAlpha(float alpha) {
        float newAlpha = currentState.alpha() * alpha;
        alphaStack.add(newAlpha);
        currentState.alpha(newAlpha);
        nvg.rawContext.globalAlpha(newAlpha);
    }

    @Override
    public void popAlpha() {
        if (alphaStack.size() > 1) {
            alphaStack.remove(alphaStack.size() - 1);
            float prevAlpha = alphaStack.get(alphaStack.size() - 1);
            currentState.alpha(prevAlpha);
            nvg.rawContext.globalAlpha(prevAlpha);
        }
    }

    @Override
    public Transform transform() {
        return transformStack.last();
    }

    @Override
    public void pushTransform() {
        transformStack.push();
    }

    @Override
    public void popTransform() {
        transformStack.pop();
        applyTransform();
    }

    @Override
    public void resetTransform() {
        transformStack.last().setMatrix(Transform.identityMatrix());
        applyTransform();
    }

    @Override
    public void translate(float x, float y) {
        transformStack.translate(x, y);
        applyTransform();
    }

    @Override
    public void scale(float sx, float sy) {
        transformStack.scale(sx, sy);
        applyTransform();
    }

    @Override
    public void rotate(float radians) {
        transformStack.rotate(radians);
        applyTransform();
    }

    @Override
    public void applyTransform(Transform transform) {
        if (transform == null || transform.isIdentity()) {
            return;
        }
        transformStack.apply(transform);
        currentState.transform(transformStack.last());
        applyTransform();
    }

    @Override
    public Vector2f transformPoint(float x, float y) {
        return transformStack.last().transformPoint(new Vector2f(x, y));
    }

    @Override
    public void scissor(float x, float y, float width, float height) {
        currentState.intersectScissor(x, y, width, height);
        nvg.rawContext.intersectScissor(x, y, width, height);
    }

    @Override
    public void resetScissor() {
        currentState.resetScissor();
        nvg.rawContext.resetScissor();
    }

    @Override
    public void beginPath() {
        nvg.beginPath();
    }

    @Override
    public void endPath(boolean fill) {
        nvg.endPath(fill);
    }

    @Override
    public void move(float x, float y) {
        nvg.rawContext.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) {
        nvg.rawContext.lineTo(x, y);
    }

    public IPaint boxGradient(float x, float y, float width,
                              float height, float radius,
                              float feather, Color innerColor,
                              Color outerColor) {
        NanoVGColor nvgInnerColor = nvg.rawContext.colorRGBA(
                innerColor.red(), innerColor.green(), innerColor.blue(), innerColor.alpha()
        );
        NanoVGColor nvgOuterColor = nvg.rawContext.colorRGBA(
                outerColor.red(), outerColor.green(), outerColor.blue(), outerColor.alpha()
        );
        NanoVGBackendPaint paint = new NanoVGBackendPaint(nvg.rawContext.boxGradient(
                x, y, width, height, radius, feather, nvgInnerColor, nvgOuterColor
        ));
        nvgInnerColor.close();
        nvgOuterColor.close();
        return paint;
    }

    @Override
    public void line(float x1, float y1, float x2, float y2) {
        nvg.line(x1, y1, x2, y2);
    }

    @Override
    public void rect(float x, float y, float width, float height) {
        nvg.rect(x, y, width, height);
    }

    @Override
    public void arc(float x, float y, float radius, float a0, float a1) {
        nvg.rawContext.arc(x, y, radius, a0, a1, 1);
    }

    @Override
    public void bezier(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        nvg.rawContext.bezierTo(c1x, c1y, c2x, c2y, x, y);
    }

    @Override
    public void roundedRectComplex(float x, float y, float width, float height,
                                   float bottomLeftRadius, float bottomRightRadius,
                                   float topLeftRadius, float topRightRadius) {
        nvg.rawContext.roundedRectVarying(x, y, width, height,
                topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
    }

    @Override
    public void strokeWidth(float width) {
        nvg.strokeWidth(width);
    }

    @Override
    public void strokeColor(Color color) {
        nvg.strokeColor(color);
    }

    @Override
    public void fillColor(Color color) {
        nvg.fillColor(color);
    }

    @Override
    public Color fillColor() {
        return nvg.fillColor();
    }

    @Override
    public Color strokeColor() {
        return nvg.strokeColor();
    }

    @Override
    public float strokeWidth() {
        return nvg.strokeWidth();
    }

    @Override
    public void paint(IPaint paint) {
        if (paint instanceof NanoVGBackendPaint) {
            nvg.rawContext.fillPaint(((NanoVGBackendPaint) paint).get());
        }
    }

    @Override
    public IPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to) {
        return new NanoVGBackendPaint(nvg.linearGradient(startX, startY, endX, endY, from, to));
    }

    @Override
    public IPaint radialGradient(float centerX, float centerY, float radius, Color beginColor, Color endColor) {
        NanoVGColor nvgBeginColor = nvg.rawContext.colorRGBA(
                beginColor.red(), beginColor.green(), beginColor.blue(), beginColor.alpha()
        );
        NanoVGColor nvgEndColor = nvg.rawContext.colorRGBA(
                endColor.red(), endColor.green(), endColor.blue(), endColor.alpha()
        );
        NanoVGBackendPaint paint = new NanoVGBackendPaint(nvg.rawContext.radialGradient(centerX, centerY, 0, radius, nvgBeginColor, nvgEndColor));
        nvgBeginColor.close();
        nvgEndColor.close();
        return paint;
    }

    @Override
    public IPaint radialGradient(float centerX, float centerY, float innerRadius, float outerRadius,
                                 Color beginColor, Color endColor) {
        NanoVGColor nvgBeginColor = nvg.rawContext.colorRGBA(
                beginColor.red(), beginColor.green(), beginColor.blue(), beginColor.alpha()
        );
        NanoVGColor nvgEndColor = nvg.rawContext.colorRGBA(
                endColor.red(), endColor.green(), endColor.blue(), endColor.alpha()
        );
        NanoVGBackendPaint paint = new NanoVGBackendPaint(nvg.rawContext.radialGradient(centerX, centerY, innerRadius, outerRadius, nvgBeginColor, nvgEndColor));
        nvgBeginColor.close();
        nvgEndColor.close();
        return paint;
    }

    @Override
    public IPaint imagePattern(float ox, float oy, float ex, float ey, float width, float height,
                               float angle, float alpha, IImage image) {
        return new NanoVGBackendPaint(nvg.imagePattern(ox, oy, ex, ey, width, height, angle, alpha, ((NanoVGImage) image).nvgId));
    }

    @Override
    public IFont font() {
        return NanoVGFontLoader.FONT_MAP.get(NanoVGFontLoader.REGULAR_VARIATION);
    }

    @Override
    public float measureTextWidth(IFont font, String text, float fontSize, float lineHeight, float weight) {
        return NanoVGBackend.RENDERER.TEXT.measureTextWidth(font, text, fontSize, lineHeight, weight);
    }

    @Override
    public float measureTextHeight(IFont font, String text, float fontSize, float lineHeight, float weight) {
        return NanoVGBackend.RENDERER.TEXT.measureTextHeight(font, text, fontSize, lineHeight, weight);
    }

    @Override
    public Vector2f measureText(IFont font, String text, float fontSize, float lineHeight, float weight) {
        return NanoVGBackend.RENDERER.TEXT.measureText(font, text, fontSize, lineHeight, weight);
    }

    @Override
    public TextMetrics measureTextMetrics(IFont font, float fontSize, String text, float lineMaxWidth,
                                          float lineHeight, float weight, boolean wrap) {
        return NanoVGBackend.RENDERER.TEXT.calculateTextMetrics(
                font, fontSize, text, lineMaxWidth, lineHeight, wrap, weight
        );
    }

    @Override
    public void drawAlignedText(IFont font, float fontSize, String text, float x, float y, float lineMaxWidth, float lineHeight, float weight, Color color, TextAlign align, boolean wrap) {
        NanoVGBackend.RENDERER.TEXT.drawAlignedText(font, fontSize, text, x, y, lineMaxWidth, lineHeight, weight, color, align, wrap);
    }

    @Override
    public void drawAlignedText(IFont font, float fontSize, TextMetrics textMetrics, float x, float y, float lineMaxWidth, float lineHeight, float weight, Color color, TextAlign align, boolean wrap) {
        NanoVGBackend.RENDERER.TEXT.drawAlignedText(font, fontSize, textMetrics, x, y, lineMaxWidth, lineHeight, weight, color, align, wrap);
    }

    @Override
    public RenderTree renderTree() {
        return renderTree;
    }

    @Override
    public void draw(float zIndex, RenderLayer layer, Consumer<RenderContext> drawFunc) {
        if (immediateMode) {
            drawFunc.accept(this);
            return;
        }

        DrawNode node = new DrawNode(zIndex, layer, drawFunc);
        node.capturedState(currentState.copy());

        if (currentGroup != null) {
            currentGroup.addChild(node);
        } else {
            renderTree.addNode(node);
        }
    }

    @Override
    public void beginGroup(float zIndex, RenderLayer layer) {
        GroupNode group = new GroupNode(zIndex, layer);
        group.capturedState(currentState.copy());

        if (currentGroup != null) {
            currentGroup.addChild(group);
            groupStack.push(currentGroup);
        }

        currentGroup = group;
    }

    @Override
    public void endGroup() {
        if (currentGroup != null) {
            if (groupStack.isEmpty()) {
                renderTree.addNode(currentGroup);
                currentGroup = null;
            } else {
                currentGroup = groupStack.pop();
            }
        }
    }

    @Override
    public void deferToLayer(RenderLayer layer, float zIndex, Consumer<RenderContext> drawFunc) {
        Vector2f screenPos = transformPoint(0, 0);
        DeferredNode node = new DeferredNode(zIndex, layer, screenPos, drawFunc);
        node.capturedState(currentState.copy());
        renderTree.addNode(node);
    }

    @Override
    public void flush() {
        renderTree.render(this,
                () -> {
                    nvg.rawContext.globalAlpha(1.0f);
                    nvg.rawContext.resetScissor();
                    nvg.resetTransform();
                    currentState.reset();
                    alphaStack.clear();
                    alphaStack.add(1.0f);
                    transformStack.setIdentity();
                },
                null
        );
        renderTree.clear();
    }

    @Override
    public void beginImmediate() {
        this.immediateMode = true;
    }

    @Override
    public void endImmediate() {
        this.immediateMode = false;
    }

    @Override
    public boolean isImmediate() {
        return immediateMode;
    }

    @Override
    public float viewportWidth() {
        return viewportWidth;
    }

    @Override
    public float viewportHeight() {
        return viewportHeight;
    }

    public IImage createImage(ITexture texture) {
        if (!(texture instanceof GlTexture2D)) {
            throw new IllegalArgumentException("Texture must be instance of GlTexture2D");
        }
        NanoVGImage image = new NanoVGImage();
        image.nvgId = nvg.rawContext.createImageFromHandle(
                (int) texture.handle(),
                texture.getWidth(),
                texture.getHeight(),
                NVG_IMAGE_NODELETE
        );
        return image;
    }

    public void deleteImage(IImage image) {
        if (image instanceof NanoVGImage) {
            image.destroy();
        } else {
            throw new IllegalArgumentException("Image must be instance of NanoVGImage");
        }
    }

    private void applyCurrentState() {
        nvg.globalAlpha(currentState.alpha());
        syncAlphaStack(currentState.alpha());

        Transform t = currentState.transform();
        transformStack.set(t);
        nvg.resetTransform();
        nvg.transform(t);

        if (currentState.hasScissor()) {
            RenderState.ScissorRect scissor = currentState.scissor();
            nvg.rawContext.scissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
        }
    }

    private void applyTransform() {
        Transform t = transformStack.last();
        currentState.transform(t);
        nvg.resetTransform();
        nvg.transform(t);
    }

    public void setViewportSize(float width, float height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }
}
