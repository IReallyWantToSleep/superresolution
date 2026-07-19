/*
 * Anemone Mod
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


import java.util.Arrays;

public final class FGCameraSnapshot {
	private static final float[] IDENTITY = new float[]{
		1.0F, 0.0F, 0.0F, 0.0F,
		0.0F, 1.0F, 0.0F, 0.0F,
		0.0F, 0.0F, 1.0F, 0.0F,
		0.0F, 0.0F, 0.0F, 1.0F
	};

	private final int frameIndex;
	private final float[] cameraViewToClip;
	private final float[] clipToCameraView;
	private final float[] clipToLensClip;
	private final float[] clipToPrevClip;
	private final float[] prevClipToClip;
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
	private final boolean cameraMotionIncluded;
	private final boolean motionVectorsJittered;
	private final boolean reset;

	FGCameraSnapshot(
		int frameIndex,
		float[] cameraViewToClip,
		float[] clipToCameraView,
		float[] clipToLensClip,
		float[] clipToPrevClip,
		float[] prevClipToClip,
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
		boolean cameraMotionIncluded,
		boolean motionVectorsJittered,
		boolean reset
	) {
		this.frameIndex = frameIndex;
		this.cameraViewToClip = copyMatrix(cameraViewToClip);
		this.clipToCameraView = copyMatrix(clipToCameraView);
		this.clipToLensClip = copyMatrix(clipToLensClip);
		this.clipToPrevClip = copyMatrix(clipToPrevClip);
		this.prevClipToClip = copyMatrix(prevClipToClip);
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
		this.cameraMotionIncluded = cameraMotionIncluded;
		this.motionVectorsJittered = motionVectorsJittered;
		this.reset = reset;
	}

	private static float[] copyMatrix(float[] matrix) {
		if (matrix == null || matrix.length != 16) {
			throw new IllegalArgumentException("Streamline matrices must contain 16 floats");
		}
		return Arrays.copyOf(matrix, matrix.length);
	}

	public int frameIndex() {
		return frameIndex;
	}

	public float[] cameraViewToClip() {
		return cameraViewToClip.clone();
	}

	public float[] clipToCameraView() {
		return clipToCameraView.clone();
	}

	public float[] clipToPrevClip() {
		return clipToPrevClip.clone();
	}

	public float[] prevClipToClip() {
		return prevClipToClip.clone();
	}

	public boolean reset() {
		return reset;
	}

	FGConstants toConstants() {
		FGConstants constants = new FGConstants();
		populate(constants, false);
		return constants;
	}

	void populate(FGConstants constants, boolean forceReset) {
		boolean resetConstants = reset || forceReset;
		constants.cameraViewToClip = cameraViewToClip.clone();
		constants.clipToCameraView = clipToCameraView.clone();
		constants.clipToLensClip = clipToLensClip.clone();
		constants.clipToPrevClip = resetConstants ? IDENTITY.clone() : clipToPrevClip.clone();
		constants.prevClipToClip = resetConstants ? IDENTITY.clone() : prevClipToClip.clone();
		constants.cameraPinholeOffsetX = 0.0F;
		constants.cameraPinholeOffsetY = 0.0F;
		constants.cameraPosX = cameraPosX;
		constants.cameraPosY = cameraPosY;
		constants.cameraPosZ = cameraPosZ;
		constants.cameraUpX = cameraUpX;
		constants.cameraUpY = cameraUpY;
		constants.cameraUpZ = cameraUpZ;
		constants.cameraRightX = cameraRightX;
		constants.cameraRightY = cameraRightY;
		constants.cameraRightZ = cameraRightZ;
		constants.cameraFwdX = cameraFwdX;
		constants.cameraFwdY = cameraFwdY;
		constants.cameraFwdZ = cameraFwdZ;
		constants.cameraNear = cameraNear;
		constants.cameraFar = cameraFar;
		constants.cameraFov = cameraFov;
		constants.cameraAspectRatio = cameraAspectRatio;
		constants.depthInverted = 0;
		constants.cameraMotionIncluded = cameraMotionIncluded ? (byte) 1 : (byte) 0;
		if (!cameraMotionIncluded) {
			constants.motionVectorsInvalidValue = 0.0F;
		}
		constants.motionVectors3D = 0;
		constants.reset = resetConstants ? (byte) 1 : (byte) 0;
		constants.orthographicProjection = 0;
		constants.motionVectorsDilated = 0;
		constants.motionVectorsJittered = motionVectorsJittered ? (byte) 1 : (byte) 0;
		constants.minRelativeLinearDepthObjectSeparation = 40.0F;
	}
}
