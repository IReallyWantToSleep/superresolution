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

package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import org.lwjgl.glfw.GLFW;

public class MouseCursor {
    public static final MouseCursor HAND = new MouseCursor(GLFW.GLFW_HAND_CURSOR);
    public static final MouseCursor CROSSHAIR = new MouseCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
    public static final MouseCursor IBEAM = new MouseCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final MouseCursor NOT_ALLOWED = new MouseCursor(GLFW.GLFW_NOT_ALLOWED_CURSOR);
    public static final MouseCursor ARROW = new MouseCursor(GLFW.GLFW_ARROW_CURSOR);

    public final int id;
    private long glfwCursor = -1;

    private MouseCursor(int id) {
        this.id = id;
    }

    public void use() {
        if (glfwCursor == -1 || glfwCursor == 0) {
            glfwCursor = GLFW.glfwCreateStandardCursor(id);
        } else {
            GLFW.glfwSetCursor(
                    MinecraftWindow.getWindowHandle(),
                    glfwCursor
            );
        }
    }
}
