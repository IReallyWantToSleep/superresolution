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

package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract Window getWindow();

    @Shadow
    @Nullable
    public ClientLevel level;
    @Shadow
    @Nullable
    public Screen screen;
    @Unique
    private int super_resolution$cacheWidth = 0;
    @Unique
    private int super_resolution$cacheHeight = 0;

    @Inject(at = @At(value = "RETURN"), method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci) {
        SuperResolution.gameIsLoaded = true;
    }

    @Inject(at = @At(value = "HEAD"), method = "runTick")
    private void onRenderBegin(CallbackInfo ci) {
        if (super_resolution$cacheWidth != RenderHandlerManager.getScreenWidth() || super_resolution$cacheHeight != RenderHandlerManager.getScreenHeight()) {
            super_resolution$cacheWidth = RenderHandlerManager.getScreenWidth();
            super_resolution$cacheHeight = RenderHandlerManager.getScreenHeight();
            Minecraft.getInstance().resizeDisplay();
        }
        org.lwjgl.opengl.GL11.glViewport(0, 0, RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
        PerformanceTracker.push("Frame");
        RenderHandlerManager.onFrameBegin();
    }

    @Inject(at = @At(value = "RETURN"), method = "runTick")
    private void onRenderEnd(CallbackInfo ci) {
        RenderHandlerManager.onFrameEnd();
        PerformanceTracker.pop("Frame");
    }

    @Inject(method = "resizeDisplay", at = @At(value = "HEAD"), cancellable = true)
    private void onResize(CallbackInfo ci) {
        if (
                io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceWidth() <= 1 ||
                        io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceHeight() <= 1
        ) {
            ci.cancel();
        }
    }
}
