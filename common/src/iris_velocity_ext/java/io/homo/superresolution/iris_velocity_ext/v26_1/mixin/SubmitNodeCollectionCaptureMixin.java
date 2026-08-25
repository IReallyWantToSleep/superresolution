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

package io.homo.superresolution.iris_velocity_ext.v26_1.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocitySubmitStorage;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionCaptureMixin {
    @WrapOperation(method = "submitModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"))
    private <E> void irisExt$capture(ModelFeatureRenderer.Storage instance, RenderType renderType, SubmitNodeStorage.ModelSubmit<?> e, Operation<Void> original) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) e).irisExt$captureKey();
        }
        original.call(instance, renderType, e);
    }

    @WrapOperation(method = "submitModelPart", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"))
    private <E> void irisExt$capture3(ModelPartFeatureRenderer.Storage instance, RenderType renderType, SubmitNodeStorage.ModelPartSubmit e, Operation<Void> original) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) e).irisExt$captureKey();
        }
        original.call(instance, renderType, e);
    }

    @WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private <E> boolean irisExt$capture4(List instance, E e, Operation<Boolean> original) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) e).irisExt$captureKey();
        }
        return original.call(instance, e);
    }
}
