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

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.iris_velocity_ext.v26_1.IrisExtVertexFormats;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityBufferBuilderAccess;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityCalc;
import io.homo.superresolution.iris_velocity_ext.v26_1.VelocityVertexState;
import net.irisshaders.iris.vertices.MemoryAccess;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderVelocityMixin implements VelocityBufferBuilderAccess {
    @Shadow
    private int elementsToFill;
    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    protected abstract long beginElement(VertexFormatElement element);

    @Unique
    private final Matrix4f irisExt$delta = new Matrix4f();
    @Unique
    private boolean irisExt$hasDelta;
    @Unique
    private VelocityVertexState[] irisExt$quadStates;
    @Unique
    private int irisExt$vertexIndex;

    @Override
    public void irisExt$attachTransformDelta(Matrix4fc delta) {
        irisExt$delta.set(delta);
        irisExt$hasDelta = true;
    }

    @Override
    public void irisExt$attachQuadStates(VelocityVertexState[] states) {
        irisExt$quadStates = states;
        irisExt$vertexIndex = 0;
    }

    @Override
    public void irisExt$detachStates() {
        irisExt$hasDelta = false;
        irisExt$quadStates = null;
        irisExt$vertexIndex = 0;
    }

    @Override
    public boolean irisExt$isVelocityFormat() {
        return SuperResolutionConfig.isIrisExtensionEnabledAtStartup() && format == IrisExtVertexFormats.ENTITY_VELOCITY;
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"),order = 1100)
    private void irisExt$writeVelocity(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!SuperResolutionConfig.isIrisExtensionEnabledAtStartup()) {
            return;
        }
        if ((this.elementsToFill & IrisExtVertexFormats.VELOCITY_ELEMENT.mask()) == 0) {
            return;
        }
        long ptr = this.beginElement(IrisExtVertexFormats.VELOCITY_ELEMENT);
        float vx = 0.0f;
        float vy = 0.0f;
        float vz = 0.0f;
        VelocityVertexState state = null;
        if (irisExt$quadStates != null && irisExt$vertexIndex < 4) {
            state = irisExt$quadStates[irisExt$vertexIndex];
        }
        if (state != null) {
            VelocityCalc.compute(state, x, y, z);
            vx = state.velX;
            vy = state.velY;
            vz = state.velZ;
        } else if (irisExt$hasDelta) {
            vx = irisExt$delta.m00() * x + irisExt$delta.m10() * y + irisExt$delta.m20() * z + irisExt$delta.m30();
            vy = irisExt$delta.m01() * x + irisExt$delta.m11() * y + irisExt$delta.m21() * z + irisExt$delta.m31();
            vz = irisExt$delta.m02() * x + irisExt$delta.m12() * y + irisExt$delta.m22() * z + irisExt$delta.m32();
        }
        MemoryAccess.setFloat(ptr, vx);
        MemoryAccess.setFloat(ptr + 4, vy);
        MemoryAccess.setFloat(ptr + 8, vz);
        irisExt$vertexIndex++;
        if (irisExt$vertexIndex >= 4) {
            irisExt$vertexIndex = 0;
        }
    }
}
