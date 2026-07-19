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

public class FGConstants {
	public float[] cameraViewToClip = identityMatrix();
	public float[] clipToCameraView = identityMatrix();
	public float[] clipToLensClip = identityMatrix();
	public float[] clipToPrevClip = identityMatrix();
	public float[] prevClipToClip = identityMatrix();
	public float jitterOffsetX;
	public float jitterOffsetY;
	public float motionVectorScaleX;
	public float motionVectorScaleY;
	public float cameraPinholeOffsetX;
	public float cameraPinholeOffsetY;
	public float cameraPosX;
	public float cameraPosY;
	public float cameraPosZ;
	public float cameraUpX;
	public float cameraUpY;
	public float cameraUpZ;
	public float cameraRightX;
	public float cameraRightY;
	public float cameraRightZ;
	public float cameraFwdX;
	public float cameraFwdY;
	public float cameraFwdZ;
	public float cameraNear;
	public float cameraFar;
	public float cameraFov;
	public float cameraAspectRatio;
	public float motionVectorsInvalidValue;
	public byte depthInverted = 2;
	public byte cameraMotionIncluded = 2;
	public byte motionVectors3D = 2;
	public byte reset = 2;
	public byte orthographicProjection;
	public byte motionVectorsDilated;
	public byte motionVectorsJittered;
	public float minRelativeLinearDepthObjectSeparation = 40.0F;
	public FGConstants() {
		super();
	}

	static float[] identityMatrix() {
		return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
	}
}
