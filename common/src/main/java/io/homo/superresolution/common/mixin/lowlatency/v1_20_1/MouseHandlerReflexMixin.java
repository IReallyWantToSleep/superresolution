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
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerReflexMixin {
    @Inject(method = "onPress", at = @At("HEAD"), require = 1)
    private void super_resolution$handleTriggerFlash(
            long windowPointer,
            int button,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        if (!LowLatency.isPclAvailable()) {
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            LowLatency.onTriggerFlash();
        }
    }
}
