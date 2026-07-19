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

package io.homo.superresolution.common.mixin.presentation.v1_21_1;

#if MC_VER >= MC_1_21 && MC_VER < MC_1_21_2
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class VulkanPresentationWindowMixin {
    @Unique
    private static final String HELPER_TITLE = "Super Resolution OpenGL Context";


    @Shadow
    @Final
    private long window;

    @Redirect(method = "<init>",at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
    private void wtf(long window){
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void super_resolution$clearPresentationHandle(CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            PresentationWindowState.clearPresentationAfterWindowClose();
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", unsafe = true,ordinal = 5,shift = At.Shift.AFTER))
    private void super_resolution$redirectWindow(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        }
    }
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V", unsafe = true))
    private void super_resolution$a(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            long vulkanWindow = this.window;
            long openglWindow;
            PresentationWindowState.attachPresentation(vulkanWindow);
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GraphicsCapabilities.getHighestOpenGLVersion().left());
            GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsCapabilities.getHighestOpenGLVersion().right());

            openglWindow = GLFW.glfwCreateWindow(1, 1, HELPER_TITLE, 0L, 0L);
            if (openglWindow == 0L) {
                throw new IllegalStateException("Failed to create the hidden OpenGL helper window");
            }
            PresentationWindowState.attachRender(openglWindow);
        }

        if (VulkanPresentationFeature.isRequested()){
            GLFW.glfwMakeContextCurrent(PresentationWindowState.renderHandle());
        } else {
            GLFW.glfwMakeContextCurrent(window);
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationWindowMixin {
}
#endif
