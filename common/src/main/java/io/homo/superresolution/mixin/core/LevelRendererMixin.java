package io.homo.superresolution.mixin.core;

import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private PostChain entityEffect;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"))
    private void captureMatrix4f(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setProjectionMatrix(projectionMatrix);
        AlgorithmManager.setModelViewMatrix(new Matrix4f().identity());
    }

    @Inject(at = @At("RETURN"), method = "doEntityOutline")
    private void onLoadEntityOutlineShader(CallbackInfo ci) {
        MinecraftRenderingStates.resizeMinecraftRenderTarget();
    }

    @Inject(at = @At("RETURN"), method = "resize")
    private void onOnResized(CallbackInfo ci) {
        if (entityEffect == null) return;
        MinecraftRenderingStates.resizeMinecraftRenderTarget();
    }
}
