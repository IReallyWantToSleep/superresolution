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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.gui.MaterialTheme;
import io.homo.superresolution.core.gui.core.backends.render.LiquidGlassParameters;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.glass.LiquidGlassStyle;
import io.homo.superresolution.core.utils.Color;

public class LiquidGlassMaterialSwitch extends MaterialSwitch {
    private static final LiquidGlassParameters DARK_HANDLE =
            new LiquidGlassParameters(45f, 0.4f, 0.8f, 20f, 0.5f, 4f);

    public static LiquidGlassMaterialSwitch create() {
        return new LiquidGlassMaterialSwitch();
    }

    @Override
    protected Color getCheckedTrackColor() {
        return isDisabled() ? super.getCheckedTrackColor()
                : scheme().primary().copy().alpha(Math.round(255 * (isDark() ? 0.4f : 0.6f)));
    }

    @Override
    protected Color getUncheckedTrackColor() {
        return isDisabled() ? super.getUncheckedTrackColor()
                : scheme().surfaceContainerHighest().copy().alpha(Math.round(255 * (isDark() ? 0.4f : 0.55f)));
    }

    @Override
    protected Color getCheckedHandleColor() {
        return isDisabled() || !isDark() ? super.getCheckedHandleColor()
                : scheme().onPrimary().copy().alpha(Math.round(255 * 0.5f));
    }

    @Override
    protected Color getUncheckedHandleColor() {
        return isDisabled() ? super.getUncheckedHandleColor()
                : scheme().outline().copy().alpha(Math.round(255 * 0.8f));
    }

    @Override
    protected void drawTrack(RenderContext ctx, Rectangle bounds, Color color) {
        if (isDisabled()) {
            super.drawTrack(ctx, bounds, color);
            return;
        }
        LiquidGlassStyle.draw(
                ctx, bounds.x, bounds.y,
                MaterialSwitchSize.Default.trackWidth(), MaterialSwitchSize.Default.trackHeight(),
                MaterialSwitchSize.Default.trackHeight() / 2,
                LiquidGlassStyle.CONTROL, color);
    }

    @Override
    protected void drawHandle(RenderContext ctx, float centerX, float centerY, float size, Color color) {
        if (isDisabled() || (!isDark() && getCheckedProgress() >= 0.999f)) {
            super.drawHandle(ctx, centerX, centerY, size, color);
            return;
        }
        LiquidGlassStyle.draw(ctx, centerX - size / 2, centerY - size / 2,
                size, size, size / 2, DARK_HANDLE, color);
    }

    private boolean isDark() {
        return scheme().theme() == MaterialTheme.Dark;
    }
}
