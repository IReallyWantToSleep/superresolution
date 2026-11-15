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

package io.homo.superresolution.common.gui.widgets.glass;

import io.homo.superresolution.common.gui.config.pages.ConfigPageContext;
import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassTitlePill extends ConfigPageContext.TitlePill {
    private static final LiquidGlassParameters LIGHT_PARAMETERS =
            new LiquidGlassParameters(45f, 0.8f, 0.8f, 20f, 0.5f, 4f);

    public LiquidGlassTitlePill(String text, float fontSize, float minHeight, float horizontalPadding, float radius) {
        super(text, fontSize, minHeight, horizontalPadding, radius);
    }

    @Override
    protected void renderBackground(RenderContext ctx, Rectangle bounds) {
        boolean dark = scheme().theme() == MaterialTheme.Dark;
        LiquidGlassStyle.draw(
                ctx,
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                getBackgroundRadius(bounds),
                dark ? LiquidGlassStyle.CONTROL : LIGHT_PARAMETERS,
                dark ? Color.rgba(44, 42, 51, Math.round(255f * 0.8f))
                        : Color.rgba(247, 242, 250, Math.round(255f * 0.4f))
        );
    }
}
