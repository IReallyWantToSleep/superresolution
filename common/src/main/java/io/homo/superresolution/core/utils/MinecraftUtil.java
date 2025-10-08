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

package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import org.joml.Vector2f;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MinecraftUtil {
    public static Vector2f getScreenSize() {
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(MinecraftWindow.getWindowHandle(), w, h);
        return new Vector2f(Math.max(1, w[0]), Math.max(1, h[0]));
    }
}
