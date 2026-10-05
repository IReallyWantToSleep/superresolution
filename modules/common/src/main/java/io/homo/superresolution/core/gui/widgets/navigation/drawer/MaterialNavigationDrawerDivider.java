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
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;

public class MaterialNavigationDrawerDivider extends MaterialWidget<MaterialNavigationDrawerDivider> {
    private static final float HEIGHT = 1f;
    private static final float VERTICAL_PADDING = 8f;
    private static final float HORIZONTAL_PADDING = 16f;

    public MaterialNavigationDrawerDivider() {
        getLayoutNode().setDebugName("NavigationDrawerDivider");
        layout().setHeight(HEIGHT);
        layout().setWidthPercent(100);
        layout().setMargin(YogaEdge.VERTICAL,4);
    }

    public static MaterialNavigationDrawerDivider create() {
        return new MaterialNavigationDrawerDivider();
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

        Color dividerColor = scheme().outlineVariant();

        float lineY = bounds.y + bounds.height / 2;
        float lineStartX = bounds.x + HORIZONTAL_PADDING;
        float lineEndX = bounds.x + bounds.width - HORIZONTAL_PADDING;
        float lineWidth = lineEndX - lineStartX;

        if (lineWidth > 0) {
            ctx.rect(
                    lineStartX,
                    lineY - HEIGHT / 2,
                    lineWidth,
                    HEIGHT,
                    dividerColor,
                    true
            );
        }
    }
}
