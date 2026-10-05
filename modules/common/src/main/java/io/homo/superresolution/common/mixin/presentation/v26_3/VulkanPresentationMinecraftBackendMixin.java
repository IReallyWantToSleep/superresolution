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
import com.mojang.renderpearl.backend.opengl.GlBackend;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.window.VulkanPresentationGlBackend;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PreferredGraphicsApi.class)
public abstract class VulkanPresentationMinecraftBackendMixin {
    @Redirect(
            method = "getBackendsToTry",
            at = @At(value = "NEW", target = "com/mojang/renderpearl/backend/opengl/GlBackend")
    )
    private GlBackend super_resolution$createPresentationBackend() {
        return PresentationBackendManager.isVulkanPresentationRequested()
                ? new VulkanPresentationGlBackend()
                : new GlBackend();
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class VulkanPresentationMinecraftBackendMixin {
}
#endif
