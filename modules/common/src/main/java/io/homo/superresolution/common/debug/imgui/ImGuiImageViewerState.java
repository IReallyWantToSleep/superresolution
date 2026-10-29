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

import imgui.ImVec2;
import imgui.type.ImBoolean;

public class ImGuiImageViewerState {
    public final String windowId;
    public final ImVec2 viewOffset = new ImVec2();
    public final ImBoolean open = new ImBoolean(true);
    public float zoom = 1.0f;
    public float zoomMin = 0.05f;
    public float zoomMax = 4096.0f;
    public boolean gridEnabled = true;
    public boolean viewReset = true;
    public boolean fitToWindow = true;

    public ImGuiImageViewerState(String windowId) {
        this.windowId = windowId;
    }
}
