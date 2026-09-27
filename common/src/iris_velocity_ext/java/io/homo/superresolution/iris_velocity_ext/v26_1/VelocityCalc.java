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

package io.homo.superresolution.iris_velocity_ext.v26_1;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Matrix4x3f;


public final class VelocityCalc {
    public static int frameId;

    private static final Matrix4f scratchInverse = new Matrix4f();
    private static final Matrix4f scratchPrev = new Matrix4f();
    private static final Matrix4f scratchPose = new Matrix4f();

    private VelocityCalc() {
    }

    public static void computeTransformDelta(VelocityTransformState state, Matrix4fc currentPose) {
        if (state.lastFrameId == frameId) {
            return;
        }
        Matrix4fc modelView = CapturedRenderingState.INSTANCE.getGbufferModelView();
        if (modelView == null) {
            state.deltaRaw[0]  = 0.0F;
            state.deltaRaw[1]  = 0.0F;
            state.deltaRaw[2]  = 0.0F;
            state.deltaRaw[3]  = 0.0F;
            state.deltaRaw[4]  = 0.0F;
            state.deltaRaw[5]  = 0.0F;
            state.deltaRaw[6]  = 0.0F;
            state.deltaRaw[7]  = 0.0F;
            state.deltaRaw[8]  = 0.0F;
            state.deltaRaw[9]  = 0.0F;
            state.deltaRaw[10] = 0.0F;
            state.deltaRaw[11] = 0.0F;
            return;
        }
        scratchInverse.set(currentPose).invert();
        if (!scratchInverse.isFinite()) {
            state.deltaRaw[0]  = 0.0F;
            state.deltaRaw[1]  = 0.0F;
            state.deltaRaw[2]  = 0.0F;
            state.deltaRaw[3]  = 0.0F;
            state.deltaRaw[4]  = 0.0F;
            state.deltaRaw[5]  = 0.0F;
            state.deltaRaw[6]  = 0.0F;
            state.deltaRaw[7]  = 0.0F;
            state.deltaRaw[8]  = 0.0F;
            state.deltaRaw[9]  = 0.0F;
            state.deltaRaw[10] = 0.0F;
            state.deltaRaw[11] = 0.0F;
            state.valid = false;
            return;
        }
        if (!state.valid || frameId - state.lastFrameId > 3) {
            state.deltaRaw[0]  = 0.0F;
            state.deltaRaw[1]  = 0.0F;
            state.deltaRaw[2]  = 0.0F;
            state.deltaRaw[3]  = 0.0F;
            state.deltaRaw[4]  = 0.0F;
            state.deltaRaw[5]  = 0.0F;
            state.deltaRaw[6]  = 0.0F;
            state.deltaRaw[7]  = 0.0F;
            state.deltaRaw[8]  = 0.0F;
            state.deltaRaw[9]  = 0.0F;
            state.deltaRaw[10] = 0.0F;
            state.deltaRaw[11] = 0.0F;
        } else {
            scratchPrev.set(state.prevModelToView).mul(scratchInverse);
            state.deltaRaw[0]  = modelView.m00() - scratchPrev.m00();
            state.deltaRaw[1]  = modelView.m01() - scratchPrev.m01();
            state.deltaRaw[2]  = modelView.m02() - scratchPrev.m02();
            state.deltaRaw[3]  = modelView.m10() - scratchPrev.m10();
            state.deltaRaw[4]  = modelView.m11() - scratchPrev.m11();
            state.deltaRaw[5]  = modelView.m12() - scratchPrev.m12();
            state.deltaRaw[6]  = modelView.m20() - scratchPrev.m20();
            state.deltaRaw[7]  = modelView.m21() - scratchPrev.m21();
            state.deltaRaw[8]  = modelView.m22() - scratchPrev.m22();
            state.deltaRaw[9]  = modelView.m30() - scratchPrev.m30();
            state.deltaRaw[10] = modelView.m31() - scratchPrev.m31();
            state.deltaRaw[11] = modelView.m32() - scratchPrev.m32();
        }
        state.prevModelToView.set(modelView).mul(currentPose);
        state.valid = true;
        state.lastFrameId = frameId;
    }

    public static void computeOffsetDelta(VelocityTransformState state, float x, float y, float z) {
        computeTransformDelta(state, scratchPose.translation(x, y, z));
    }
}
