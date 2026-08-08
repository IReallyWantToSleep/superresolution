/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.presentation.v1_20_1;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.WindowEventHandler;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Minecraft 1.20.1 Vulkan-presentation window bridge.
 *
 * <p>Vanilla can be turned into a GLFW_NO_API presentation window before it is created.
 * Forge 1.20.1 is different: its immediate/early display can hand an already-created
 * OpenGL window to Minecraft. In that case the existing window has to stay alive as the
 * OpenGL render context and a second GLFW_NO_API window becomes Minecraft's presentation
 * handle.</p>
 */
@Mixin(Window.class)
public abstract class VulkanPresentationWindowMixin {
    @Unique
    private static final String HELPER_TITLE = "Super Resolution OpenGL Context";

    /**
     * Mutable only for the Forge early-window fallback. In a normal source build this is
     * preferable to the binary hotfix's reflection against the re-obfuscated field name.
     */
    @Shadow
    @Final
    @Mutable
    private long window;

    @ModifyConstant(
            method = "<init>",
            constant = @Constant(intValue = GLFW_OPENGL_API, ordinal = 0)
    )
    private int super_resolution$setNoApiClient(int originalClientApi) {
        return VulkanPresentationFeature.isRequested() ? GLFW_NO_API : originalClientApi;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"
            )
    )
    private void super_resolution$createRenderContext(
            long constructorWindow,
            WindowEventHandler eventHandler,
            ScreenManager screenManager,
            DisplayData displayData,
            String preferredFullscreenVideoMode,
            String title
    ) {
        if (!VulkanPresentationFeature.isRequested()) {
            GLFW.glfwMakeContextCurrent(constructorWindow);
            return;
        }

        String presentationTitle = title == null ? "Minecraft" : title;
        long createdPresentationWindow = 0L;
        long createdHelperWindow = 0L;
        boolean replacedConstructorWindow = false;
        try {
            if (GLFW.glfwGetWindowAttrib(constructorWindow, GLFW_CLIENT_API) == GLFW_OPENGL_API) {
                // Forge 1.20.1's early display has already created and handed off an
                // OpenGL-capable window. Window hints cannot change an existing window's
                // client API, so keep it as the render context and create a clean Vulkan
                // presentation window instead.
                SuperResolution.LOGGER.warn(
                        "Forge 1.20.1 early OpenGL window detected; keeping it as the "
                                + "render context and creating a clean GLFW_NO_API Vulkan presentation window"
                );

                int[] width = {1};
                int[] height = {1};
                int[] x = {0};
                int[] y = {0};
                GLFW.glfwGetWindowSize(constructorWindow, width, height);
                GLFW.glfwGetWindowPos(constructorWindow, x, y);

                GLFW.glfwDefaultWindowHints();
                GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
                GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
                GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);

                createdPresentationWindow = GLFW.glfwCreateWindow(
                        Math.max(width[0], 1),
                        Math.max(height[0], 1),
                        presentationTitle,
                        0L,
                        0L
                );
                if (createdPresentationWindow == 0L) {
                    throw new IllegalStateException(
                            "Failed to create clean GLFW_NO_API presentation window for "
                                    + "Forge 1.20.1 early-window compatibility"
                    );
                }

                GLFW.glfwSetWindowPos(createdPresentationWindow, x[0], y[0]);

                // Minecraft and every downstream presentation path must now see the
                // GLFW_NO_API window. The original Forge window remains the GL context.
                this.window = createdPresentationWindow;
                replacedConstructorWindow = true;
                PresentationWindowState.attachPresentation(createdPresentationWindow);
                PresentationWindowState.attachRender(constructorWindow);
                GLFW.glfwMakeContextCurrent(constructorWindow);
            } else {
                // Normal path: Minecraft's own window was successfully created as
                // GLFW_NO_API. Create a tiny hidden OpenGL helper context.
                PresentationWindowState.attachPresentation(this.window);

                GLFW.glfwDefaultWindowHints();
                GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
                GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
                GLFW.glfwWindowHint(
                        GLFW_CONTEXT_VERSION_MAJOR,
                        GraphicsCapabilities.getHighestOpenGLVersion().left()
                );
                GLFW.glfwWindowHint(
                        GLFW_CONTEXT_VERSION_MINOR,
                        GraphicsCapabilities.getHighestOpenGLVersion().right()
                );

                createdHelperWindow = GLFW.glfwCreateWindow(1, 1, HELPER_TITLE, 0L, 0L);
                if (createdHelperWindow == 0L) {
                    throw new IllegalStateException("Failed to create the hidden OpenGL helper window");
                }

                PresentationWindowState.attachRender(createdHelperWindow);
                GLFW.glfwMakeContextCurrent(PresentationWindowState.renderHandle());
            }
        } catch (Throwable throwable) {
            PresentationWindowState.resetAfterStartupFailure();
            if (replacedConstructorWindow) {
                this.window = constructorWindow;
            }
            super_resolution$destroyCreatedWindow(createdHelperWindow);
            super_resolution$destroyCreatedWindow(createdPresentationWindow);
            VulkanPresentationFeature.disableAfterFailure(throwable);

            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(throwable);
        } finally {
            GLFW.glfwDefaultWindowHints();
        }
    }

    @Unique
    private static void super_resolution$destroyCreatedWindow(long handle) {
        if (handle == 0L) {
            return;
        }
        if (GLFW.glfwGetCurrentContext() == handle) {
            GL.setCapabilities(null);
            GLFW.glfwMakeContextCurrent(0L);
        }
        GLFW.glfwDestroyWindow(handle);
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void super_resolution$clearPresentationHandle(CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            PresentationWindowState.clearPresentationAfterWindowClose();
        }
    }
}
