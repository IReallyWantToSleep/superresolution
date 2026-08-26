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

package io.homo.superresolution.iris_velocity_ext.v26_1.mixin.feature;

import com.llamalad7.mixinextras.sugar.Local;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocitySubmitStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererVelocityMixin {
    @Inject(method = "renderTranslucents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$TranslucentModelSubmit;modelSubmit()Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;"))
    private void irisExt$restore(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, List<SubmitNodeStorage.TranslucentModelSubmit<?>> list, MultiBufferSource.BufferSource bufferSource2, CallbackInfo ci, @Local SubmitNodeStorage.TranslucentModelSubmit<?> modelSubmit) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        ((VelocitySubmitStorage) (Object) modelSubmit.modelSubmit()).irisExt$restoreCache();
    }

    @Inject(method = "renderBatch", at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;", ordinal = 1))
    private void irisExt$restore2(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> map, MultiBufferSource.BufferSource bufferSource2, CallbackInfo ci, @Local SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        ((VelocitySubmitStorage) (Object) modelSubmit).irisExt$restoreCache();
    }

    @Inject(method = {"renderTranslucents", "renderBatch"}, at = @At("RETURN"))
    private void irisExt$clear(CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        VelocityRenderContext.clear();
    }
}
