/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.lowlatency.v26_3;

#if MC_VER == MC_26_3
import io.homo.superresolution.common.lowlatency.LowLatency;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLScancode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerReflexMixin {
    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void super_resolution$handleLatencyPing(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (!LowLatency.isPclAvailable() || event.key() != SDLScancode.SDL_SCANCODE_F13) {
            return;
        }
        LowLatency.onLatencyPing(action == 1);
        ci.cancel();
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class KeyboardHandlerReflexMixin {
}
#endif
