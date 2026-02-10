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

package io.homo.superresolution.neoforge.mixin.neoforge;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
//import net.neoforged.fml.loading.EarlyLoadingScreenController;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NATIVE_CONTEXT_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.opengl.GL11C.GL_TRUE;

@Mixin(Window.class)
public abstract class WindowMixin {
    /*
    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private boolean fullscreen;

    @Shadow
    @Final
    private ScreenManager screenManager;

    @Shadow
    private Optional<VideoMode> preferredFullscreenVideoMode;

    @Shadow
    private int windowedX;

    @Shadow
    private int windowedY;

    @Shadow
    private int x;

    @Shadow
    private int y;

    @Shadow
    protected abstract long takeOverWindow(EarlyLoadingScreenController earlyLoadingScreen, String title);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;takeOverWindow(Lnet/neoforged/fml/loading/EarlyLoadingScreenController;Ljava/lang/String;)J"))
    public long fkEarlyLoadingWindow(Window instance, EarlyLoadingScreenController earlyLoadingScreen, String title) {
        long oldHandle = this.takeOverWindow(earlyLoadingScreen, title);
        GLFW.glfwDestroyWindow(oldHandle);
        Monitor monitor = this.screenManager.getMonitor(GLFW.glfwGetPrimaryMonitor());
        GraphicsCapabilities.detectSupportedVersions();
        glfwDefaultWindowHints();
        GLFW.glfwWindowHint(139265, 196609);
        GLFW.glfwWindowHint(139275, 221185);
        GLFW.glfwWindowHint(139272, 204801);
        GLFW.glfwWindowHint(139270, 1);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        long handle = GLFW.glfwCreateWindow(this.width, this.height, title, this.fullscreen && monitor != null ? monitor.getMonitor() : 0L, 0L);
        if (monitor != null) {
            VideoMode videomode = monitor.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
            this.windowedX = monitor.getX() + videomode.getWidth() / 2 - this.width / 2;
            this.windowedY = monitor.getY() + videomode.getHeight() / 2 - this.height / 2;
            this.x = this.windowedX;
            this.y = this.windowedY;
        } else {
            int[] aint1 = new int[1];
            int[] aint = new int[1];
            GLFW.glfwGetWindowPos(handle, aint1, aint);
            this.windowedX = aint1[0];
            this.windowedY = aint[0];
            this.x = this.windowedX;
            this.y = this.windowedY;
        }
        return handle;
    }*/
}