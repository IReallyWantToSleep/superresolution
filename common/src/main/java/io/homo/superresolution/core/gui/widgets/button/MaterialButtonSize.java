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

package io.homo.superresolution.core.gui.widgets.button;

public record MaterialButtonSize(
        float height,
        float padding,
        float iconPadding,
        float roundCornerSize,
        float squareCornerSize,
        float pressedCornerSize,
        float iconSize,
        float fontSize
) {
    public static final MaterialButtonSize ExtraSmall = new MaterialButtonSize(
            32,
            12,
            4,
            0,// full
            12,
            8,
            20,
            14
    );

    public static final MaterialButtonSize Small = new MaterialButtonSize(
            40,
            16,
            8,
            0, // full
            12,
            8,
            20,
            14
    );

    public static final MaterialButtonSize Medium = new MaterialButtonSize(
            56,
            24,
            8,
            0, // full
            16,
            12,
            24,
            16
    );

    public static final MaterialButtonSize Large = new MaterialButtonSize(
            96,
            48,
            12,
            0, // full
            28,
            16,
            32,
            24
    );

    public static final MaterialButtonSize ExtraLarge = new MaterialButtonSize(
            136,
            64,
            16,
            0, // full
            28,
            16,
            40,
            32
    );
}
