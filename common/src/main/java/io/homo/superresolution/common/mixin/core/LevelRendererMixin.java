package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
#if MC_VER > MC_1_20_1
import net.minecraft.client.DeltaTracker;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import io.homo.superresolution.common.render.gl.texture.Texture;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private PostChain entityEffect;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private boolean super_resolution$needResize = true;
    @Unique
    private int super_resolution$frameCount = 0;

    @Shadow
    public abstract void resize(int width, int height);

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"))
    #if MC_VER > MC_1_20_1
    private void captureMatrix4f(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setProjectionMatrix(projectionMatrix);
        AlgorithmManager.setModelViewMatrix(new Matrix4f().identity());
    }
    #else
    private void captureMatrix4f(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        AlgorithmManager.setProjectionMatrix(projectionMatrix);
        AlgorithmManager.setModelViewMatrix(new Matrix4f().identity());
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "initOutline")
    private void onInitOutline(CallbackInfo ci) {
        MinecraftRenderingStates.setClientRenderTarget(MinecraftRenderingStates.getRenderTarget());
    }

    @Inject(at = @At(value = "RETURN", target = "Lnet/minecraft/client/renderer/PostChain;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/server/packs/resources/ResourceManager;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lnet/minecraft/resources/ResourceLocation;)V"), method = "initOutline")
    private void onInitOutlineDone(CallbackInfo ci) {
        MinecraftRenderingStates.setClientRenderTarget(MinecraftRenderingStates.getOriginRenderTarget());
    }

    @Inject(at = @At("RETURN"), method = "doEntityOutline")
    private void onLoadEntityOutlineShader(CallbackInfo ci) {
        if (entityEffect == null) return;
        if (super_resolution$frameCount == 3) {
            entityEffect.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"), method = "doEntityOutline")
    private void fixEntityOutlineEffect(RenderTarget instance, int width, int height, boolean disableBlend) {
        Texture.blitToScreen(
                instance.width,
                instance.height,
                MinecraftRenderingStates.getScreenWidth(),
                MinecraftRenderingStates.getScreenHeight(),
                instance.getColorTextureId()
        );
    }


    @Inject(at = @At("RETURN"), method = "resize")
    private void onResized(CallbackInfo ci) {
        if (minecraft.level == null) return;
        MinecraftRenderingStates.resizeMinecraftRenderTarget();
        entityEffect.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;resize(II)V"), method = "resize")
    private void onResizePostChain(PostChain instance, int w, int h) {
        super_resolution$needResize = true;
        instance.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
    }

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Config.isEnableUpscale())
            ((PostChainAccessor) entityEffect).setScreenTarget(MinecraftRenderingStates.getRenderTarget());
        super_resolution$frameCount++;
        if (super_resolution$needResize) {
            entityEffect.resize(MinecraftRenderingStates.getRenderWidth(), MinecraftRenderingStates.getRenderHeight());
            super_resolution$needResize = false;
        }
        if (Minecraft.getInstance().level != null) {
            SuperResolution.isRenderingWorld = true;
            if (Config.isEnableUpscale()) MinecraftRenderingStates.setShouldScale(true);
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            SuperResolution.isRenderingWorld = false;
            if (Config.isEnableUpscale()) MinecraftRenderingStates.setShouldScale(false);
            if (Config.isEnableUpscale()) {
                SuperResolution.currentAlgorithm.dispatch(SuperResolution.frameTimeDelta);
                SuperResolution.currentAlgorithm.blitToScreen(
                        minecraft.getWindow().getScreenWidth(),
                        minecraft.getWindow().getScreenHeight()
                );
            }
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "allChanged")
    private void onReloadDone(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().resizeDisplay();
        }
    }
}
