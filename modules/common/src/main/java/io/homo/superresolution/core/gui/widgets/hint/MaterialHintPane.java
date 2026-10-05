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

package io.homo.superresolution.core.gui.widgets.hint;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialHintPane extends MaterialWidget<MaterialHintPane> {
    private Supplier<MaterialSymbol> iconSupplier = MaterialSymbols::iconInfo;
    private Supplier<String> titleSupplier = () -> "";
    private Supplier<String> textSupplier = () -> "";

    public MaterialHintPane() {
        this.style = new MaterialHintPaneStyle();
        getLayoutNode().setDebugName("MaterialHintPane");
    }

    public static MaterialHintPane create() {
        return new MaterialHintPane();
    }

    @Override
    public MaterialHintPaneStyle style() {
        return (MaterialHintPaneStyle) style;
    }

    public MaterialHintPane icon(MaterialSymbol icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public MaterialHintPane iconProvider(Supplier<MaterialSymbol> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public MaterialHintPane title(String title) {
        this.titleSupplier = () -> title;
        return this;
    }

    public MaterialHintPane titleProvider(Supplier<String> titleSupplier) {
        this.titleSupplier = titleSupplier;
        return this;
    }

    public MaterialHintPane text(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public MaterialHintPane textProvider(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    @Override
    protected void init() {
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    @Override
    public void layouting(RenderContext ctx) {
        MaterialHintPaneStyle s = style();
        Rectangle bounds = getBounds();
        float availableWidth = bounds.width > 0 ? bounds.width - s.paddingHorizontal() * 2 : 0;
        if (availableWidth <= 0) {
            setElementHeight(s.paddingVertical() * 2 + s.iconSize());
            return;
        }

        float headerRowHeight = Math.max(s.iconSize(), s.titleFontSize() * s.lineHeightRatio());
        float contentMaxWidth = availableWidth - s.textHorizontalPadding() * 2;

        float contentTextHeight = 0;
        String text = textSupplier.get();
        if (text != null && !text.isEmpty() && contentMaxWidth > 0) {
            float contentLineHeight = s.contentFontSize() * s.lineHeightRatio();
            TextMetrics metrics = ctx.measureTextMetrics(
                    s.contentFontSize(), text, contentMaxWidth, contentLineHeight, true, s.contentFontWeight());
            contentTextHeight = metrics.totalHeight;
        }

        float totalHeight = s.paddingVertical() + headerRowHeight;
        if (contentTextHeight > 0) {
            totalHeight += s.headerContentGap() + contentTextHeight;
        }
        totalHeight += s.paddingVertical();

        setElementHeight(totalHeight);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        MaterialHintPaneStyle s = style();
        Rectangle bounds = getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) return;

        ctx.roundedRect(
                bounds.x, bounds.y, bounds.width, bounds.height,
                s.cornerRadius(),
                scheme().surfaceContainerLow(),
                true
        );

        float contentX = bounds.x + s.paddingHorizontal();
        float headerY = bounds.y + s.paddingVertical();
        float availableWidth = bounds.width - s.paddingHorizontal() * 2;

        MaterialSymbol icon = iconSupplier.get();
        float iconCenterY = headerY + s.iconSize() / 2f;
        if (icon != null) {
            icon.render(ctx, scheme().secondary(), s.iconSize(), new Vector2f(contentX + s.iconSize() / 2f, iconCenterY));
        }

        String title = titleSupplier.get();
        float titleX = contentX + s.iconSize() + s.headerGap();
        float titleMaxWidth = availableWidth - s.iconSize() - s.headerGap();
        if (title != null && !title.isEmpty() && titleMaxWidth > 0) {
            float titleLineHeight = s.titleFontSize() * s.lineHeightRatio();
            float titleCenterY = headerY + s.iconSize() / 2f;
            ctx.drawAlignedText(
                    ctx.font(),
                    s.titleFontSize(),
                    title,
                    titleX,
                    iconCenterY,
                    titleMaxWidth,
                    titleLineHeight,
                    s.titleFontWeight(),
                    scheme().onSurface(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }

        String text = textSupplier.get();
        if (text != null && !text.isEmpty()) {
            float contentTextX = contentX + s.textHorizontalPadding();
            float headerRowHeight = Math.max(s.iconSize(), s.titleFontSize() * s.lineHeightRatio());
            float contentTextY = headerY + headerRowHeight + s.headerContentGap();
            float contentMaxWidth = availableWidth - s.textHorizontalPadding() * 2;
            float contentLineHeight = s.contentFontSize() * s.lineHeightRatio();

            if (contentMaxWidth > 0) {
                TextMetrics metrics = ctx.measureTextMetrics(
                        s.contentFontSize(), text, contentMaxWidth, contentLineHeight, true, s.contentFontWeight());
                ctx.drawAlignedText(
                        ctx.font(),
                        s.contentFontSize(),
                        metrics,
                        contentTextX,
                        contentTextY,
                        contentMaxWidth,
                        contentLineHeight,
                        s.contentFontWeight(),
                        scheme().onSurfaceVariant(),
                        TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                        true
                );
            }
        }
    }
}
