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
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(value = Window.class)
public class ForceOpenGLVersion_WindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 5), remap = false)
    private void forceOpenGLVersion(int hint, int value) {
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        //glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);

        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());
        glfwWindowHint(hint, value);
    }
}