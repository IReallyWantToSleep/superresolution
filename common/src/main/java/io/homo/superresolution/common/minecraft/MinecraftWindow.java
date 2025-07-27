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
