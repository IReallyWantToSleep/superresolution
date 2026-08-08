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

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class VulkanPresentationMinecraftBackendMixin {
    @Inject(method = "flipFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private static void super_resolution$skipOpenGlPresentation(CallbackInfo ci) {
        if (VulkanPresentationFeature.isRequested()) {
            RenderSystem.replayQueue();
            ci.cancel();
        }
    }
}
