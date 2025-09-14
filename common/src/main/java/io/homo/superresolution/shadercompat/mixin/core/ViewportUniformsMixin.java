/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmManager;
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
                () -> new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
        );
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSize",
                () -> new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
        );

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
        );
        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
        );
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRJitterOffset",
                () -> {
                    if (!SuperResolution.getCurrentAlgorithm().isSupportJitter()) return new Vector2f(0);
                    io.homo.superresolution.core.math.Vector2f jitterOffset = AlgorithmManager.getJitterOffset();
                    return new Vector2f(jitterOffset.x, jitterOffset.y);
                }
        );
    }
}
