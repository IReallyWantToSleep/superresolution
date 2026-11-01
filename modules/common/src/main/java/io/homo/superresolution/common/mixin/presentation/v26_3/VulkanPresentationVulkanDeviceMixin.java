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

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.window.VulkanPresentationGpuSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(VulkanDevice.class)
public abstract class VulkanPresentationVulkanDeviceMixin {
    @Inject(method = "createSurface", at = @At("HEAD"), cancellable = true)
    private void super_resolution$createSurface(long windowHandle, BooleanSupplier isIconified, CallbackInfoReturnable<GpuSurfaceBackend> cir) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            cir.setReturnValue(new VulkanPresentationGpuSurface((VulkanDevice) (Object) this, windowHandle, isIconified));
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationVulkanDeviceMixin {
}
#endif
