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

import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationWindow;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.platform.Window")
public abstract class VulkanPresentationGlDeviceMixin {
    @Inject(method = "updateVsync", at = @At("HEAD"), cancellable = true)
    private void super_resolution$setVulkanVsync(boolean enabled, CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            VulkanPresentationWindow.setVsync(enabled);
            ci.cancel();
        }
    }

    @Inject(
            method = "close",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwTerminate()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void super_resolution$destroyRenderWindow(CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            PresentationWindowState.destroyRenderWindow();
        }
    }
}
