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

package io.homo.superresolution.core.gui.widgets.navigation.drawer;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialNavigationDrawerHeader extends MaterialWidget<MaterialNavigationDrawerHeader> {
    private static final float HEIGHT = 56f;
    private static final float ICON_SIZE = 24f;
    private static final float FONT_SIZE = 16f;
    private static final float ICON_MARGIN_LEFT = 16f;
    private static final float ICON_TEXT_GAP = 12f;

    private Supplier<String> titleSupplier = () -> "";
    private Supplier<MaterialSymbol> iconSupplier = () -> null;

    public MaterialNavigationDrawerHeader() {
        getLayoutNode().setDebugName("NavigationDrawerHeader");
        layout().setHeight(HEIGHT);
        layout().setWidthPercent(100);
    }

    public static MaterialNavigationDrawerHeader create() {
        return new MaterialNavigationDrawerHeader();
    }

    @Override
    protected void init() {
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Rectangle bounds = getBounds();

        float contentX = bounds.x + ICON_MARGIN_LEFT;
        float centerY = bounds.y + bounds.height / 2;

        MaterialSymbol icon = iconSupplier.get();
        if (icon != null) {
            Vector2f iconPos = new Vector2f(contentX + ICON_SIZE / 2, centerY);
            icon.render(ctx, scheme().onSurface(), ICON_SIZE, iconPos);
            contentX += ICON_SIZE + ICON_TEXT_GAP;
        }

        String title = titleSupplier.get();
        if (title != null && !title.isEmpty()) {
            //TODO:使用bold变体的字体或许更好
            ctx.drawAlignedText(
                    ctx.font(),
                    FONT_SIZE,
                    title,
                    contentX,
                    centerY,
                    bounds.width - contentX + bounds.x,
                    bounds.height,
                    scheme().onSurfaceVariant(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );

            ctx.drawAlignedText(
                    ctx.font(),
                    FONT_SIZE,
                    title,
                    contentX,
                    centerY,
                    bounds.width - contentX + bounds.x,
                    bounds.height,
                    scheme().onSurfaceVariant(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }
    }

    public MaterialNavigationDrawerHeader title(String title) {
        this.titleSupplier = () -> title;
        return this;
    }

    public MaterialNavigationDrawerHeader title(Supplier<String> supplier) {
        this.titleSupplier = supplier;
        return this;
    }

    public MaterialNavigationDrawerHeader icon(MaterialSymbol icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public MaterialNavigationDrawerHeader icon(Supplier<MaterialSymbol> supplier) {
        this.iconSupplier = supplier;
        return this;
    }

    public float computeContentWidth(RenderContext ctx) {
        float width = ICON_MARGIN_LEFT;
        if (iconSupplier.get() != null) {
            width += ICON_SIZE + ICON_TEXT_GAP;
        }
        String title = titleSupplier.get();
        if (title != null && !title.isEmpty()) {
            width += ctx.measureTextWidth(title, FONT_SIZE, FONT_SIZE);
        }
        width += ICON_MARGIN_LEFT;
        return width;
    }
}
