package io.homo.superresolution.mixin.core;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.debug.DebugInfo;
import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.upscale.AlgorithmManager;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Unique
    public float super_resolution$frameTimeDelta_algo = 16.6f;
    @Unique
    public float super_resolution$lastRenderTime_algo = -1;
    @Shadow
    @Final
    Minecraft minecraft;
    @Unique
    private boolean super_resolution$shouldResize = true;

    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoad) {
            SuperResolution.getInstance().resize(i, j);
            MinecraftRenderingStates.onResolutionChanged();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "render")
    private void onRenderStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel && this.minecraft.level != null) {
            SuperResolution.isRenderingWorld = true;
            if (super_resolution$shouldResize) {
                super_resolution$shouldResize = false;
                SuperResolution.getInstance().resize(
                        SuperResolution.getMinecraftWidth(),
                        SuperResolution.getMinecraftHeight()
                );
                MinecraftRenderingStates.onResolutionChanged();
            }
        } else {
            super_resolution$shouldResize = true;
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"), method = "render")
    private void onRenderBegin(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderingStates.setShouldScale(true);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            SuperResolution.isRenderingWorld = false;
            super_resolution$lastRenderTime_algo = Util.getMillis();
            if (Config.enableUpscale) {
                SuperResolution.currentAlgorithm.dispatch(SuperResolution.frameTimeDelta);
            }
            super_resolution$frameTimeDelta_algo = Util.getMillis() - super_resolution$lastRenderTime_algo;
            DebugInfo.setFrameTimeDeltaAlgo(super_resolution$frameTimeDelta_algo);
            MinecraftRenderingStates.setShouldScale(false);
            if (Config.enableUpscale) {
                SuperResolution.currentAlgorithm.blitToScreen(
                        minecraft.getWindow().getScreenWidth(),
                        minecraft.getWindow().getScreenHeight()
                );
            } else {
                SuperResolution.defaultAlgorithm.blitToScreen(
                        minecraft.getWindow().getScreenWidth(),
                        minecraft.getWindow().getScreenHeight()
                );
            }

        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;"), method = "renderLevel", locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void captureFov(DeltaTracker deltaTracker, CallbackInfo ci, float f, boolean bl, Camera camera, Entity entity, float g, double d) {
        AlgorithmManager.setFov(d);
    }
}
