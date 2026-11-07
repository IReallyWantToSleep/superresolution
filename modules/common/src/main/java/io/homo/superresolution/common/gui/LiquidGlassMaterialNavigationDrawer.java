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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialNavigationDrawer extends MaterialNavigationDrawer {
    private static final LiquidGlassParameters LIGHT_PARAMETERS =
            new LiquidGlassParameters(45f, 0.8f, 0.65f, 60f, 0f, 5f);
    private static final LiquidGlassParameters DARK_PARAMETERS =
            new LiquidGlassParameters(45f, 0.8f, 0.8f, 75f, 0.46f, 5f);

    @Override
    protected void renderBackground(RenderContext ctx, Rectangle bounds) {
        boolean dark = scheme().theme() == MaterialTheme.Dark;
        LiquidGlassStyle.draw(
                ctx,
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                16f,
                dark ? DARK_PARAMETERS : LIGHT_PARAMETERS,
                dark ? Color.rgba(28, 27, 32, Math.round(255f * 0.45f))
                        : Color.rgba(255, 255, 255, Math.round(255f * 0.35f))
        );
    }
}
