/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.label;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;

import java.util.function.Function;
import java.util.function.Supplier;

public class MaterialLabel extends MaterialWidget<MaterialLabel> {
    private Supplier<String> textSupplier = () -> "";
    private Function<MaterialScheme, Color> colorSupplier = (scheme) -> scheme.theme() == MaterialTheme.Dark ? Color.rgb(255, 255, 255) : Color.black();
    //cached infos
    private float cachedTextSize;
    private float cachedLineHeight;
    private boolean cachedWrap;
    private float cachedWeight;
    private String cachedText;
    private Rectangle cachedBounds;
    private TextMetrics cachedTextMetrics;

    public MaterialLabel() {
        this.style = new MaterialLabelStyle();
        getLayoutNode().setDebugName("MaterialLabel");
    }

    public static MaterialLabel create() {
        return new MaterialLabel();
    }

    public MaterialLabel text(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public MaterialLabel text(Supplier<String> supplier) {
        this.textSupplier = supplier;
        return this;
    }

    public MaterialLabel color(Color color) {
        style().color(color);
        this.colorSupplier = (scheme) -> color;
        return this;
    }

    public MaterialLabel color(Function<MaterialScheme, Color> color) {
        this.colorSupplier = color;
        return this;
    }


    public MaterialLabel fontSize(float fontSize) {
        style().fontSize(fontSize);
        return this;
    }

    public MaterialLabel lineHeight(float lineHeight) {
        style().lineHeight(lineHeight);
        return this;
    }

    public MaterialLabel weight(float weight) {
        style().weight(weight);
        return this;
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        String text = textSupplier.get();
        style().color(colorSupplier.apply(scheme()));
        if (text.equals(cachedText) &&
                style().fontSize() == cachedTextSize &&
                style().lineHeight() == cachedLineHeight &&
                style().wrap() == cachedWrap &&
                style().weight() == cachedWeight &&
                getBounds().equals(cachedBounds)
        ) {
            if (style().sizeToContent()) {
                setElementWidth(cachedTextMetrics.maxLineWidth);
            }
            setElementHeight(cachedTextMetrics.totalHeight);
        } else {
            cachedText = text;
            cachedTextSize = style().fontSize();
            cachedLineHeight = style().lineHeight();
            cachedWrap = style().wrap();
            cachedWeight = style().weight();
            cachedBounds = getBounds();
            float maxWidth = cachedBounds.width > 0 ? cachedBounds.width : Float.MAX_VALUE;

            cachedTextMetrics = ctx.measureTextMetrics(
                    ctx.font(),
                    style().fontSize(),
                    text,
                    maxWidth,
                    style().lineHeight(),
                    style().weight(), style().wrap()
            );
            if (style().sizeToContent()) {
                setElementWidth(cachedTextMetrics.maxLineWidth);
            }
            setElementHeight(cachedTextMetrics.totalHeight);
        }
    }

    @Override
    public MaterialLabelStyle style() {
        return (MaterialLabelStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        String text = textSupplier.get();
        if (text != null && !text.isEmpty()) {
            if (!text.equals(cachedText)) {
                layouting(ctx);
            }
            Rectangle bounds = getBounds();
            float maxWidth = bounds.width > 0 ? bounds.width : Float.MAX_VALUE;

            Color textColor = getTextColor();
            ctx.beginGroup(style().zIndex());
            ctx.drawAlignedText(
                    ctx.font(),
                    style().fontSize(),
                    cachedTextMetrics,
                    bounds.x,
                    bounds.y,
                    maxWidth,
                    style().lineHeight(),
                    style().weight(),
                    textColor,
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    style().wrap()
            );
            ctx.endGroup();
        }
    }

    private Color getTextColor() {
        if (style().color() != null) {
            return isDisabled() ?
                    style().color().copy().alpha((int) (255 * 0.38)) :
                    style().color();
        } else {
            return isDisabled() ?
                    scheme().onSurface().copy().alpha((int) (255 * 0.38)) :
                    scheme().onSurface();
        }
    }
}