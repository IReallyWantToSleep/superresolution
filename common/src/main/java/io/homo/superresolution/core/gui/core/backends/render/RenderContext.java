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

package io.homo.superresolution.core.gui.core.backends.render;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.gui.core.backends.interfaces.*;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

import java.util.function.Consumer;

public interface RenderContext {
    float DEFAULT_FONT_WEIGHT = 500;

    float guiScale();

    void setGuiScale(float scale);

    float dpiScale();

    void setDpiScale(float dpi);

    default float effectiveScale() {
        return guiScale();
    }

    default float toPhysical(float logical) {
        return logical * effectiveScale();
    }

    default float toLogical(float physical) {
        return physical / effectiveScale();
    }

    void save();

    void restore();

    void resetState();

    RenderState currentState();

    void applyState(RenderState state);

    void restoreState();

    void globalAlpha(float alpha);

    float globalAlpha();

    void pushAlpha(float alpha);

    void popAlpha();

    Transform transform();

    void pushTransform();

    void popTransform();

    void resetTransform();

    void translate(float x, float y);

    void scale(float sx, float sy);

    void rotate(float radians);

    void applyTransform(Transform transform);

    Vector2f transformPoint(float x, float y);

    default Vector2f transformPoint(Vector2f point) {
        return transformPoint(point.x, point.y);
    }

    void scissor(float x, float y, float width, float height);

    void resetScissor();

    void beginPath();

    void endPath(boolean fill);

    default void endPath() {
        endPath(true);
    }

    void move(float x, float y);

    void lineTo(float x, float y);

    IPaint boxGradient(float x, float y, float width,
                       float height, float radius,
                       float feather, Color innerColor,
                       Color outerColor);

    void line(float x1, float y1, float x2, float y2);

    void rect(float x, float y, float width, float height);

    void arc(float x, float y, float radius, float a0, float a1);

    void bezier(float c1x, float c1y, float c2x, float c2y, float x, float y);

    default void arc(float x, float y, float radius) {
        arc(x, y, radius, 0, (float) (2 * Math.PI));
    }

    default void roundedRect(float x, float y, float width, float height, float radius) {
        roundedRectComplex(x, y, width, height, radius, radius, radius, radius);
    }

    void roundedRectComplex(float x, float y, float width, float height,
                            float bottomLeftRadius, float bottomRightRadius,
                            float topLeftRadius, float topRightRadius);

    default void roundedRectComplex(float x, float y, float width, float height,
                                    float bottomLeftRadius, float bottomRightRadius,
                                    float topLeftRadius, float topRightRadius,
                                    Color color, boolean fill) {
        beginPath();
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
        roundedRectComplex(x, y, width, height, bottomLeftRadius, bottomRightRadius, topLeftRadius, topRightRadius);
        endPath(fill);
    }

    default void line(float x1, float y1, float x2, float y2, float lineWidth, Color color) {
        beginPath();
        strokeWidth(lineWidth);
        strokeColor(color);
        line(x1, y1, x2, y2);
        endPath(false);
    }

    default void rect(float x, float y, float width, float height, Color color, boolean fill) {
        beginPath();
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
        rect(x, y, width, height);
        endPath(fill);
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Color color, boolean fill) {
        beginPath();
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
        roundedRect(x, y, width, height, radius);
        endPath(fill);
    }

    default void roundedRect(float x, float y, float width, float height,
                             float bottomLeftRadius, float bottomRightRadius,
                             float topLeftRadius, float topRightRadius,
                             Color color, boolean fill) {
        beginPath();
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
        roundedRectComplex(x, y, width, height, bottomLeftRadius, bottomRightRadius, topLeftRadius, topRightRadius);
        endPath(fill);
    }

    default void arc(float x, float y, float radius, Color color, boolean fill) {
        beginPath();
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
        arc(x, y, radius);
        endPath(fill);
    }

    void strokeWidth(float width);

    void strokeColor(Color color);

    void fillColor(Color color);

    Color fillColor();

    Color strokeColor();

    float strokeWidth();

    void paint(IPaint paint);

    IPaint linearGradient(float startX, float startY, float endX, float endY, Color from, Color to);

    IPaint radialGradient(float centerX, float centerY, float radius, Color beginColor, Color endColor);

    IPaint radialGradient(float centerX, float centerY, float innerRadius, float outerRadius,
                          Color beginColor, Color endColor);

    IPaint imagePattern(float ox, float oy, float ex, float ey, float width, float height,
                        float angle, float alpha, IImage image);

    IFont font();

    float measureTextWidth(IFont font, String text, float fontSize, float lineHeight, float weight);

    float measureTextHeight(IFont font, String text, float fontSize, float lineHeight, float weight);

    Vector2f measureText(IFont font, String text, float fontSize, float lineHeight, float weight);

    TextMetrics measureTextMetrics(IFont font, float fontSize, String text, float maxWidth,
                                   float lineHeight, float weight, boolean wrap);

    void drawAlignedText(IFont font, float fontSize, String text,
                         float x, float y, float lineMaxWidth, float lineHeight, float weight,
                         Color color, TextAlign align, boolean wrap);

    void drawAlignedText(IFont font, float fontSize, TextMetrics textMetrics,
                         float x, float y, float lineMaxWidth, float lineHeight, float weight,
                         Color color, TextAlign align, boolean wrap);

    default float measureTextWidth(IFont font, String text, float fontSize, float lineHeight) {
        return measureTextWidth(font, text, fontSize, lineHeight, DEFAULT_FONT_WEIGHT);
    }

    default float measureTextHeight(IFont font, String text, float fontSize, float lineHeight) {
        return measureTextHeight(font, text, fontSize, lineHeight, DEFAULT_FONT_WEIGHT);
    }

    default Vector2f measureText(IFont font, String text, float fontSize, float lineHeight) {
        return measureText(font, text, fontSize, lineHeight, DEFAULT_FONT_WEIGHT);
    }

    default TextMetrics measureTextMetrics(IFont font, float fontSize, String text, float maxWidth,
                                           float lineHeight, boolean wrap) {
        return measureTextMetrics(font, fontSize, text, maxWidth, lineHeight, DEFAULT_FONT_WEIGHT, wrap);
    }

    default float measureTextWidth(String text, float fontSize, float lineHeight) {
        return measureTextWidth(font(), text, fontSize, lineHeight);
    }

    default float measureTextHeight(String text, float fontSize, float lineHeight) {
        return measureTextHeight(font(), text, fontSize, lineHeight);
    }

    default Vector2f measureText(String text, float fontSize, float lineHeight) {
        return measureText(font(), text, fontSize, lineHeight);
    }

    default TextMetrics measureTextMetrics(float fontSize, String text, float maxWidth,
                                           float lineHeight, boolean wrap) {
        return measureTextMetrics(font(), fontSize, text, maxWidth, lineHeight, wrap);
    }

    default void drawAlignedText(IFont font, float fontSize, String text,
                                 float x, float y, float lineMaxWidth, float lineHeight,
                                 Color color, TextAlign align, boolean wrap) {
        drawAlignedText(font, fontSize, text, x, y, lineMaxWidth, lineHeight, DEFAULT_FONT_WEIGHT, color, align, wrap);
    }

    default void drawAlignedText(IFont font, float fontSize, TextMetrics textMetrics,
                                 float x, float y, float lineMaxWidth, float lineHeight,
                                 Color color, TextAlign align, boolean wrap) {
        drawAlignedText(font, fontSize, textMetrics, x, y, lineMaxWidth, lineHeight, DEFAULT_FONT_WEIGHT, color, align, wrap);
    }

    default float measureTextWidth(String text, float fontSize, float lineHeight, float weight) {
        return measureTextWidth(font(), text, fontSize, lineHeight, weight);
    }

    default float measureTextHeight(String text, float fontSize, float lineHeight, float weight) {
        return measureTextHeight(font(), text, fontSize, lineHeight, weight);
    }

    default Vector2f measureText(String text, float fontSize, float lineHeight, float weight) {
        return measureText(font(), text, fontSize, lineHeight, weight);
    }

    default TextMetrics measureTextMetrics(float fontSize, String text, float maxWidth,
                                           float lineHeight, boolean wrap, float weight) {
        return measureTextMetrics(font(), fontSize, text, maxWidth, lineHeight, weight, wrap);
    }

    RenderTree renderTree();

    void draw(float zIndex, RenderLayer layer, Consumer<RenderContext> drawFunc);

    default void draw(float zIndex, Consumer<RenderContext> drawFunc) {
        draw(zIndex, RenderLayer.Content, drawFunc);
    }

    default void draw(Consumer<RenderContext> drawFunc) {
        draw(0, RenderLayer.Content, drawFunc);
    }

    void beginGroup(float zIndex, RenderLayer layer);

    default void beginGroup(float zIndex) {
        beginGroup(zIndex, RenderLayer.Content);
    }

    void endGroup();

    void deferToLayer(RenderLayer layer, float zIndex, Consumer<RenderContext> drawFunc);

    void flush();

    void beginImmediate();

    void endImmediate();

    boolean isImmediate();


    float viewportWidth();

    float viewportHeight();

    default Vector2f viewportSize() {
        return new Vector2f(viewportWidth(), viewportHeight());
    }

    IImage createImage(ITexture texture);

    void delectImage(IImage image);

}
