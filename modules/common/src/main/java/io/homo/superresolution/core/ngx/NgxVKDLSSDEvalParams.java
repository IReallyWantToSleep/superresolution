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

public final class NgxVKDLSSDEvalParams {
    public final NgxVKFeatureEvalParams feature = new NgxVKFeatureEvalParams();
    public final NgxDimensions renderSubrectDimensions = new NgxDimensions();
    public final NgxVKGBuffer gBuffer = new NgxVKGBuffer();

    public final NgxCoordinates alphaSubrectBase = new NgxCoordinates();
    public final NgxCoordinates outputAlphaSubrectBase = new NgxCoordinates();
    public final NgxCoordinates diffuseAlbedoSubrectBase = new NgxCoordinates();
    public final NgxCoordinates specularAlbedoSubrectBase = new NgxCoordinates();
    public final NgxCoordinates normalsSubrectBase = new NgxCoordinates();
    public final NgxCoordinates roughnessSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates depthSubrectBase = new NgxCoordinates();
    public final NgxCoordinates motionVectorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates translucencySubrectBase = new NgxCoordinates();
    public final NgxCoordinates biasCurrentColorSubrectBase = new NgxCoordinates();
    public final NgxCoordinates outputSubrectBase = new NgxCoordinates();
    public final NgxCoordinates reflectedAlbedoSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeParticlesSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterParticlesSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeTransparencySubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterTransparencySubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeFogSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterFogSubrectBase = new NgxCoordinates();
    public final NgxCoordinates screenSpaceSubsurfaceScatteringGuideSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeScreenSpaceSubsurfaceScatteringSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterScreenSpaceSubsurfaceScatteringSubrectBase = new NgxCoordinates();
    public final NgxCoordinates screenSpaceRefractionGuideSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeScreenSpaceRefractionSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterScreenSpaceRefractionSubrectBase = new NgxCoordinates();
    public final NgxCoordinates depthOfFieldGuideSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorBeforeDepthOfFieldSubrectBase = new NgxCoordinates();
    public final NgxCoordinates colorAfterDepthOfFieldSubrectBase = new NgxCoordinates();
    public final NgxCoordinates diffuseHitDistanceSubrectBase = new NgxCoordinates();
    public final NgxCoordinates specularHitDistanceSubrectBase = new NgxCoordinates();
    public final NgxCoordinates diffuseRayDirectionSubrectBase = new NgxCoordinates();
    public final NgxCoordinates specularRayDirectionSubrectBase = new NgxCoordinates();
    public final NgxCoordinates diffuseRayDirectionHitDistanceSubrectBase = new NgxCoordinates();
    public final NgxCoordinates specularRayDirectionHitDistanceSubrectBase = new NgxCoordinates();
    public final NgxCoordinates transparencyLayerSubrectBase = new NgxCoordinates();
    public final NgxCoordinates transparencyLayerOpacitySubrectBase = new NgxCoordinates();
    public final NgxCoordinates transparencyLayerMotionVectorsSubrectBase = new NgxCoordinates();
    public final NgxCoordinates disocclusionMaskSubrectBase = new NgxCoordinates();
    public final NgxCoordinates responsivityMaskSubrectBase = new NgxCoordinates();
    public NgxResourceVK diffuseAlbedo;
    public NgxResourceVK specularAlbedo;
    public NgxResourceVK normals;
    public NgxResourceVK roughness;
    public NgxResourceVK alpha;
    public NgxResourceVK outputAlpha;
    public NgxResourceVK depth;
    public NgxResourceVK motionVectors;
    public NgxResourceVK transparencyMask;
    public NgxResourceVK exposureTexture;
    public NgxResourceVK biasCurrentColorMask;
    public float jitterOffsetX;
    public float jitterOffsetY;
    public int reset;
    public float motionVectorScaleX;
    public float motionVectorScaleY;
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
    public NgxResourceVK reflectedAlbedo;
    public NgxResourceVK colorBeforeParticles;
    public NgxResourceVK colorAfterParticles;
    public NgxResourceVK colorBeforeTransparency;
    public NgxResourceVK colorAfterTransparency;
    public NgxResourceVK colorBeforeFog;
    public NgxResourceVK colorAfterFog;
    public NgxResourceVK screenSpaceSubsurfaceScatteringGuide;
    public NgxResourceVK colorBeforeScreenSpaceSubsurfaceScattering;
    public NgxResourceVK colorAfterScreenSpaceSubsurfaceScattering;
    public NgxResourceVK screenSpaceRefractionGuide;
    public NgxResourceVK colorBeforeScreenSpaceRefraction;
    public NgxResourceVK colorAfterScreenSpaceRefraction;
    public NgxResourceVK depthOfFieldGuide;
    public NgxResourceVK colorBeforeDepthOfField;
    public NgxResourceVK colorAfterDepthOfField;
    public NgxResourceVK diffuseHitDistance;
    public NgxResourceVK specularHitDistance;
    public NgxResourceVK diffuseRayDirection;
    public NgxResourceVK specularRayDirection;
    public NgxResourceVK diffuseRayDirectionHitDistance;
    public NgxResourceVK specularRayDirectionHitDistance;
    public FloatBuffer worldToViewMatrix;
    public FloatBuffer viewToClipMatrix;
    public NgxResourceVK transparencyLayer;
    public NgxResourceVK transparencyLayerOpacity;
    public NgxResourceVK transparencyLayerMotionVectors;
    public NgxResourceVK disocclusionMask;
    public NgxResourceVK responsivityMask;
}
