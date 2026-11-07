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

import io.homo.superresolution.core.utils.Color;

/**
 * Material tint and adaptive legibility from liquid.sksl, independent of color correction.
 */
public record LiquidGlassTint(float red, float green, float blue, float alpha, float legibility) {
    public static final LiquidGlassTint DEFAULT =
            new LiquidGlassTint(10f / 255f, 11f / 255f, 15f / 255f, 95f / 255f, 0.32f);

    public LiquidGlassTint {
        if (!unit(red) || !unit(green) || !unit(blue) || !unit(alpha) || !unit(legibility)) {
            throw new IllegalArgumentException("Liquid glass tint components must be finite and in [0, 1]");
        }
    }

    public static LiquidGlassTint from(Color color, float legibility) {
        return new LiquidGlassTint(color.red() / 255f, color.green() / 255f,
                color.blue() / 255f, color.alpha() / 255f, legibility);
    }

    private static boolean unit(float value) {
        return Float.isFinite(value) && value >= 0f && value <= 1f;
    }
}
