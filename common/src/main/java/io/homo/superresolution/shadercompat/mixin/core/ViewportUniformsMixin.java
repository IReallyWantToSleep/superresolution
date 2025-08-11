package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.uniforms.ViewportUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewportUniforms.class)
public class ViewportUniformsMixin {
    @Inject(method = "addViewportUniforms", at = @At("RETURN"), remap = false)
    private static void addUniforms(UniformHolder uniforms, CallbackInfo ci) {
        if (!MinecraftRenderHandle.isShaderPackCompat() || !SuperResolutionConfig.isEnableUpscale()) return;

        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScale",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getRenderScaleFactor() : 1
        );
        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRatio",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getUpscaleRatio() : 1
        );
    }
}
