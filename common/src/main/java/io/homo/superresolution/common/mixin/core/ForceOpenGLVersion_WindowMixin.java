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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

import static org.lwjgl.glfw.GLFW.*;
#if MC_VER > MC_1_21_11
@Mixin(value = com.mojang.blaze3d.opengl.GlBackend.class)
#else
@Mixin(value = com.mojang.blaze3d.platform.Window.class)
#endif
public class ForceOpenGLVersion_WindowMixin {
    #if MC_VER > MC_1_21_11
    @org.spongepowered.asm.mixin.injection.Inject(method = "setWindowHints", at = @At(value = "TAIL"))
    private void forceOpenGLVersion(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        //#if !IS_VULKAN
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        //#endif
    }

    #else
    @org.spongepowered.asm.mixin.injection.Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 4), remap = false)
    private void forceOpenGLVersion(int hint, int value) {
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        glfwWindowHint(hint, value);
    }
    #endif
}