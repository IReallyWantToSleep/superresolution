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

package io.homo.superresolution.core.gui.widgets.select;

public record MaterialSelectSize(
        float containerHeight,

        float horizontalPadding,

        float cornerRadius,

        float outlineWidth,

        float outlineFocusedWidth,

        float iconSize,

        float iconTextGap,

        float inputFontSize,

        float labelFontSize,

        float labelCenterFontSize,

        float supportingTextFontSize,

        float supportingTextTopMargin
) {
    public static final MaterialSelectSize Standard = new MaterialSelectSize(
            56,    // containerHeight
            16,    // horizontalPadding
            4,     // cornerRadius
            1,     // outlineWidth
            2,     // outlineFocusedWidth
            24,    // iconSize
            16,    // iconTextGap
            16,    // inputFontSize
            12,    // labelFontSize (floating)
            16,    // labelCenterFontSize (resting)
            12,    // supportingTextFontSize
            4      // supportingTextTopMargin
    );
    public static final MaterialSelectSize Small = new MaterialSelectSize(
            48,    // containerHeight
            12,    // horizontalPadding
            4,     // cornerRadius
            1,     // outlineWidth
            2,     // outlineFocusedWidth
            20,    // iconSize
            12,    // iconTextGap
            14,    // inputFontSize
            11,    // labelFontSize
            14,    // labelCenterFontSize
            11,    // supportingTextFontSize
            4      // supportingTextTopMargin
    );
}
