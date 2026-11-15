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

package io.homo.superresolution.core.gui.widgets.menu;

import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialMenuGroup extends MaterialMenuGroup {
    public LiquidGlassMaterialMenuGroup(MaterialMenu menu) {
        super(menu);
    }

    @Override
    protected void drawBackground(RenderContext ctx, Rectangle bounds, float animatedHeight,
                                  float bottomLeft, float bottomRight, float topLeft, float topRight,
                                  Color backgroundColor) {
        Rectangle visibleBounds = new Rectangle(bounds.x, bounds.y, bounds.width, animatedHeight);
        LiquidGlassStyle.drawPanel(ctx, visibleBounds, bottomLeft, bottomRight, topLeft, topRight);
    }
}
