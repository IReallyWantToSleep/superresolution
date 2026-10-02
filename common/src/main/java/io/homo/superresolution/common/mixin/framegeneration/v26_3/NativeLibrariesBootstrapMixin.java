/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.framegeneration.v26_3;

#if MC_VER == MC_26_3
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NativeLibrariesBootstrap.class)
public class NativeLibrariesBootstrapMixin {
    @Inject(method = "tryLoadingVulkan", at = @At("HEAD"), cancellable = true)
    private static void skipVulkanPreload(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class NativeLibrariesBootstrapMixin {
}
#endif
