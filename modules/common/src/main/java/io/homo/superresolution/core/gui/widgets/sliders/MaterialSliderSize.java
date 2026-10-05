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

package io.homo.superresolution.core.gui.widgets.sliders;

public record MaterialSliderSize(
        float trackCornerSize,

        float trackHeight,

        float handleWidthPress,

        float handleWidth,

        float handleHeight,

        float insetIconSize,

        float stepsSize,

        float stepsHorizontalPadding,

        float stepsVerticalPadding,

        float valueIndicatorTextHorizontalPadding,

        float valueIndicatorTextVerticalPadding,

        float valueIndicatorBottomPadding,

        float handleHorizontalPadding
) {

    public static final MaterialSliderSize ExtraSmall = new MaterialSliderSize(
            8,
            16,
            2,
            4,
            44,
            0,
            4,
            4,
            6,
            16,
            12,
            4,
            6
    );

    public static final MaterialSliderSize Small = new MaterialSliderSize(
            8,
            24,
            2,
            4,
            44,
            0,
            4,
            4,
            6,
            16,
            12,
            4,
            6
    );

    public static final MaterialSliderSize Medium = new MaterialSliderSize(
            12,
            40,
            2,
            4,
            52,
            24,
            4,
            4,
            6,
            16,
            12,
            4,
            6
    );

    public static final MaterialSliderSize Large = new MaterialSliderSize(
            16,
            56,
            2,
            4,
            68,
            24,
            4,
            4,
            6,
            16,
            12,
            4,
            6
    );

    public static final MaterialSliderSize ExtraLarge = new MaterialSliderSize(
            28,
            96,
            2,
            4,
            108,
            32,
            4,
            4,
            6,
            16,
            12,
            4,
            6
    );
}
