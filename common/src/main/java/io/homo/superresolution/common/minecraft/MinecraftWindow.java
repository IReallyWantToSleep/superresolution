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

package io.homo.superresolution.common.minecraft;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MinecraftWindow {
    public static boolean hasWindow() {
        return Minecraft.getInstance().getWindow() != null;
    }

    public static long getWindowHandle() {
        return hasWindow() ? Minecraft.getInstance().getWindow().getWindow() : -1;
    }

    public static int[] getWindowSize() {
        int[] sizeX = new int[]{1};
        int[] sizeY = new int[]{1};
        if (hasWindow()) {
            GLFW.glfwGetFramebufferSize(getWindowHandle(), sizeX, sizeY);
        }
        return new int[]{
                Math.max(sizeX[0], 1),
                Math.max(sizeY[0], 1)
        };
    }

    public static int getWindowWidth() {
        return getWindowSize()[0];
    }

    public static int getWindowHeight() {
        return getWindowSize()[1];
    }

    public static int[] getWindowSourceSize() {
        int[] sizeX = new int[]{1};
        int[] sizeY = new int[]{1};
        if (hasWindow()) {
            GLFW.glfwGetFramebufferSize(getWindowHandle(), sizeX, sizeY);
        }
        return new int[]{
                sizeX[0],
                sizeY[0]
        };
    }

    public static int getWindowSourceWidth() {
        return getWindowSourceSize()[0];
    }

    public static int getWindowSourceHeight() {
        return getWindowSourceSize()[1];
    }
}
