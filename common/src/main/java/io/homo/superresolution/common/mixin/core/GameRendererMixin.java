package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.math.Vector2f;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
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

    @Shadow
    #if MC_VER < MC_1_21_4
    protected abstract double getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

    #else
    protected abstract float getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

    #endif
    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoad) {
            SuperResolution.getInstance().resize(i, j);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderLevel", cancellable = true)
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin(CallType.GAME_RENDERER);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd(CallType.GAME_RENDERER);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "render")
    private void onRenderBegin(CallbackInfo ci) {
        PerformanceInfo.begin("gameRenderer");
    }

    @Inject(at = @At(value = "RETURN"), method = "render")
    private void onRenderEnd(CallbackInfo ci) {
        PerformanceInfo.end("gameRenderer");
    }

    @Inject(at = @At(value = "HEAD"), method = "renderItemInHand")
    private void onRenderHandBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandBegin();
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderItemInHand")
    private void onRenderHandEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandEnd();
        }
    }


    #if MC_VER < MC_1_21_4
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)D"), method = "renderLevel")
    private double onGetFov(GameRenderer instance, Camera d0, float fogtype, boolean b) {
        AlgorithmManager.param.verticalFov = getFov(d0, fogtype, b);
        return AlgorithmManager.param.verticalFov;
    }
    #else
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)F"), method = "renderLevel")
    private float onGetFov(GameRenderer instance, Camera d0, float fogtype, boolean b) {
        AlgorithmManager.param.verticalFov = getFov(d0, fogtype, b);
        return (float) AlgorithmManager.param.verticalFov;
    }
    #endif

    #if MC_VER > MC_1_21_1
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;", ordinal = 0))
    public Matrix4f applyJitterToProjectionMatrix(GameRenderer instance, float fov)
     #else
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;", ordinal = 0))
    public Matrix4f applyJitterToProjectionMatrix(GameRenderer instance, double fov)
    #endif {
        if (SuperResolutionAPI.getCurrentAlgorithm() != null) {
            Vector2f currentJitter = AlgorithmManager.getJitterOffset();
            Matrix4f projectionMatrix = new Matrix4f(instance.getProjectionMatrix(fov));
            projectionMatrix.mul(new Matrix4f().translation(
                    currentJitter.x,
                    currentJitter.y,
                    0.0f
            ));
            return projectionMatrix;
        }
        return instance.getProjectionMatrix(fov);
    }
}
