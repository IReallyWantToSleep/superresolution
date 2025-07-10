package io.homo.superresolution.forge.mixin.compat.simpleclouds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.DefaultPipeline;
import io.homo.superresolution.api.SuperResolutionAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DefaultPipeline.class, remap = false)
public class DefaultPipelineMixin {
    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V", ordinal = 0))
    public void blitToScreenA(RenderTarget instance, int width, int height, boolean disableBlend) {
    }

    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V", ordinal = 1))
    public void blitToScreenB(RenderTarget instance, int width, int height, boolean disableBlend) {
        instance.blitToScreen(
                Math.max(SuperResolutionAPI.getRenderWidth(), SuperResolutionAPI.getScreenWidth()),
                Math.max(SuperResolutionAPI.getRenderHeight(), SuperResolutionAPI.getScreenHeight()),
                disableBlend
        );
        GlStateManager._viewport(
                0,
                0,
                SuperResolutionAPI.getRenderWidth(),
                SuperResolutionAPI.getRenderHeight()
        );
    }

    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Ldev/nonamecrackers2/simpleclouds/client/framebuffer/FrameBufferUtils;blitTargetPreservingAlpha(Lcom/mojang/blaze3d/pipeline/RenderTarget;II)V"))
    public void blitToScreenBC(RenderTarget target, int width, int height) {
    }
}
