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
import com.mojang.blaze3d.systems.GpuSurface;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlSurface")
public abstract class VulkanPresentationGlDeviceMixin {
    @Inject(method = "present", at = @At("HEAD"), cancellable = true)
    private void super_resolution$skipOpenGlPresentation(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            ci.cancel();
        }
    }

    @Inject(method = "configure", at = @At("HEAD"), cancellable = true)
    private void super_resolution$setVulkanVsync(GpuSurface.Configuration config, CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationBackendManager.setVsync(config.presentMode() == GpuSurface.PresentMode.FIFO);
            ci.cancel();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void super_resolution$destroyRenderWindow(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationWindowState.destroyRenderWindow();
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationGlDeviceMixin {
}
#endif
