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

package io.homo.superresolution.common.mixin.presentation.v26_2;

#if MC_VER >= MC_26_2
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationWindow;
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
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
                    shift =  At.Shift.AFTER
            )
    )
    private void super_resolution$renderAndPresent(
            boolean advanceGameTime, CallbackInfo ci
    ) {
        if (VulkanPresentationFeature.isRequested()) {
            VulkanPresentationWindow.endMinecraftFrame();
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"
            )
    )
    private void super_resolution$skipOpenGlBlit(GpuSurface instance, CommandEncoder commandEncoder, GpuTextureView textureView) {
        if (!VulkanPresentationFeature.isRequested()) {
            instance.blitFromTexture(commandEncoder, textureView);
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V"
            )
    )
    private void super_resolution$skipOpenGlPresent(GpuSurface instance) {
        if (!VulkanPresentationFeature.isRequested()) {
            instance.present();
        }
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;isAcquired()Z"
            )
    )
    private boolean super_resolution$isAcquired(GpuSurface instance) {
        if (!VulkanPresentationFeature.isRequested()) {
            return instance.isAcquired();
        }
        return false;
    }

    @Redirect(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;acquireNextTexture()V"
            )
    )
    private void super_resolution$acquireNextTexture(GpuSurface instance) throws SurfaceException {
        if (!VulkanPresentationFeature.isRequested()) {
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
