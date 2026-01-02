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

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;

import java.util.function.Supplier;

public class MaterialNavigationDrawerSectionHeader extends MaterialWidget<MaterialNavigationDrawerSectionHeader> {
    private static final float HEIGHT = 56f;
    private static final float FONT_SIZE = 14f;
    private static final float PADDING_LEFT = 16f;

    private Supplier<String> titleSupplier = () -> "";

    public MaterialNavigationDrawerSectionHeader() {
        getLayoutNode().setDebugName("NavigationDrawerSectionHeader");
        layout().setHeight(HEIGHT);
        layout().setWidthPercent(100);
    }

    public static MaterialNavigationDrawerSectionHeader create() {
        return new MaterialNavigationDrawerSectionHeader();
    }

    public static MaterialNavigationDrawerSectionHeader create(String title) {
        return new MaterialNavigationDrawerSectionHeader().title(title);
    }

    @Override
    protected void init() {
    }

    @Override
    protected boolean isInteractive() {
        return false;
    }

    public MaterialNavigationDrawerSectionHeader title(String title) {
        this.titleSupplier = () -> title;
        return this;
    }

    public MaterialNavigationDrawerSectionHeader title(Supplier<String> supplier) {
        this.titleSupplier = supplier;
        return this;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Rectangle bounds = getBounds();

        String title = titleSupplier.get();
        if (title != null && !title.isEmpty()) {
            ctx.drawAlignedText(
                    ctx.font(),
                    FONT_SIZE,
                    title,
                    bounds.x + PADDING_LEFT,
                    bounds.getCenterY(),
                    bounds.width - PADDING_LEFT,
                    bounds.height,
                    scheme().onSurfaceVariant(),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }
    }
}
