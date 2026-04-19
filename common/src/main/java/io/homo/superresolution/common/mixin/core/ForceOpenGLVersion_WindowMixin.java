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

package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(value = Window.class)
public class ForceOpenGLVersion_WindowMixin {
    #if MC_VER > MC_1_21_11
    @Redirect(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuBackend;setWindowHints()V"), remap = false)
    private static void forceOpenGLVersion(com.mojang.blaze3d.systems.GpuBackend instance) {
        instance.setWindowHints();
        //#if !IS_VULKAN
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        //#endif
    }

    @Inject(method = "createGlfwWindow", at = @At(value = "TAIL"), remap = false)
    private static void showWindow(int width, int height, String title, long monitor, com.mojang.blaze3d.systems.GpuBackend backend, CallbackInfoReturnable<Long> cir) {
        //TODO:**SR**似乎会导致游戏窗口不会自己出现（？），暂时先这样解决
        if (cir.getReturnValueJ() > 0)glfwShowWindow(cir.getReturnValueJ());
    }
    #else
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 4), remap = false)
    private void forceOpenGLVersion(int hint, int value) {
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        glfwWindowHint(hint, value);
    }
    #endif
}