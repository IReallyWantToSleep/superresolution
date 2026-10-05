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

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_3.IrisExtVertexFormats;
import io.homo.superresolution.iris_velocity_ext.v26_3.VelocityBufferBuilderAccess;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Matrix4x3fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderVelocityMixin implements VelocityBufferBuilderAccess {
    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    private long vertexPointer;

    @Unique
    private final float[] irisExt$delta = new float[12];
    @Unique
    private boolean irisExt$hasDelta;

    @Override
    public void irisExt$attachTransformDelta(MemorySegment delta) {
        MemorySegment.copy(
                delta,
                ValueLayout.JAVA_FLOAT,
                0,
                irisExt$delta,
                0,
                12
        );
        irisExt$hasDelta = true;
    }

    @Override
    public void irisExt$detachStates() {
        irisExt$hasDelta = false;
    }

    @Override
    public boolean irisExt$isVelocityFormat() {
        return SuperResolutionConfig.isIrisExtensionEnabledAtStartup() && format == IrisExtVertexFormats.ENTITY_VELOCITY;
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"), order = 1100)
    private void irisExt$writeVelocity(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if (!irisExt$isVelocityFormat() || this.vertexPointer == -1L) {
            return;
        }
        long ptr = this.vertexPointer + IrisExtVertexFormats.velocityOffset();
        float vx = 0.0f;
        float vy = 0.0f;
        float vz = 0.0f;
        if (irisExt$hasDelta) {
            vx = irisExt$delta[0] * x + irisExt$delta[3 + 0] * y + irisExt$delta[3 + 3 + 0] * z + irisExt$delta[3 + 3 + 3 + 0];
            vy = irisExt$delta[1] * x + irisExt$delta[3 + 1] * y + irisExt$delta[3 + 3 + 1] * z + irisExt$delta[3 + 3 + 3 + 1];
            vz = irisExt$delta[2] * x + irisExt$delta[3 + 2] * y + irisExt$delta[3 + 3 + 2] * z + irisExt$delta[3 + 3 + 3 + 2];
        }
        MemoryUtil.memPutFloat(ptr, vx);
        MemoryUtil.memPutFloat(ptr + 4, vy);
        MemoryUtil.memPutFloat(ptr + 8, vz);
    }
}
