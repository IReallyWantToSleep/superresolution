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
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererConstantsMixin {
    @Inject(method = "render()V", at = @At("HEAD"), require = 1)
    private void super_resolution$beginConstantsFrame(CallbackInfo ci) {
        FGConstantsFeature.beginRenderFrame();
    }

    @Inject(method = "render()V", at = @At("RETURN"), require = 1)
    private void super_resolution$endConstantsFrame(CallbackInfo ci) {
        FGConstantsFeature.endRenderFrame();
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class GameRendererConstantsMixin {
}
#endif
