package io.homo.superresolution.forge.mixin.compat.simpleclouds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.ShaderSupportPipeline;
import io.homo.superresolution.api.SuperResolutionAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShaderSupportPipeline.class, remap = false)
public class ShaderSupportPipelineMixin {
    @Redirect(method = "afterSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V", ordinal = 0))
    public void blitToScreenA(RenderTarget instance, int width, int height, boolean disableBlend) {

    }

    @Redirect(method = "afterLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(IIZ)V", ordinal = 0))
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
}
