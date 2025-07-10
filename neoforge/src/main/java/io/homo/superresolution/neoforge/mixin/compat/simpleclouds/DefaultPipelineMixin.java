package io.homo.superresolution.neoforge.mixin.compat.simpleclouds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.DefaultPipeline;
import io.homo.superresolution.api.SuperResolutionAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DefaultPipeline.class, remap = false)
public class DefaultPipelineMixin {
    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    public void bindWrite(RenderTarget instance, boolean setViewport) {
        instance.bindWrite(true);
    }

    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V"))
    public void blitToScreen(RenderTarget instance, int width, int height, boolean disableBlend) {
        instance.blitToScreen(
                SuperResolutionAPI.getScreenWidth(),
                SuperResolutionAPI.getScreenHeight(),
                disableBlend
        );
        GlStateManager._viewport(0, 0, SuperResolutionAPI.getRenderWidth(), SuperResolutionAPI.getRenderHeight());
    }
}
