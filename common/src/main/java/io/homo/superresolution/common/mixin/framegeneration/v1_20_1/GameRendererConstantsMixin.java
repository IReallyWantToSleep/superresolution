/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.framegeneration.v1_20_1;

import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.framegeneration.constants.MinecraftCameraState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererConstantsMixin {
    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;",
                    ordinal = 0
            ),
            index = 0,
            require = 1
    )
    private double super_resolution$captureFov(double fov) {
        MinecraftCameraState.fov = (float) fov;
        return fov;
    }

    @Inject(method = "render(FJZ)V", at = @At("HEAD"), require = 1)
    private void super_resolution$beginConstantsFrame(
            float partialTicks,
            long nanoTime,
            boolean renderLevel,
            CallbackInfo ci
    ) {
        FGConstantsFeature.beginRenderFrame();
    }

    @Inject(method = "render(FJZ)V", at = @At("RETURN"), require = 1)
    private void super_resolution$endConstantsFrame(
            float partialTicks,
            long nanoTime,
            boolean renderLevel,
            CallbackInfo ci
    ) {
        FGConstantsFeature.endRenderFrame();
    }
}
