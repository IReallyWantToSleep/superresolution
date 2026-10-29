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

public final class NgxVKDLSSEvalParams {
    public final NgxVKFeatureEvalParams feature = new NgxVKFeatureEvalParams();
    public final NgxDimensions renderSubrectDimensions = new NgxDimensions();
    public final NgxCoordinates colorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates depthSubrectBase = new NgxCoordinates();
    public final NgxCoordinates motionVectorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates translucencySubrectBase = new NgxCoordinates();
    public final NgxCoordinates biasCurrentColorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates outputSubrectBase = new NgxCoordinates();
    public final NgxVKGBuffer gBuffer = new NgxVKGBuffer();
    public NgxResourceVK depth;
    public NgxResourceVK motionVectors;
    public float jitterOffsetX;
    public float jitterOffsetY;
    public int reset;
    public float motionVectorScaleX;
    public float motionVectorScaleY;
    public NgxResourceVK transparencyMask;
    public NgxResourceVK exposureTexture;
    public NgxResourceVK biasCurrentColorMask;
    public float preExposure;
    public float exposureScale;
    public int indicatorInvertXAxis;
    public int indicatorInvertYAxis;
    public int toneMapperType;
    public NgxResourceVK motionVectors3D;
    public NgxResourceVK particleMask;
    public NgxResourceVK animatedTextureMask;
    public NgxResourceVK depthHighRes;
    public NgxResourceVK positionViewSpace;
    public float frameTimeDeltaInMsec;
    public NgxResourceVK rayTracingHitDistance;
    public NgxResourceVK motionVectorsReflections;
}
