/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.framegeneration.constants;

import org.joml.*;

import java.lang.Math;

public final class FGConstantsBuilder {
    private static final float MATRIX_EPSILON = 1.0E-3F;
    private static final float MIN_DETERMINANT = 1.0E-8F;
    private static final float[] IDENTITY = new Matrix4f().get(new float[16]);

    private FGConstantsBuilder() {
    }

    public static FrameGenerationConstants build(
            CameraFrame current,
            CameraFrame previous,
            boolean reset,
            boolean cameraMotionIncluded,
            boolean motionVectorsJittered,
            float jitterOffsetX,
            float jitterOffsetY,
            float motionVectorScaleX,
            float motionVectorScaleY
    ) {
        boolean resetHistory = reset || previous == null;
        Matrix4f projectionInverse = inverse(current.projection, "cameraViewToClip");
        Matrix4f clipToPreviousClip = new Matrix4f();
        Matrix4f previousClipToClip = new Matrix4f();

        if (!resetHistory) {
            Matrix4f currentRotationInverse = inverse(current.viewRotation, "current view rotation");
            Vector3d relativeCameraPosition = new Vector3d(current.cameraPosition).sub(previous.cameraPosition);

            Matrix4f currentViewToPreviousView = new Matrix4f(previous.viewRotation)
                    .translate(
                            (float) relativeCameraPosition.x,
                            (float) relativeCameraPosition.y,
                            (float) relativeCameraPosition.z
                    )
                    .mul(currentRotationInverse);
            clipToPreviousClip
                    .set(previous.projection)
                    .mul(currentViewToPreviousView)
                    .mul(projectionInverse);
            previousClipToClip.set(inverse(clipToPreviousClip, "clipToPrevClip"));
        }

        return new FrameGenerationConstants(
                toStreamlineMatrix(current.projection),
                toStreamlineMatrix(projectionInverse),
                IDENTITY,
                resetHistory ? IDENTITY : toStreamlineMatrix(clipToPreviousClip),
                resetHistory ? IDENTITY : toStreamlineMatrix(previousClipToClip),
                jitterOffsetX,
                jitterOffsetY,
                motionVectorScaleX,
                motionVectorScaleY,
                0.0F,
                0.0F,
                (float) current.cameraPosition.x,
                (float) current.cameraPosition.y,
                (float) current.cameraPosition.z,
                current.cameraUp.x,
                current.cameraUp.y,
                current.cameraUp.z,
                current.cameraRight.x,
                current.cameraRight.y,
                current.cameraRight.z,
                current.cameraForward.x,
                current.cameraForward.y,
                current.cameraForward.z,
                current.cameraNear,
                current.cameraFar,
                current.cameraFovRadians,
                current.cameraAspectRatio,
                0.0F,
                (byte) 0,
                cameraMotionIncluded ? (byte) 1 : (byte) 0,
                (byte) 0,
                resetHistory ? (byte) 1 : (byte) 0,
                (byte) 0,
                (byte) 0,
                motionVectorsJittered ? (byte) 1 : (byte) 0,
                40.0F
        );
    }

    public static float resolveAspectRatio(Matrix4fc projection, int renderWidth, int renderHeight) {
        float renderAspect = (float) renderWidth / (float) renderHeight;
        float matrixAspect = Math.abs(projection.m11() / projection.m00());
        if (!Float.isFinite(matrixAspect) || matrixAspect <= 0.0F) {
            return renderAspect;
        }
        float relativeDifference = Math.abs(matrixAspect - renderAspect) / renderAspect;
        return relativeDifference <= 0.1F ? matrixAspect : renderAspect;
    }

    static Matrix4f inverse(Matrix4fc matrix, String name) {
        float determinant = matrix.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < MIN_DETERMINANT) {
            throw new IllegalArgumentException(name + " is not invertible");
        }
        Matrix4f inverse = matrix.invert(new Matrix4f());
        Matrix4f product = new Matrix4f(matrix).mul(inverse);
        if (!product.equals(new Matrix4f(), MATRIX_EPSILON)) {
            throw new IllegalArgumentException(name + " inverse validation failed");
        }
        return inverse;
    }

    private static float[] toStreamlineMatrix(Matrix4fc matrix) {
        return matrix.get(new float[16]);
    }


    public static final class CameraFrame {
        private final int frameIndex;
        private final Matrix4f projection;
        private final Matrix4f viewRotation;
        private final Vector3d cameraPosition;
        private final Vector3f cameraUp;
        private final Vector3f cameraRight;
        private final Vector3f cameraForward;
        private final float cameraNear;
        private final float cameraFar;
        private final float cameraFovRadians;
        private final float cameraAspectRatio;

        public CameraFrame(
                int frameIndex,
                Matrix4fc projection,
                Matrix4fc viewRotation,
                Vector3dc cameraPosition,
                Vector3fc cameraUp,
                Vector3fc cameraRight,
                Vector3fc cameraForward,
                float cameraNear,
                float cameraFar,
                float cameraFovRadians,
                float cameraAspectRatio
        ) {
            this.frameIndex = frameIndex;
            this.projection = new Matrix4f(projection);
            this.viewRotation = new Matrix4f(viewRotation);
            this.cameraPosition = new Vector3d(cameraPosition);
            this.cameraUp = new Vector3f(cameraUp);
            this.cameraRight = new Vector3f(cameraRight);
            this.cameraForward = new Vector3f(cameraForward);
            this.cameraNear = cameraNear;
            this.cameraFar = cameraFar;
            this.cameraFovRadians = cameraFovRadians;
            this.cameraAspectRatio = cameraAspectRatio;
        }

        public int frameIndex() {
            return frameIndex;
        }
    }
}
