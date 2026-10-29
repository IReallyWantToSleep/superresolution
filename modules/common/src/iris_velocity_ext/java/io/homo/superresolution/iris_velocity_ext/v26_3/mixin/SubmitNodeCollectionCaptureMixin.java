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

package io.homo.superresolution.iris_velocity_ext.v26_3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_3.VelocitySubmitStorage;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionCaptureMixin {
    @Inject(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;waterMask()Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private <S> void irisExt$capture(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            UvMapping uvMapping,
            int outlineColor,
            CallbackInfo ci,
            @Local ModelFeatureRenderer.Submit<S> submit
    ) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) submit).irisExt$captureCache();
        }
    }

    @WrapOperation(
            method = "submitBlockModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;hasBlending()Z"
            )
    )
    private <S> boolean irisExt$captureBlockModel(
            RenderType instance,
            Operation<Boolean> original,
            @Local BlockModelFeatureRenderer.Submit submit
    ) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) submit).irisExt$captureCache();
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "submitCustomGeometry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;hasBlending()Z"
            )
    )
    private boolean irisExt$captureCustomGeometry(
            RenderType instance,
            Operation<Boolean> original,
            @Local CustomFeatureRenderer.Submit submit
    ) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            ((VelocitySubmitStorage) (Object) submit).irisExt$captureCache();
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "submitItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
            )
    )
    private void irisExt$captureItem(
            FeatureRenderPhase<?> phase,
            SubmitNode submit,
            Operation<Void> original
    ) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()
                && submit instanceof ItemFeatureRenderer.Submit itemSubmit) {
            ((VelocitySubmitStorage) (Object) itemSubmit).irisExt$captureCache();
        }
        original.call(phase, submit);
    }

    @WrapOperation(
            method = "submitItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
            )
    )
    private void irisExt$captureItemSimple(
            SimpleFeatureRenderPhase phase,
            SubmitNode submit,
            Operation<Void> original
    ) {
        if (SuperResolutionConfig.isIrisExtensionEnabledAtStartup()
                && submit instanceof ItemFeatureRenderer.Submit itemSubmit) {
            ((VelocitySubmitStorage) (Object) itemSubmit).irisExt$captureCache();
        }
        original.call(phase, submit);
    }
}
