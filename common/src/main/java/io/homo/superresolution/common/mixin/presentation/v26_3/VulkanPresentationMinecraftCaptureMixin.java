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
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationMinecraftCaptureMixin {
    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render()V",
                    shift = At.Shift.AFTER
            )
    )
    private void super_resolution$renderAndPresent(boolean advanceGameTime, CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationBackendManager.endMinecraftFrame();
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"
            )
    )
    private void super_resolution$skipOpenGlBlit(GpuSurface instance, CommandEncoder commandEncoder, GpuTextureView textureView) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            instance.blitFromTexture(commandEncoder, textureView);
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;present()V"
            )
    )
    private void super_resolution$skipOpenGlPresent(GpuSurface instance) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            instance.present();
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;isAcquired()Z"
            )
    )
    private boolean super_resolution$isAcquired(GpuSurface instance) {
        return !PresentationBackendManager.isVulkanPresentationRequested() && instance.isAcquired();
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;acquireNextTexture()V"
            )
    )
    private void super_resolution$acquireNextTexture(GpuSurface instance) throws SurfaceException {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            instance.acquireNextTexture();
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationMinecraftCaptureMixin {
}
#endif
