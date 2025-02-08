package io.homo.superresolution.mixin.core;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import io.homo.superresolution.render.MinecraftRenderingStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private void onRenderStart(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        SuperResolution.setFrameTimeDelta(partialTicks * 1000);
        DebugInfo.setFrameTimeDelta(partialTicks * 1000);
        if (renderLevel && this.minecraft.level != null) {
            //SuperResolution.isRenderingWorld = true;
            if (super_resolution$shouldResize) {
                super_resolution$shouldResize = false;
                Minecraft.getInstance().resizeDisplay();
            }
        } else {
            super_resolution$shouldResize = true;
        }
    }


/*
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER), method = "renderLevel")
    private void onRenderHandItemStart(CallbackInfo ci) {
        if (Config.enableUpscale) {
            SuperResolution.isRenderingWorld = false;
            glBindFramebuffer(36160, MinecraftRenderingStates.getOriginRenderTarget().frameBufferId);
            GlStateManager._viewport(0, 0, MinecraftRenderingStates.getOriginRenderTarget().viewWidth, MinecraftRenderingStates.getOriginRenderTarget().viewHeight);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"), method = "renderLevel")
    private void onRenderHandItemEnd(CallbackInfo ci) {
        if (Config.enableUpscale) {
            SuperResolution.isRenderingWorld = true;
            glBindFramebuffer(36160, MinecraftRenderingStates.getRenderTarget().frameBufferId);
            GlStateManager._viewport(0, 0, MinecraftRenderingStates.getRenderTarget().viewWidth, MinecraftRenderingStates.getRenderTarget().viewHeight);
        }
    }*/

    /*

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderingStates.setShouldScale(true);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            SuperResolution.isRenderingWorld = false;
            MinecraftRenderingStates.setShouldScale(false);
            super_resolution$lastRenderTime_algo = Util.getMillis();
            if (Config.enableUpscale) {
                SuperResolution.currentAlgorithm.dispatch(SuperResolution.frameTimeDelta);
            }
            super_resolution$frameTimeDelta_algo = Util.getMillis() - super_resolution$lastRenderTime_algo;
            DebugInfo.setFrameTimeDeltaAlgo(super_resolution$frameTimeDelta_algo);
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
    }*/
}
