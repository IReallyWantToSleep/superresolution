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

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialLabel extends MaterialWidget<MaterialLabel> {
    private Supplier<String> textSupplier = () -> "";

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

    @Override
    protected void init() {
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
            Rectangle bounds = getBounds();
            TextMetrics textMetrics = ctx.measureTextMetrics(
                    ctx.font(),
                    style().fontSize(),
                    text,
                    bounds.width,
                    style().lineHeight(),
                    style().wrap()
            );
            Vector2f textSize = new Vector2f(textMetrics.maxLineWidth, textMetrics.totalHeight);
            setElementSize(textSize.x, textSize.y);

            Color textColor = getTextColor();
            ctx.beginGroup(style().zIndex());
            ctx.drawAlignedText(
                    ctx.font(),
                    style().fontSize(),
                    text,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    style().lineHeight(),
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