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

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoaded) {
            SuperResolution.getInstance().resize(i, j);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderLevel", cancellable = true)
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            PerformanceTracker.push("Level Render");
            RenderHandlerManager.onRenderWorldBegin(CallType.GAME_RENDERER);
        }
    }

    #if MC_VER > MC_1_21_8
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"), method = "renderLevel")
    private void cancelDepthClear(
            com.mojang.blaze3d.systems.CommandEncoder commandEncoder,
            com.mojang.blaze3d.textures.GpuTexture gpuTexture,
            double depth
    ) {
        //这里必需取消深度清除，不然后面捕获不到深度
        if (
                !Platform.currentPlatform.iris().isShaderPackInUse() //没启用光影时
                        ||
                        !SuperResolutionConfig.isEnableUpscale()//禁用超分时
        ) {
            commandEncoder.clearDepthTexture(
                    gpuTexture,
                    depth
            );
        }
    }
    #endif

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderTarget renderTarget = MinecraftUtils.getMainRenderTarget();
            RenderHandlerManager.onRenderWorldEnd(CallType.GAME_RENDERER);
            #if MC_VER > MC_1_21_8
            if (
                    !(!Platform.currentPlatform.iris().isShaderPackInUse() //没启用光影时
                            ||
                            !SuperResolutionConfig.isEnableUpscale()//禁用超分时
                    )
            ) {
                if (renderTarget.getDepthTexture() != null) {
                    com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(
                            renderTarget.getDepthTexture(),
                            1.0f
                    );
                }
            }
            #endif
            PerformanceTracker.pop("Level Render");
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
    private void onRenderBegin(CallbackInfo ci) {
        //#if MC_VER < MC_1_20_6
        if (io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceWidth() < 1 || io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceHeight() < 1) {
            ci.cancel();
        }
        //#endif
        PerformanceTracker.push("Main Render");
    }

    @Inject(at = @At(value = "RETURN"), method = "render")
    private void onRenderEnd(CallbackInfo ci) {
        PerformanceTracker.pop("Main Render");
    }

    @Inject(at = @At(value = "HEAD"), method = "renderItemInHand")
    private void onRenderHandBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderHandBegin();
        }
    }


    @Inject(at = @At(value = "RETURN"), method = "renderItemInHand")
    private void onRenderHandEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderHandEnd();
        }
    }
}
