/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.lowlatency.v1_20_1;

import io.homo.superresolution.common.lowlatency.LowLatency;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerReflexMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 1)
    private void super_resolution$handleLatencyPing(
            long windowPointer,
            int key,
            int scanCode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        if (!LowLatency.isPclAvailable() || key != GLFW.GLFW_KEY_F13) {
            return;
        }
        LowLatency.onLatencyPing(action == GLFW.GLFW_PRESS);
        ci.cancel();
    }
}
