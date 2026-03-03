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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.google.material.dynamiccolor.Variant;

/**
 * Material Design 3 color scheme variants
 */
public enum SchemeVariant {
    MONOCHROME(Variant.MONOCHROME),
    NEUTRAL(Variant.NEUTRAL),
    TONAL_SPOT(Variant.TONAL_SPOT),
    VIBRANT(Variant.VIBRANT),
    EXPRESSIVE(Variant.EXPRESSIVE),
    FIDELITY(Variant.FIDELITY),
    CONTENT(Variant.CONTENT),
    RAINBOW(Variant.RAINBOW),
    FRUIT_SALAD(Variant.FRUIT_SALAD);

    public final Variant variant;

    SchemeVariant(Variant variant) {
        this.variant = variant;
    }
}

