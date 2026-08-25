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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityBufferBuilderAccess;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityQuadCacheHolder;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexConsumer.class)
public interface VertexConsumerBakedQuadMixin {
    @Inject(method = "putBakedQuad", at = @At("HEAD"))
    default void irisExt$attachQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, CallbackInfo ci) {
        irisExt$attach(quad);
    }

    @Inject(method = "putBakedQuad", at = @At("RETURN"))
    default void irisExt$detachQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, CallbackInfo ci) {
        irisExt$detach();
    }

    @Inject(method = "putBlockBakedQuad", at = @At("HEAD"))
    default void irisExt$attachBlockQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance, CallbackInfo ci) {
        irisExt$attach(quad);
    }

    @Inject(method = "putBlockBakedQuad", at = @At("RETURN"))
    default void irisExt$detachBlockQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance, CallbackInfo ci) {
        irisExt$detach();
    }

    default void irisExt$attach(BakedQuad quad) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup() || !VelocityRenderContext.hasKey) {
            return;
        }
        if ((Object) this instanceof VelocityBufferBuilderAccess access
                && (Object) quad instanceof VelocityQuadCacheHolder holder) {
            access.irisExt$attachQuadStates(holder.irisExt$getOrCreateStates(VelocityRenderContext.currentKey));
        }
    }

    default void irisExt$detach() {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if ((Object) this instanceof VelocityBufferBuilderAccess access) {
            access.irisExt$detachStates();
        }
    }
}
