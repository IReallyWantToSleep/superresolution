/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceConverter;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public class IrisRenderingPipelineMixin {
    @Inject(method = "beginLevelRendering", at = @At("HEAD"), remap = false)
    private void beginLevelRendering(CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferProjection(),
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferModelView()
        );
    }

    @Redirect(method = "beginLevelRendering", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/pathways/colorspace/ColorSpaceConverter;rebuildProgram(IILnet/irisshaders/iris/pathways/colorspace/ColorSpace;)V"), remap = false)
    private void replaceRebuildColorSpaceConvertShaderParameters(
            ColorSpaceConverter instance,
            int width,
            int height,
            ColorSpace colorSpace
    ) {
        instance.rebuildProgram(
                SuperResolutionAPI.getScreenWidth(),
                SuperResolutionAPI.getScreenHeight(),
                colorSpace
        );
    }
}
