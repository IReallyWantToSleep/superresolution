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
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityBufferBuilderAccess;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCache;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCalc;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityRenderContext;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityTransformState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelPart.class)
public abstract class ModelPartTransformVelocityMixin {
    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;

    @Inject(method = "compile", at = @At("HEAD"))
    private void irisExt$attach(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color, CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if (this.cubes.isEmpty()) {
            return;
        }
        VelocityCache cache = VelocityRenderContext.current;
        if (cache == null || !(builder instanceof VelocityBufferBuilderAccess access)) {
            return;
        }
        if (IrisApi.getInstance().isRenderingShadowPass()) {
            return;
        }
        VelocityTransformState state = cache.getOrCreatePartState((ModelPart) (Object) this);
        VelocityCalc.computeTransformDelta(state, pose.pose());
        access.irisExt$attachTransformDelta(state.delta);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void irisExt$detach(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color, CallbackInfo ci) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if (builder instanceof VelocityBufferBuilderAccess access) {
            access.irisExt$detachStates();
        }
    }
}
