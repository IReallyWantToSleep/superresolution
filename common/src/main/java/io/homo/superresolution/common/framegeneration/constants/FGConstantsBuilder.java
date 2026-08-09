/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.framegeneration.constants;

import org.joml.*;

import java.lang.Math;

public final class FGConstantsBuilder {
    private static final float MATRIX_EPSILON = 1.0E-3F;
    private static final float BASIS_EPSILON = 1.0E-3F;
    private static final float MIN_DETERMINANT = 1.0E-8F;
    private static final float[] IDENTITY = new Matrix4f().get(new float[16]);

    private FGConstantsBuilder() {
    }

    public static FGConstants build(
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
        validateFrame(current);
        boolean resetHistory = reset || previous == null;
        Matrix4f projectionInverse = inverseChecked(current.projection, "cameraViewToClip");
        Matrix4f clipToPreviousClip = new Matrix4f();
        Matrix4f previousClipToClip = new Matrix4f();

        if (!resetHistory) {
            validateFrame(previous);
            Matrix4f currentRotationInverse = inverseChecked(current.viewRotation, "current view rotation");
            Vector3d relativeCameraPosition = new Vector3d(current.cameraPosition).sub(previous.cameraPosition);
            requireFloatRange(relativeCameraPosition.x, "relative camera X");
            requireFloatRange(relativeCameraPosition.y, "relative camera Y");
            requireFloatRange(relativeCameraPosition.z, "relative camera Z");

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
            requireFinite(clipToPreviousClip, "clipToPrevClip");
            previousClipToClip.set(inverseChecked(clipToPreviousClip, "clipToPrevClip"));
        }

        return new FGConstants(
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
        if (renderWidth <= 0 || renderHeight <= 0) {
            throw new IllegalArgumentException("Render size must be positive");
        }
        requireFinite(projection, "projection");
        float renderAspect = (float) renderWidth / (float) renderHeight;
        float matrixAspect = Math.abs(projection.m11() / projection.m00());
        if (!Float.isFinite(matrixAspect) || matrixAspect <= 0.0F) {
            return renderAspect;
        }
        float relativeDifference = Math.abs(matrixAspect - renderAspect) / renderAspect;
        return relativeDifference <= 0.1F ? matrixAspect : renderAspect;
    }

    static Matrix4f inverseChecked(Matrix4fc matrix, String name) {
        requireFinite(matrix, name);
        float determinant = matrix.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < MIN_DETERMINANT) {
            throw new IllegalArgumentException(name + " is not invertible");
        }
        Matrix4f inverse = matrix.invert(new Matrix4f());
        requireFinite(inverse, name + " inverse");
        Matrix4f product = new Matrix4f(matrix).mul(inverse);
        if (!product.equals(new Matrix4f(), MATRIX_EPSILON)) {
            throw new IllegalArgumentException(name + " inverse validation failed");
        }
        return inverse;
    }

    private static float[] toStreamlineMatrix(Matrix4fc matrix) {
        requireFinite(matrix, "matrix");
        return matrix.get(new float[16]);
    }

    private static void validateFrame(CameraFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("Camera frame must not be null");
        }
        requireFinite(frame.projection, "projection");
        requireFinite(frame.viewRotation, "view rotation");
        requireFinite(frame.cameraPosition, "camera position");
        requireFloatRange(frame.cameraPosition.x, "camera X");
        requireFloatRange(frame.cameraPosition.y, "camera Y");
        requireFloatRange(frame.cameraPosition.z, "camera Z");
        requireOrthonormalBasis(frame.cameraRight, frame.cameraUp, frame.cameraForward);
        if (!Float.isFinite(frame.cameraNear) || frame.cameraNear <= 0.0F) {
            throw new IllegalArgumentException("Camera near plane must be finite and positive");
        }
        if (!Float.isFinite(frame.cameraFar) || frame.cameraFar <= frame.cameraNear) {
            throw new IllegalArgumentException("Camera far plane must be finite and greater than the near plane");
        }
        if (!Float.isFinite(frame.cameraFovRadians)
                || frame.cameraFovRadians <= 0.0F
                || frame.cameraFovRadians >= Math.PI) {
            throw new IllegalArgumentException("Camera FOV must be in radians and inside (0, pi)");
        }
        if (!Float.isFinite(frame.cameraAspectRatio) || frame.cameraAspectRatio <= 0.0F) {
            throw new IllegalArgumentException("Camera aspect ratio must be finite and positive");
        }
        inverseChecked(frame.projection, "projection");
        inverseChecked(frame.viewRotation, "view rotation");
    }

    private static void requireOrthonormalBasis(Vector3fc right, Vector3fc up, Vector3fc forward) {
        requireFinite(right, "camera right");
        requireFinite(up, "camera up");
        requireFinite(forward, "camera forward");
        if (Math.abs(right.lengthSquared() - 1.0F) > BASIS_EPSILON
                || Math.abs(up.lengthSquared() - 1.0F) > BASIS_EPSILON
                || Math.abs(forward.lengthSquared() - 1.0F) > BASIS_EPSILON
                || Math.abs(right.dot(up)) > BASIS_EPSILON
                || Math.abs(right.dot(forward)) > BASIS_EPSILON
                || Math.abs(up.dot(forward)) > BASIS_EPSILON) {
            throw new IllegalArgumentException("Camera basis must be orthonormal");
        }
    }

    private static void requireFinite(Matrix4fc matrix, String name) {
        if (matrix == null || !matrix.isFinite()) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFinite(Vector3dc vector, String name) {
        if (vector == null
                || !Double.isFinite(vector.x())
                || !Double.isFinite(vector.y())
                || !Double.isFinite(vector.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFinite(Vector3fc vector, String name) {
        if (vector == null || !vector.isFinite()) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFloatRange(double value, String name) {
        if (!Double.isFinite(value) || Math.abs(value) > Float.MAX_VALUE) {
            throw new IllegalArgumentException(name + " is outside Streamline float range");
        }
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
