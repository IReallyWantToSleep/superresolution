package io.homo.superresolution.common.mixin.core;

#if MC_VER > MC_1_20_4

import net.minecraft.client.DeltaTracker;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif

#if MC_VER > MC_1_21_1
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
#endif

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.utils.CallType;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(at = @At(value = "HEAD"), method = "initOutline")
    private void onInitOutline(CallbackInfo ci) {
        MinecraftRenderHandle.onInitEntityEffectBegin();
    }

    @Inject(at = @At(value = "RETURN"), method = "initOutline")
    private void onInitOutlineEnd(CallbackInfo ci) {
        MinecraftRenderHandle.onInitEntityEffectEnd();
    }


    #if MC_VER <= MC_1_21_1
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"), method = "doEntityOutline")
    private void fixEntityOutlineEffect(RenderTarget instance, int width, int height, boolean disableBlend) {
        MinecraftRenderHandle.onBlitEntityEffect();
    }
    #endif

    #if MC_VER < MC_1_21_4
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;resize(II)V"), method = "resize")
    private void onResizePostChain(PostChain instance, int w, int h) {
        instance.resize(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    #if MC_VER == MC_1_21_1
    private void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrix(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_21_4
    private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrix(projectionMatrix,frustumMatrix );
    }
    #elif MC_VER == MC_1_21_5
    private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrix(projectionMatrix, frustumMatrix);
    }
    #elif MC_VER == MC_1_20_1
    private void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrix(projectionMatrix, poseStack.last().pose());
    }
    #elif MC_VER == MC_1_20_4
    private void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setMatrix(projectionMatrix, poseStack.last().pose());
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin(CallType.LEVEL_RENDERER);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd(CallType.LEVEL_RENDERER);
        }
    }
}
