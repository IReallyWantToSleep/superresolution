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

package io.homo.superresolution.iris_velocity_ext.v26_3.mixin.feature;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_3.VelocityRenderContext;
import io.homo.superresolution.iris_velocity_ext.v26_3.VelocitySubmitStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererVelocityMixin {
    @Inject(method = "prepareModel", at = @At("HEAD"))
    private <S> void irisExt$restore(ModelFeatureRenderer.Submit<S> submit, CallbackInfo ci) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) submit).irisExt$restoreCache();
        }
    }

    @Inject(method = "prepareModel", at = @At("RETURN"))
    private <S> void irisExt$clear(ModelFeatureRenderer.Submit<S> submit, CallbackInfo ci) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            VelocityRenderContext.clearCache();
        }
    }
}
