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

package io.homo.superresolution.common.debug.imgui;

public final class ImGuiDebugColors {
    private ImGuiDebugColors() {
    }

    public static final int TITLE_BAR_BADGE = rgba(57, 86, 122, 255);
    public static final int WINDOW_BUTTON_ON = rgba(76, 122, 64, 255);
    public static final int WINDOW_BUTTON_ON_HOVERED = rgba(92, 145, 77, 255);
    public static final int WINDOW_BUTTON_ON_ACTIVE = rgba(63, 101, 54, 255);
    public static final int WINDOW_BUTTON_OFF = rgba(70, 74, 82, 255);
    public static final int WINDOW_BUTTON_OFF_HOVERED = rgba(89, 95, 106, 255);
    public static final int WINDOW_BUTTON_OFF_ACTIVE = rgba(58, 62, 69, 255);
    public static final int VIEWER_BORDER = rgba(255, 255, 255, 255);
    public static final int VIEWER_BACKGROUND = rgba(100, 100, 100, 255);
    public static final int VIEWER_GRID = rgba(255, 255, 255, 100);

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24)
                | ((b & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (r & 0xFF);
    }
}
