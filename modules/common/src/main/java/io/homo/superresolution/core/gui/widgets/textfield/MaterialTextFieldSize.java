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

package io.homo.superresolution.core.gui.widgets.textfield;

public record MaterialTextFieldSize(
        float containerHeight,

        float horizontalPadding,

        float cornerRadius,

        float outlineWidth,

        float focusedOutlineWidth,

        float indicatorHeight,

        float focusedIndicatorHeight,

        float iconSize,

        float iconTextGap,

        float inputFontSize,

        float labelFontSize,

        float labelCenterFontSize,

        float floatingInputVerticalOffset,

        float supportingTextFontSize,

        float supportingTextTopMargin
) {
    public static final MaterialTextFieldSize Standard = new MaterialTextFieldSize(
            56f,
            16f,
            4f,
            1f,
            2f,
            1f,
            2f,
            24f,
            12f,
            16f,
            12f,
            16f,
            8f,
            12f,
            4f
    );

    public static final MaterialTextFieldSize Compact = new MaterialTextFieldSize(
            48f,
            12f,
            4f,
            1f,
            2f,
            1f,
            2f,
            20f,
            10f,
            14f,
            11f,
            14f,
            6f,
            11f,
            4f
    );
}
