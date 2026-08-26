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
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomFeatureRenderer.class)
public class CustomFeatureRendererVelocityMixin {
    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    private void irisExt$restore(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci, @Local SubmitNodeStorage.CustomGeometrySubmit modelSubmit) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        ((VelocitySubmitStorage) (Object) modelSubmit).irisExt$restoreCache();
    }

    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("RETURN"))
    private void irisExt$clear(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        VelocityRenderContext.clear();
    }
}
