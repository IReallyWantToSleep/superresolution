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
import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class VulkanPresentationWindowCloseMixin {
    @Inject(method = "close", at = @At("TAIL"))
    private void super_resolution$clearPresentationHandle(CallbackInfo ci) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            PresentationWindowState.clearPresentationAfterWindowClose();
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationWindowCloseMixin {
}
#endif
