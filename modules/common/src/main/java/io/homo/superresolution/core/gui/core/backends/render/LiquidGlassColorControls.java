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

package io.homo.superresolution.core.gui.core.backends.render;

public record LiquidGlassColorControls(float brightness, float contrast, float saturation) {
    public static final LiquidGlassColorControls DEFAULT = new LiquidGlassColorControls(0f, 1f, 1f);

    public LiquidGlassColorControls {
        if (!Float.isFinite(brightness) || !Float.isFinite(contrast) || !Float.isFinite(saturation)) {
            throw new IllegalArgumentException("Liquid glass color controls must be finite");
        }
    }
}
