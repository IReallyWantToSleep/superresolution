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

import java.util.Arrays;

public final class FGConstants {
    private final float[] cameraViewToClip;
    private final float[] clipToCameraView;
    private final float[] clipToLensClip;
    private final float[] clipToPrevClip;
    private final float[] prevClipToClip;
    private final float jitterOffsetX;
    private final float jitterOffsetY;
    private final float motionVectorScaleX;
    private final float motionVectorScaleY;
    private final float cameraPinholeOffsetX;
    private final float cameraPinholeOffsetY;
    private final float cameraPosX;
    private final float cameraPosY;
    private final float cameraPosZ;
    private final float cameraUpX;
    private final float cameraUpY;
    private final float cameraUpZ;
    private final float cameraRightX;
    private final float cameraRightY;
    private final float cameraRightZ;
    private final float cameraFwdX;
    private final float cameraFwdY;
    private final float cameraFwdZ;
    private final float cameraNear;
    private final float cameraFar;
    private final float cameraFov;
    private final float cameraAspectRatio;
    private final float motionVectorsInvalidValue;
    private final byte depthInverted;
    private final byte cameraMotionIncluded;
    private final byte motionVectors3D;
    private final byte reset;
    private final byte orthographicProjection;
    private final byte motionVectorsDilated;
    private final byte motionVectorsJittered;
    private final float minRelativeLinearDepthObjectSeparation;

    public FGConstants(
            float[] cameraViewToClip,
            float[] clipToCameraView,
            float[] clipToLensClip,
            float[] clipToPrevClip,
            float[] prevClipToClip,
            float jitterOffsetX,
            float jitterOffsetY,
            float motionVectorScaleX,
            float motionVectorScaleY,
            float cameraPinholeOffsetX,
            float cameraPinholeOffsetY,
            float cameraPosX,
            float cameraPosY,
            float cameraPosZ,
            float cameraUpX,
            float cameraUpY,
            float cameraUpZ,
            float cameraRightX,
            float cameraRightY,
            float cameraRightZ,
            float cameraFwdX,
            float cameraFwdY,
            float cameraFwdZ,
            float cameraNear,
            float cameraFar,
            float cameraFov,
            float cameraAspectRatio,
            float motionVectorsInvalidValue,
            byte depthInverted,
            byte cameraMotionIncluded,
            byte motionVectors3D,
            byte reset,
            byte orthographicProjection,
            byte motionVectorsDilated,
            byte motionVectorsJittered,
            float minRelativeLinearDepthObjectSeparation
    ) {
        this.cameraViewToClip = copyMatrix(cameraViewToClip, "cameraViewToClip");
        this.clipToCameraView = copyMatrix(clipToCameraView, "clipToCameraView");
        this.clipToLensClip = copyMatrix(clipToLensClip, "clipToLensClip");
        this.clipToPrevClip = copyMatrix(clipToPrevClip, "clipToPrevClip");
        this.prevClipToClip = copyMatrix(prevClipToClip, "prevClipToClip");
        this.jitterOffsetX = jitterOffsetX;
        this.jitterOffsetY = jitterOffsetY;
        this.motionVectorScaleX = motionVectorScaleX;
        this.motionVectorScaleY = motionVectorScaleY;
        this.cameraPinholeOffsetX = cameraPinholeOffsetX;
        this.cameraPinholeOffsetY = cameraPinholeOffsetY;
        this.cameraPosX = cameraPosX;
        this.cameraPosY = cameraPosY;
        this.cameraPosZ = cameraPosZ;
        this.cameraUpX = cameraUpX;
        this.cameraUpY = cameraUpY;
        this.cameraUpZ = cameraUpZ;
        this.cameraRightX = cameraRightX;
        this.cameraRightY = cameraRightY;
        this.cameraRightZ = cameraRightZ;
        this.cameraFwdX = cameraFwdX;
        this.cameraFwdY = cameraFwdY;
        this.cameraFwdZ = cameraFwdZ;
        this.cameraNear = cameraNear;
        this.cameraFar = cameraFar;
        this.cameraFov = cameraFov;
        this.cameraAspectRatio = cameraAspectRatio;
        this.motionVectorsInvalidValue = motionVectorsInvalidValue;
        this.depthInverted = depthInverted;
        this.cameraMotionIncluded = cameraMotionIncluded;
        this.motionVectors3D = motionVectors3D;
        this.reset = reset;
        this.orthographicProjection = orthographicProjection;
        this.motionVectorsDilated = motionVectorsDilated;
        this.motionVectorsJittered = motionVectorsJittered;
        this.minRelativeLinearDepthObjectSeparation = minRelativeLinearDepthObjectSeparation;
    }

    private static float[] copyMatrix(float[] matrix, String name) {
        if (matrix == null || matrix.length != 16) {
            throw new IllegalArgumentException(name + " must contain 16 floats");
        }
        return Arrays.copyOf(matrix, matrix.length);
    }

    public float[] cameraViewToClip() {
        return cameraViewToClip.clone();
    }

    public float[] clipToCameraView() {
        return clipToCameraView.clone();
    }

    public float[] clipToLensClip() {
        return clipToLensClip.clone();
    }

    public float[] clipToPrevClip() {
        return clipToPrevClip.clone();
    }

    public float[] prevClipToClip() {
        return prevClipToClip.clone();
    }

    public float jitterOffsetX() {
        return jitterOffsetX;
    }

    public float jitterOffsetY() {
        return jitterOffsetY;
    }

    public float motionVectorScaleX() {
        return motionVectorScaleX;
    }

    public float motionVectorScaleY() {
        return motionVectorScaleY;
    }

    public float cameraPinholeOffsetX() {
        return cameraPinholeOffsetX;
    }

    public float cameraPinholeOffsetY() {
        return cameraPinholeOffsetY;
    }

    public float cameraPosX() {
        return cameraPosX;
    }

    public float cameraPosY() {
        return cameraPosY;
    }

    public float cameraPosZ() {
        return cameraPosZ;
    }

    public float cameraUpX() {
        return cameraUpX;
    }

    public float cameraUpY() {
        return cameraUpY;
    }

    public float cameraUpZ() {
        return cameraUpZ;
    }

    public float cameraRightX() {
        return cameraRightX;
    }

    public float cameraRightY() {
        return cameraRightY;
    }

    public float cameraRightZ() {
        return cameraRightZ;
    }

    public float cameraFwdX() {
        return cameraFwdX;
    }

    public float cameraFwdY() {
        return cameraFwdY;
    }

    public float cameraFwdZ() {
        return cameraFwdZ;
    }

    public float cameraNear() {
        return cameraNear;
    }

    public float cameraFar() {
        return cameraFar;
    }

    public float cameraFov() {
        return cameraFov;
    }

    public float cameraAspectRatio() {
        return cameraAspectRatio;
    }

    public float motionVectorsInvalidValue() {
        return motionVectorsInvalidValue;
    }

    public byte depthInverted() {
        return depthInverted;
    }

    public byte cameraMotionIncluded() {
        return cameraMotionIncluded;
    }

    public byte motionVectors3D() {
        return motionVectors3D;
    }

    public byte reset() {
        return reset;
    }

    public byte orthographicProjection() {
        return orthographicProjection;
    }

    public byte motionVectorsDilated() {
        return motionVectorsDilated;
    }

    public byte motionVectorsJittered() {
        return motionVectorsJittered;
    }

    public float minRelativeLinearDepthObjectSeparation() {
        return minRelativeLinearDepthObjectSeparation;
    }
}
