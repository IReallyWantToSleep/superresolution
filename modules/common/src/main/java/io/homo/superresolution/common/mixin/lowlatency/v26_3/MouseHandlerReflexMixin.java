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
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.sdl.SDLMouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerReflexMixin {
    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), require = 1)
    private void super_resolution$handleTriggerFlash(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (LowLatency.isPclAvailable()
                && rawButtonInfo.button() == SDLMouse.SDL_BUTTON_LEFT
                && action == 1) {
            LowLatency.onTriggerFlash();
        }
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MouseHandlerReflexMixin {
}
#endif
