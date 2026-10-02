/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.presentation.v26_3;

#if MC_VER == MC_26_3
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class VulkanPresentationGameRendererCaptureMixin {
    @Inject(method = "render()V", at = @At("HEAD"))
    private void super_resolution$beginRealFrameRendering(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationBackendManager.beginRealFrameRendering();
        }
    }

    @Inject(
            method = "render()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V",
                    shift = At.Shift.AFTER
            )
    )
    private void super_resolution$captureHudlessColor(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            FrameCaptureManager.captureHudlessColor();
        }
    }

    @Inject(method = "render()V", at = @At("RETURN"), order = 2000)
    private void super_resolution$captureFinalColor(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            FrameCaptureManager.captureFinalColor();
        }
    }

    @Inject(method = "render()V", at = @At("RETURN"), order = 3000)
    private void super_resolution$endRealFrameRendering(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationBackendManager.endRealFrameRendering();
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationGameRendererCaptureMixin {
}
#endif
