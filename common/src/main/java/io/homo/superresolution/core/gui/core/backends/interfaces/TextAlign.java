/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import org.lwjgl.nanovg.NanoVG;

public record TextAlign(TextAlignType horizontal, TextAlignType vertical) {
    //horizontal
    public static final int ALIGN_LEFT = NanoVG.NVG_ALIGN_LEFT;
    public static final int ALIGN_CENTER = NanoVG.NVG_ALIGN_CENTER;
    public static final int ALIGN_RIGHT = NanoVG.NVG_ALIGN_RIGHT;
    //vertical
    public static final int ALIGN_TOP = NanoVG.NVG_ALIGN_TOP;
    public static final int ALIGN_MIDDLE = NanoVG.NVG_ALIGN_MIDDLE;
    public static final int ALIGN_BOTTOM = NanoVG.NVG_ALIGN_BOTTOM;

    public static TextAlign of(TextAlignType horizontal, TextAlignType vertical) {
        return new TextAlign(horizontal, vertical);
    }
}
