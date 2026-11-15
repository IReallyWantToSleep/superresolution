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

package io.homo.superresolution.core.gui.widgets.glass;

import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassColorControls;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassTint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.utils.Color;

public final class LiquidGlassStyle {
    public static final LiquidGlassParameters CONTROL =
            new LiquidGlassParameters(-45f, 0.15f, 0.8f, 20f, 0.5f, 4f);
    private static final LiquidGlassParameters LIGHT_PANEL =
            new LiquidGlassParameters(-45f, 0.8f, 0.8f, 80f, 0.79f, 1f);
    private static final LiquidGlassParameters DARK_PANEL =
            new LiquidGlassParameters(-45f, 0.8f, 0.8f, 80f, 0.73f, 1f);

    private LiquidGlassStyle() {
    }

    public static void drawPanel(RenderContext ctx, Rectangle bounds, float radius) {
        drawPanel(ctx, bounds, radius, radius, radius, radius);
    }

    public static void drawPanel(RenderContext ctx, Rectangle bounds,
                                 float bottomLeft, float bottomRight, float topLeft, float topRight) {
        boolean dark = MaterialUI.Scheme.theme() == MaterialTheme.Dark;
        draw(ctx, bounds.x, bounds.y, bounds.width, bounds.height,
                bottomLeft, bottomRight, topLeft, topRight,
                dark ? DARK_PANEL : LIGHT_PANEL,
                dark ? Color.rgba(28, 27, 32, Math.round(255f * 0.4f))
                        : Color.rgba(255, 255, 255, Math.round(255f * 0.13f)));
    }

    public static void draw(RenderContext ctx, float x, float y, float width, float height, float radius,
                            LiquidGlassParameters parameters, Color tint) {
        draw(ctx, x, y, width, height, radius, radius, radius, radius, parameters, tint);
    }

    public static void draw(RenderContext ctx, float x, float y, float width, float height,
                            float bl, float br, float tl, float tr,
                            LiquidGlassParameters parameters, Color tint) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        ctx.roundedRectLiquidGlass(x, y, width, height, bl, br, tl, tr,
                parameters, LiquidGlassColorControls.DEFAULT, LiquidGlassTint.from(tint, 0f));
    }
}
