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

package io.homo.superresolution.core.ngx;

import java.nio.FloatBuffer;

public final class NgxDLSSFGOptEvalParams {
    public final float[] jitterOffset = new float[2];
    public final float[] motionVectorScale = new float[2];
    public final float[] cameraPinholeOffset = new float[2];
    public final float[] cameraPosition = new float[3];
    public final float[] cameraUp = new float[3];
    public final float[] cameraRight = new float[3];
    public final float[] cameraForward = new float[3];
    public final NgxCoordinates motionVectorsSubrectBase = new NgxCoordinates();
    public final NgxDimensions motionVectorsSubrectSize = new NgxDimensions();
    public final NgxCoordinates depthSubrectBase = new NgxCoordinates();
    public final NgxDimensions depthSubrectSize = new NgxDimensions();
    public final NgxCoordinates hudlessSubrectBase = new NgxCoordinates();
    public final NgxDimensions hudlessSubrectSize = new NgxDimensions();
    public final NgxCoordinates uiSubrectBase = new NgxCoordinates();
    public final NgxDimensions uiSubrectSize = new NgxDimensions();
    public final NgxCoordinates uiAlphaSubrectBase = new NgxCoordinates();
    public final NgxDimensions uiAlphaSubrectSize = new NgxDimensions();
    public final NgxCoordinates bidirectionalDistortionFieldSubrectBase = new NgxCoordinates();
    public final NgxDimensions bidirectionalDistortionFieldSubrectSize = new NgxDimensions();
    public final NgxPrecisionInfo bidirectionalDistortionFieldPrecisionInfo = new NgxPrecisionInfo();
    public final NgxCoordinates backbufferSubrectBase = new NgxCoordinates();
    public final NgxDimensions backbufferSubrectSize = new NgxDimensions();
    public final NgxCoordinates outputInterpolatedSubrectBase = new NgxCoordinates();
    public final NgxDimensions outputInterpolatedSubrectSize = new NgxDimensions();
    public final NgxCoordinates outputRealSubrectBase = new NgxCoordinates();
    public final NgxDimensions outputRealSubrectSize = new NgxDimensions();
    public int multiFrameCount = 1;
    public int multiFrameIndex = 1;
    public FloatBuffer cameraViewToClip;
    public FloatBuffer clipToCameraView;
    public FloatBuffer clipToLensClip;
    public FloatBuffer clipToPrevClip;
    public FloatBuffer prevClipToClip;
    public float cameraNear;
    public float cameraFar;
    public float cameraFov;
    public float cameraAspectRatio;
    public boolean colorBuffersHdr;
    public boolean depthInverted;
    public boolean cameraMotionIncluded;
    public boolean reset;
    public boolean automodeOverrideReset;
    public boolean notRenderingGameFrames;
    public boolean orthoProjection;
    public float motionVectorsInvalidValue;
    public boolean motionVectorsDilated;
    public boolean menuDetectionEnabled;
    public float minRelativeLinearDepthObjectSeparation = 40.0f;
}
