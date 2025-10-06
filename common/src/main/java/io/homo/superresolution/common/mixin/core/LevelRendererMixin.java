/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

import io.homo.superresolution.common.SuperResolution;

#if MC_VER > MC_1_20_6
import net.minecraft.client.DeltaTracker;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif

#if MC_VER > MC_1_21_1
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
#endif

#if MC_VER > MC_1_21_5
import com.mojang.blaze3d.buffers.GpuBufferSlice;
#endif

import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.handler.MinecraftRenderHandler;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.ShaderCompatHandler;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique
    private static boolean superresolution$windowIsHide = false;

    #if MC_VER < MC_1_21_4
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;resize(II)V"), method = "resize")
    private void onResizePostChain(PostChain instance, int w, int h) {
        if (ShaderCompatHandler.isShaderPackCompatSuperResolution()) return;
        instance.resize(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight());
    }
    #endif


    @Inject(method = "renderLevel", at = @At("HEAD"))
    #if MC_VER == MC_1_21_1
    private void renderLevel_MC_1_21_1(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_21_4
    private void renderLevel_MC_1_21_4(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix,frustumMatrix );
    }
    #elif MC_VER == MC_1_21_5
    private void renderLevel_MC_1_21_5(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_20_1
    private void renderLevel_MC_1_20_1(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, poseStack.last().pose());
    }
    #elif MC_VER == MC_1_20_4
    private void renderLevel_MC_1_20_4(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, poseStack.last().pose());
    }
    #elif MC_VER == MC_1_20_6
    private void renderLevel_MC_1_20_6(float partialTick, long nanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_21_6
    private void renderLevel_MC_1_21_6(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, org.joml.Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_21_9
    private void renderLevel_MC_1_21_9(GraphicsResourceAllocator p_361796_, DeltaTracker p_348530_, boolean p_109603_, Camera p_109604_, Matrix4f p_254120_, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice p_425977_, Vector4f p_425544_, boolean p_426302_, CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(projectionMatrix, frustumMatrix);
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "renderLevel", cancellable = true)
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderWorldBegin(CallType.LEVEL_RENDERER);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderWorldEnd(CallType.LEVEL_RENDERER);
        }
    }
}
