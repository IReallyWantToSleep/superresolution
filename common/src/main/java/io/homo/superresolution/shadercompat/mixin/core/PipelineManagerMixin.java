/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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
import io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PipelineManager.class,remap = false)
public class PipelineManagerMixin {
    @Inject(method = "preparePipeline",at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/shaderpack/materialmap/WorldRenderingSettings;clearReloadRequired()V"))
    private void reset(NamespacedId currentDimension, CallbackInfoReturnable<WorldRenderingPipeline> cir){
        IrisShaderCompatUpscaleDispatcher.reset();
        SuperResolution.recreateAlgorithm();
    }
}
