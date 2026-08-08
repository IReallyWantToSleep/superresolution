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

import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class VulkanPresentationGameRendererCaptureMixin {
    @Inject(
            method = "render(FJZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void super_resolution$captureHudlessColor(
            float partialTicks,
            long nanoTime,
            boolean renderLevel,
            CallbackInfo ci
    ) {
        if (VulkanPresentationFeature.isRequested()) {
            FrameCaptureManager.captureHudlessColor();
        }
    }

    @Inject(method = "render(FJZ)V", at = @At("RETURN"))
    private void super_resolution$captureFinalColor(
            float partialTicks,
            long nanoTime,
            boolean renderLevel,
            CallbackInfo ci
    ) {
        if (VulkanPresentationFeature.isRequested()) {
            FrameCaptureManager.captureFinalColor();
        }
    }
}
