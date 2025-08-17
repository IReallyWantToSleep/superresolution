package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.shadercompat.IrisShaderPipelineHandle;
import io.homo.superresolution.shadercompat.SRCompatShaderPack;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.uniforms.ViewportUniforms;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewportUniforms.class)
public class ViewportUniformsMixin {
    @Inject(method = "addViewportUniforms", at = @At("RETURN"), remap = false)
    private static void addUniforms(UniformHolder uniforms, CallbackInfo ci) {
        if (SuperResolutionConfig.isForceDisableShaderCompat())
            return;

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
        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScaleLog2",
                () -> Math.log(SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getUpscaleRatio() : 1)
        );
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSize",
                () -> new Vector2f(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
        );
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSize",
                () -> new Vector2f(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
        );

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSizeI",
                () -> new Vector2i(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
        );
        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSizeI",
                () -> new Vector2i(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
        );
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRJitterOffset",
                () -> {
                    io.homo.superresolution.core.math.Vector2f jitterOffset = AlgorithmManager.getJitterOffset();
                    return new Vector2f(jitterOffset.x, jitterOffset.y);
                }
        );
    }
}
