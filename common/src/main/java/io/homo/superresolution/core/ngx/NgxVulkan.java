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

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public final class NgxVulkan {
    private NgxVulkan() {
    }

    public static int initWithProjectId(
            String projectId,
            int engineType,
            String engineVersion,
            String applicationDataPath,
            long instance,
            long physicalDevice,
            long device,
            long getInstanceProcAddr,
            long getDeviceProcAddr,
            NgxFeatureCommonInfo featureInfo,
            int sdkVersion
    ) {
        return NgxNative.nInitWithProjectId(
                projectId,
                engineType,
                engineVersion,
                applicationDataPath,
                instance,
                physicalDevice,
                device,
                getInstanceProcAddr,
                getDeviceProcAddr,
                featureInfo,
                sdkVersion
        );
    }

    public static int shutdown() {
        return NgxNative.nShutdown();
    }

    public static int getFeatureRequirements(
            long instance,
            long physicalDevice,
            NgxFeatureDiscoveryInfo discoveryInfo,
            NgxFeatureRequirement outRequirements
    ) {
        return NgxNative.nGetFeatureRequirements(instance, physicalDevice, discoveryInfo, outRequirements);
    }

    public static int allocateParameters(NgxParameters outParameters) {
        return NgxNative.nAllocateParameters(outParameters);
    }

    public static int getCapabilityParameters(NgxParameters outParameters) {
        return NgxNative.nGetCapabilityParameters(outParameters);
    }

    public static int destroyParameters(NgxParameters parameters) {
        return parameters.destroy();
    }

    public static int createFeature(long commandBuffer, int feature, NgxParameters parameters, NgxFeature outFeature) {
        return NgxNative.nCreateFeature(commandBuffer, feature, requirePointer(parameters), outFeature);
    }

    public static int releaseFeature(NgxFeature feature) {
        return feature.release();
    }

    public static int evaluateFeature(
            long commandBuffer,
            NgxFeature feature,
            NgxParameters parameters,
            NgxProgressCallback progressCallback
    ) {
        return NgxNative.nEvaluateFeature(
                commandBuffer,
                requirePointer(feature),
                requirePointer(parameters),
                progressCallback
        );
    }

    public static NgxResourceVK createImageViewResourceVK(
            long imageView,
            long image,
            NgxImageSubresourceRange subresourceRange,
            int format,
            int width,
            int height,
            boolean readWrite
    ) {
        NgxResourceVK resource = new NgxResourceVK();
        resource.type = NgxConstants.RESOURCE_VK_IMAGE_VIEW;
        resource.imageViewInfo.imageView = imageView;
        resource.imageViewInfo.image = image;
        resource.imageViewInfo.subresourceRange = subresourceRange == null
                ? new NgxImageSubresourceRange()
                : subresourceRange;
        resource.imageViewInfo.format = format;
        resource.imageViewInfo.width = width;
        resource.imageViewInfo.height = height;
        resource.readWrite = readWrite;
        return resource;
    }

    public static int createDLSS(
            long commandBuffer,
            int creationNodeMask,
            int visibilityNodeMask,
            NgxFeature outFeature,
            NgxParameters parameters,
            NgxDLSSCreateParams createParams
    ) {
        parameters.setUnsignedInt(NgxConstants.CREATION_NODE_MASK, creationNodeMask);
        parameters.setUnsignedInt(NgxConstants.VISIBILITY_NODE_MASK, visibilityNodeMask);
        parameters.setUnsignedInt(NgxConstants.WIDTH, createParams.feature.width);
        parameters.setUnsignedInt(NgxConstants.HEIGHT, createParams.feature.height);
        parameters.setUnsignedInt(NgxConstants.OUT_WIDTH, createParams.feature.targetWidth);
        parameters.setUnsignedInt(NgxConstants.OUT_HEIGHT, createParams.feature.targetHeight);
        parameters.setInt(NgxConstants.PERF_QUALITY_VALUE, createParams.feature.perfQualityValue);
        parameters.setInt(NgxConstants.DLSS_CREATE_FLAGS, createParams.featureCreateFlags);
        parameters.setInt(NgxConstants.DLSS_ENABLE_OUTPUT_SUBRECTS, createParams.enableOutputSubrects ? 1 : 0);
        return createFeature(commandBuffer, NgxConstants.FEATURE_SUPER_SAMPLING, parameters, outFeature);
    }

    public static int createDLSSD(
            long commandBuffer,
            int creationNodeMask,
            int visibilityNodeMask,
            NgxFeature outFeature,
            NgxParameters parameters,
            NgxDLSSDCreateParams createParams
    ) {
        parameters.setUnsignedInt(NgxConstants.CREATION_NODE_MASK, creationNodeMask);
        parameters.setUnsignedInt(NgxConstants.VISIBILITY_NODE_MASK, visibilityNodeMask);
        parameters.setUnsignedInt(NgxConstants.WIDTH, createParams.feature.width);
        parameters.setUnsignedInt(NgxConstants.HEIGHT, createParams.feature.height);
        parameters.setUnsignedInt(NgxConstants.OUT_WIDTH, createParams.feature.targetWidth);
        parameters.setUnsignedInt(NgxConstants.OUT_HEIGHT, createParams.feature.targetHeight);
        parameters.setInt(NgxConstants.PERF_QUALITY_VALUE, createParams.feature.perfQualityValue);
        parameters.setInt(NgxConstants.DLSS_CREATE_FLAGS, createParams.featureCreateFlags);
        parameters.setInt(NgxConstants.DLSS_ENABLE_OUTPUT_SUBRECTS, createParams.enableOutputSubrects ? 1 : 0);
        parameters.setInt(NgxConstants.DLSS_DENOISE_MODE, NgxConstants.DLSS_DENOISE_MODE_DL_UNIFIED);
        parameters.setUnsignedInt(NgxConstants.DLSS_ROUGHNESS_MODE, createParams.roughnessMode);
        parameters.setUnsignedInt(NgxConstants.DLSS_USE_HARDWARE_DEPTH, createParams.depthType);
        return createFeature(commandBuffer, NgxConstants.FEATURE_RAY_RECONSTRUCTION, parameters, outFeature);
    }

    public static int evaluateDLSS(
            long commandBuffer,
            NgxFeature feature,
            NgxParameters parameters,
            NgxVKDLSSEvalParams evalParams
    ) {
        parameters.setPointer(NgxConstants.COLOR, address(evalParams.feature.inputColor));
        parameters.setPointer(NgxConstants.OUTPUT, address(evalParams.feature.output));
        parameters.setPointer(NgxConstants.DEPTH, address(evalParams.depth));
        parameters.setPointer(NgxConstants.MOTION_VECTORS, address(evalParams.motionVectors));
        parameters.setFloat(NgxConstants.JITTER_OFFSET_X, evalParams.jitterOffsetX);
        parameters.setFloat(NgxConstants.JITTER_OFFSET_Y, evalParams.jitterOffsetY);
        parameters.setFloat(NgxConstants.SHARPNESS, evalParams.feature.sharpness);
        parameters.setInt(NgxConstants.RESET, evalParams.reset);
        parameters.setFloat(NgxConstants.MOTION_VECTOR_SCALE_X,
                evalParams.motionVectorScaleX == 0.0f ? 1.0f : evalParams.motionVectorScaleX);
        parameters.setFloat(NgxConstants.MOTION_VECTOR_SCALE_Y,
                evalParams.motionVectorScaleY == 0.0f ? 1.0f : evalParams.motionVectorScaleY);
        parameters.setPointer(NgxConstants.TRANSPARENCY_MASK, address(evalParams.transparencyMask));
        parameters.setPointer(NgxConstants.EXPOSURE_TEXTURE, address(evalParams.exposureTexture));
        parameters.setPointer(NgxConstants.DLSS_BIAS_CURRENT_COLOR_MASK, address(evalParams.biasCurrentColorMask));
        for (int i = 0; i < NgxConstants.DLSS_GBUFFER.length; i++) {
            parameters.setPointer(NgxConstants.DLSS_GBUFFER[i], address(evalParams.gBuffer.attributes[i]));
        }
        parameters.setUnsignedInt("TonemapperType", evalParams.toneMapperType);
        parameters.setPointer("MotionVectors3D", address(evalParams.motionVectors3D));
        parameters.setPointer("IsParticleMask", address(evalParams.particleMask));
        parameters.setPointer("AnimatedTextureMask", address(evalParams.animatedTextureMask));
        parameters.setPointer("DepthHighRes", address(evalParams.depthHighRes));
        parameters.setPointer("Position.ViewSpace", address(evalParams.positionViewSpace));
        parameters.setFloat(NgxConstants.FRAME_TIME_DELTA_MS, evalParams.frameTimeDeltaInMsec);
        parameters.setPointer("RayTracingHitDistance", address(evalParams.rayTracingHitDistance));
        parameters.setPointer("MotionVectorsReflection", address(evalParams.motionVectorsReflections));
        setCoordinates(parameters, "DLSS.Input.Color.Subrect.Base", evalParams.colorSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Depth.Subrect.Base", evalParams.depthSubrectBase);
        setCoordinates(parameters, "DLSS.Input.MV.SubrectBase", evalParams.motionVectorSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Translucency.Subrect.Base", evalParams.translucencySubrectBase);
        setCoordinates(parameters, "DLSS.Input.Bias.Current.Color.Subrect.Base", evalParams.biasCurrentColorSubrectBase);
        setCoordinates(parameters, "DLSS.Output.Subrect.Base", evalParams.outputSubrectBase);
        parameters.setUnsignedInt(
                "DLSS.Render.Subrect.Dimensions.Width",
                evalParams.renderSubrectDimensions.width
        );
        parameters.setUnsignedInt(
                "DLSS.Render.Subrect.Dimensions.Height",
                evalParams.renderSubrectDimensions.height
        );
        parameters.setFloat(NgxConstants.DLSS_PRE_EXPOSURE,
                evalParams.preExposure == 0.0f ? 1.0f : evalParams.preExposure);
        parameters.setFloat(NgxConstants.DLSS_EXPOSURE_SCALE,
                evalParams.exposureScale == 0.0f ? 1.0f : evalParams.exposureScale);
        parameters.setInt(NgxConstants.DLSS_INDICATOR_INVERT_X, evalParams.indicatorInvertXAxis);
        parameters.setInt(NgxConstants.DLSS_INDICATOR_INVERT_Y, evalParams.indicatorInvertYAxis);
        return evaluateFeature(commandBuffer, feature, parameters, null);
    }

    public static int evaluateDLSSD(
            long commandBuffer,
            NgxFeature feature,
            NgxParameters parameters,
            NgxVKDLSSDEvalParams evalParams
    ) {
        parameters.setPointer(NgxConstants.COLOR, address(evalParams.feature.inputColor));
        parameters.setPointer(NgxConstants.OUTPUT, address(evalParams.feature.output));
        parameters.setPointer(NgxConstants.DEPTH, address(evalParams.depth));
        parameters.setPointer(NgxConstants.MOTION_VECTORS, address(evalParams.motionVectors));
        parameters.setFloat(NgxConstants.JITTER_OFFSET_X, evalParams.jitterOffsetX);
        parameters.setFloat(NgxConstants.JITTER_OFFSET_Y, evalParams.jitterOffsetY);
        parameters.setInt(NgxConstants.RESET, evalParams.reset);
        parameters.setFloat(NgxConstants.MOTION_VECTOR_SCALE_X,
                evalParams.motionVectorScaleX == 0.0f ? 1.0f : evalParams.motionVectorScaleX);
        parameters.setFloat(NgxConstants.MOTION_VECTOR_SCALE_Y,
                evalParams.motionVectorScaleY == 0.0f ? 1.0f : evalParams.motionVectorScaleY);
        parameters.setPointer(NgxConstants.TRANSPARENCY_MASK, address(evalParams.transparencyMask));
        parameters.setPointer(NgxConstants.EXPOSURE_TEXTURE, address(evalParams.exposureTexture));
        parameters.setPointer(NgxConstants.DLSS_BIAS_CURRENT_COLOR_MASK, address(evalParams.biasCurrentColorMask));
        setDLSSDGBuffer(parameters, evalParams);
        parameters.setUnsignedInt("TonemapperType", evalParams.toneMapperType);
        parameters.setPointer("MotionVectors3D", address(evalParams.motionVectors3D));
        parameters.setPointer("IsParticleMask", address(evalParams.particleMask));
        parameters.setPointer("AnimatedTextureMask", address(evalParams.animatedTextureMask));
        parameters.setPointer("DepthHighRes", address(evalParams.depthHighRes));
        parameters.setPointer("Position.ViewSpace", address(evalParams.positionViewSpace));
        parameters.setFloat(NgxConstants.FRAME_TIME_DELTA_MS, evalParams.frameTimeDeltaInMsec);
        parameters.setPointer("RayTracingHitDistance", address(evalParams.rayTracingHitDistance));

        parameters.setPointer(NgxConstants.DLSS_DIFFUSE_ALBEDO, address(evalParams.diffuseAlbedo));
        parameters.setPointer(NgxConstants.DLSS_SPECULAR_ALBEDO, address(evalParams.specularAlbedo));
        parameters.setPointer(NgxConstants.DLSS_GBUFFER[5], address(evalParams.normals));
        parameters.setPointer(NgxConstants.DLSS_GBUFFER[1], address(evalParams.roughness));
        setCoordinates(parameters, "DLSS.Input.DiffuseAlbedo.Subrect.Base", evalParams.diffuseAlbedoSubrectBase);
        setCoordinates(parameters, "DLSS.Input.SpecularAlbedo.Subrect.Base", evalParams.specularAlbedoSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Normals.Subrect.Base", evalParams.normalsSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Roughness.Subrect.Base", evalParams.roughnessSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Color.Subrect.Base", evalParams.colorSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Depth.Subrect.Base", evalParams.depthSubrectBase);
        setCoordinates(parameters, "DLSS.Input.MV.SubrectBase", evalParams.motionVectorSubrectBase);
        setCoordinates(parameters, "DLSS.Input.Translucency.Subrect.Base", evalParams.translucencySubrectBase);
        setCoordinates(parameters, "DLSS.Input.Bias.Current.Color.Subrect.Base", evalParams.biasCurrentColorSubrectBase);
        setCoordinates(parameters, "DLSS.Output.Subrect.Base", evalParams.outputSubrectBase);
        parameters.setUnsignedInt("DLSS.Render.Subrect.Dimensions.Width", evalParams.renderSubrectDimensions.width);
        parameters.setUnsignedInt("DLSS.Render.Subrect.Dimensions.Height", evalParams.renderSubrectDimensions.height);
        parameters.setFloat(NgxConstants.DLSS_PRE_EXPOSURE,
                evalParams.preExposure == 0.0f ? 1.0f : evalParams.preExposure);
        parameters.setFloat(NgxConstants.DLSS_EXPOSURE_SCALE,
                evalParams.exposureScale == 0.0f ? 1.0f : evalParams.exposureScale);
        parameters.setInt(NgxConstants.DLSS_INDICATOR_INVERT_X, evalParams.indicatorInvertXAxis);
        parameters.setInt(NgxConstants.DLSS_INDICATOR_INVERT_Y, evalParams.indicatorInvertYAxis);

        setDLSSDResource(parameters, NgxConstants.DLSSD_ALPHA, evalParams.alpha, evalParams.alphaSubrectBase);
        setDLSSDResource(parameters, NgxConstants.DLSSD_OUTPUT_ALPHA, evalParams.outputAlpha, evalParams.outputAlphaSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ReflectedAlbedo", evalParams.reflectedAlbedo, evalParams.reflectedAlbedoSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeParticles", evalParams.colorBeforeParticles, evalParams.colorBeforeParticlesSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterParticles", evalParams.colorAfterParticles, evalParams.colorAfterParticlesSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeTransparency", evalParams.colorBeforeTransparency, evalParams.colorBeforeTransparencySubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterTransparency", evalParams.colorAfterTransparency, evalParams.colorAfterTransparencySubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeFog", evalParams.colorBeforeFog, evalParams.colorBeforeFogSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterFog", evalParams.colorAfterFog, evalParams.colorAfterFogSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ScreenSpaceSubsurfaceScatteringGuide",
                evalParams.screenSpaceSubsurfaceScatteringGuide,
                evalParams.screenSpaceSubsurfaceScatteringGuideSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeScreenSpaceSubsurfaceScattering",
                evalParams.colorBeforeScreenSpaceSubsurfaceScattering,
                evalParams.colorBeforeScreenSpaceSubsurfaceScatteringSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterScreenSpaceSubsurfaceScattering",
                evalParams.colorAfterScreenSpaceSubsurfaceScattering,
                evalParams.colorAfterScreenSpaceSubsurfaceScatteringSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ScreenSpaceRefractionGuide", evalParams.screenSpaceRefractionGuide,
                evalParams.screenSpaceRefractionGuideSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeScreenSpaceRefraction",
                evalParams.colorBeforeScreenSpaceRefraction,
                evalParams.colorBeforeScreenSpaceRefractionSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterScreenSpaceRefraction",
                evalParams.colorAfterScreenSpaceRefraction,
                evalParams.colorAfterScreenSpaceRefractionSubrectBase);
        setDLSSDResource(parameters, "DLSSD.DepthOfFieldGuide", evalParams.depthOfFieldGuide,
                evalParams.depthOfFieldGuideSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorBeforeDepthOfField", evalParams.colorBeforeDepthOfField,
                evalParams.colorBeforeDepthOfFieldSubrectBase);
        setDLSSDResource(parameters, "DLSSD.ColorAfterDepthOfField", evalParams.colorAfterDepthOfField,
                evalParams.colorAfterDepthOfFieldSubrectBase);
        setDLSSDResource(parameters, "DLSSD.DiffuseHitDistance", evalParams.diffuseHitDistance,
                evalParams.diffuseHitDistanceSubrectBase);
        setDLSSDResource(parameters, "DLSSD.SpecularHitDistance", evalParams.specularHitDistance,
                evalParams.specularHitDistanceSubrectBase);
        setDLSSDResource(parameters, "DLSSD.DiffuseRayDirection", evalParams.diffuseRayDirection,
                evalParams.diffuseRayDirectionSubrectBase);
        setDLSSDResource(parameters, "DLSSD.SpecularRayDirection", evalParams.specularRayDirection,
                evalParams.specularRayDirectionSubrectBase);
        setDLSSDResource(parameters, "DLSSD.DiffuseRayDirectionHitDistance",
                evalParams.diffuseRayDirectionHitDistance,
                evalParams.diffuseRayDirectionHitDistanceSubrectBase);
        setDLSSDResource(parameters, "DLSSD.SpecularRayDirectionHitDistance",
                evalParams.specularRayDirectionHitDistance,
                evalParams.specularRayDirectionHitDistanceSubrectBase);
        parameters.setPointer("WorldToViewMatrix", address(evalParams.worldToViewMatrix));
        parameters.setPointer("ViewToClipMatrix", address(evalParams.viewToClipMatrix));
        setDLSSDResource(parameters, "DLSS.TransparencyLayer", evalParams.transparencyLayer,
                evalParams.transparencyLayerSubrectBase);
        setDLSSDResource(parameters, "DLSS.TransparencyLayerOpacity", evalParams.transparencyLayerOpacity,
                evalParams.transparencyLayerOpacitySubrectBase);
        setDLSSDResource(parameters, "DLSS.TransparencyLayerMvecs", evalParams.transparencyLayerMotionVectors,
                evalParams.transparencyLayerMotionVectorsSubrectBase);
        setDLSSDResource(parameters, "DLSS.DisocclusionMask", evalParams.disocclusionMask,
                evalParams.disocclusionMaskSubrectBase);
        setDLSSDResource(parameters, NgxConstants.DLSSD_RESPONSIVITY_MASK, evalParams.responsivityMask,
                evalParams.responsivityMaskSubrectBase);
        return evaluateFeature(commandBuffer, feature, parameters, null);
    }

    public static int createDLSSFG(
            long commandBuffer,
            int creationNodeMask,
            int visibilityNodeMask,
            NgxFeature outFeature,
            NgxParameters parameters,
            NgxDLSSFGCreateParams createParams
    ) {
        parameters.setUnsignedInt(NgxConstants.CREATION_NODE_MASK, creationNodeMask);
        parameters.setUnsignedInt(NgxConstants.VISIBILITY_NODE_MASK, visibilityNodeMask);
        parameters.setUnsignedInt(NgxConstants.WIDTH, createParams.width);
        parameters.setUnsignedInt(NgxConstants.HEIGHT, createParams.height);
        parameters.setUnsignedInt(NgxConstants.DLSSFG_BACKBUFFER_FORMAT, createParams.nativeBackbufferFormat);
        parameters.setUnsignedInt(NgxConstants.DLSSFG_INTERNAL_WIDTH, createParams.renderWidth);
        parameters.setUnsignedInt(NgxConstants.DLSSFG_INTERNAL_HEIGHT, createParams.renderHeight);
        parameters.setUnsignedInt(NgxConstants.DLSSFG_DYNAMIC_RESOLUTION, createParams.dynamicResolutionScaling ? 1 : 0);
        return createFeature(commandBuffer, NgxConstants.FEATURE_FRAME_GENERATION, parameters, outFeature);
    }

    public static int evaluateDLSSFG(
            long commandBuffer,
            NgxFeature feature,
            NgxParameters parameters,
            NgxVKDLSSFGEvalParams evalParams,
            NgxDLSSFGOptEvalParams optionalParams
    ) {
        parameters.setPointer(NgxConstants.DLSSFG_BACKBUFFER, address(evalParams.backbuffer));
        parameters.setPointer(NgxConstants.DLSSFG_MVECS, address(evalParams.motionVectors));
        parameters.setPointer(NgxConstants.DLSSFG_DEPTH, address(evalParams.depth));
        parameters.setPointer(NgxConstants.DLSSFG_HUDLESS, address(evalParams.hudless));
        parameters.setPointer(NgxConstants.DLSSFG_UI, address(evalParams.ui));
        parameters.setPointer(NgxConstants.DLSSFG_UI_ALPHA, address(evalParams.uiAlpha));
        parameters.setPointer(NgxConstants.DLSSFG_BIDIRECTIONAL_DISTORTION_FIELD,
                address(evalParams.bidirectionalDistortionField));
        parameters.setPointer(NgxConstants.DLSSFG_OUTPUT_INTERPOLATED, address(evalParams.outputInterpolatedFrame));
        parameters.setPointer(NgxConstants.DLSSFG_OUTPUT_REAL, address(evalParams.outputRealFrame));
        parameters.setPointer(NgxConstants.DLSSFG_OUTPUT_DISABLE_INTERPOLATION,
                address(evalParams.outputDisableInterpolation));
        if (optionalParams != null) {
            writeDLSSFGOptionalParameters(parameters, optionalParams);
        }
        return evaluateFeature(commandBuffer, feature, parameters, null);
    }

    private static void writeDLSSFGOptionalParameters(NgxParameters parameters, NgxDLSSFGOptEvalParams value) {
        parameters.setUnsignedInt(NgxConstants.DLSSFG_MULTI_FRAME_COUNT, value.multiFrameCount);
        parameters.setUnsignedInt(NgxConstants.DLSSFG_MULTI_FRAME_INDEX, value.multiFrameIndex);
        parameters.setPointer("DLSSG.CameraViewToClip", address(value.cameraViewToClip));
        parameters.setPointer("DLSSG.ClipToCameraView", address(value.clipToCameraView));
        parameters.setPointer("DLSSG.ClipToLensClip", address(value.clipToLensClip));
        parameters.setPointer("DLSSG.ClipToPrevClip", address(value.clipToPrevClip));
        parameters.setPointer("DLSSG.PrevClipToClip", address(value.prevClipToClip));
        setFloat2(parameters, "DLSSG.JitterOffset", value.jitterOffset);
        setFloat2(parameters, "DLSSG.MvecScale", value.motionVectorScale);
        setFloat2(parameters, "DLSSG.CameraPinholeOffset", value.cameraPinholeOffset);
        setFloat3(parameters, "DLSSG.CameraPos", value.cameraPosition);
        setFloat3(parameters, "DLSSG.CameraUp", value.cameraUp);
        setFloat3(parameters, "DLSSG.CameraRight", value.cameraRight);
        setFloat3(parameters, "DLSSG.CameraFwd", value.cameraForward);
        parameters.setFloat("DLSSG.CameraNear", value.cameraNear);
        parameters.setFloat("DLSSG.CameraFar", value.cameraFar);
        parameters.setFloat("DLSSG.CameraFOV", value.cameraFov);
        parameters.setFloat("DLSSG.CameraAspectRatio", value.cameraAspectRatio);
        parameters.setUnsignedInt("DLSSG.ColorBuffersHDR", bool(value.colorBuffersHdr));
        parameters.setUnsignedInt("DLSSG.DepthInverted", bool(value.depthInverted));
        parameters.setUnsignedInt("DLSSG.CameraMotionIncluded", bool(value.cameraMotionIncluded));
        parameters.setUnsignedInt("DLSSG.Reset", bool(value.reset));
        parameters.setUnsignedInt("DLSSG.AutomodeOverrideReset", bool(value.automodeOverrideReset));
        parameters.setUnsignedInt("DLSSG.NotRenderingGameFrames", bool(value.notRenderingGameFrames));
        parameters.setUnsignedInt("DLSSG.OrthoProjection", bool(value.orthoProjection));
        parameters.setFloat("DLSSG.MvecInvalidValue", value.motionVectorsInvalidValue);
        parameters.setUnsignedInt("DLSSG.MvecDilated", bool(value.motionVectorsDilated));
        parameters.setUnsignedInt(NgxConstants.DLSSFG_MVEC_JITTERED, bool(value.motionVectorsJittered));
        parameters.setUnsignedInt("DLSSG.MenuDetectionEnabled", bool(value.menuDetectionEnabled));
        setSubrect(parameters, "DLSSG.MVecsSubrect", value.motionVectorsSubrectBase, value.motionVectorsSubrectSize);
        setSubrect(parameters, "DLSSG.DepthSubrect", value.depthSubrectBase, value.depthSubrectSize);
        setSubrect(parameters, "DLSSG.HUDLessSubrect", value.hudlessSubrectBase, value.hudlessSubrectSize);
        setSubrect(parameters, "DLSSG.UISubrect", value.uiSubrectBase, value.uiSubrectSize);
        setSubrect(parameters, "DLSSG.UIAlphaSubrect", value.uiAlphaSubrectBase, value.uiAlphaSubrectSize);
        setSubrect(parameters, "DLSSG.BidirectionalDistortionFieldSubrect",
                value.bidirectionalDistortionFieldSubrectBase, value.bidirectionalDistortionFieldSubrectSize);
        parameters.setUnsignedInt("DLSSG.BidirectionalDistortionFieldLowPrecision.IsLowPrecision",
                value.bidirectionalDistortionFieldPrecisionInfo.isLowPrecision);
        parameters.setFloat("DLSSG.BidirectionalDistortionFieldLowPrecision.Bias",
                value.bidirectionalDistortionFieldPrecisionInfo.bias);
        parameters.setFloat("DLSSG.BidirectionalDistortionFieldLowPrecision.Scale",
                value.bidirectionalDistortionFieldPrecisionInfo.scale);
        parameters.setFloat("DLSSG.MinRelativeLinearDepthObjectSeparation",
                value.minRelativeLinearDepthObjectSeparation);
        setSubrect(parameters, "DLSSG.InputBackbufferSubrect",
                value.backbufferSubrectBase, value.backbufferSubrectSize);
        setSubrect(parameters, "DLSSG.OutputInterpolatedSubrect",
                value.outputInterpolatedSubrectBase, value.outputInterpolatedSubrectSize);
        setSubrect(parameters, "DLSSG.OutputRealSubrect",
                value.outputRealSubrectBase, value.outputRealSubrectSize);
    }

    private static void setDLSSDGBuffer(NgxParameters parameters, NgxVKDLSSDEvalParams evalParams) {
        for (int i = 0; i < NgxConstants.DLSS_GBUFFER.length; i++) {
            if (i != 10) {
                parameters.setPointer(NgxConstants.DLSS_GBUFFER[i], address(evalParams.gBuffer.attributes[i]));
            }
        }
        parameters.setPointer(NgxConstants.DLSS_GBUFFER_SPECULAR_MVEC, address(evalParams.motionVectorsReflections));
    }

    private static void setDLSSDResource(
            NgxParameters parameters,
            String key,
            NgxResourceVK resource,
            NgxCoordinates subrectBase
    ) {
        parameters.setPointer(key, address(resource));
        setCoordinates(parameters, key + ".Subrect.Base", subrectBase);
    }

    private static void setCoordinates(NgxParameters parameters, String prefix, NgxCoordinates value) {
        parameters.setUnsignedInt(prefix + ".X", value.x);
        parameters.setUnsignedInt(prefix + ".Y", value.y);
    }

    private static void setSubrect(
            NgxParameters parameters,
            String prefix,
            NgxCoordinates base,
            NgxDimensions size
    ) {
        parameters.setUnsignedInt(prefix + "BaseX", base.x);
        parameters.setUnsignedInt(prefix + "BaseY", base.y);
        parameters.setUnsignedInt(prefix + "Width", size.width);
        parameters.setUnsignedInt(prefix + "Height", size.height);
    }

    private static void setFloat2(NgxParameters parameters, String prefix, float[] value) {
        parameters.setFloat(prefix + "X", value[0]);
        parameters.setFloat(prefix + "Y", value[1]);
    }

    private static void setFloat3(NgxParameters parameters, String prefix, float[] value) {
        parameters.setFloat(prefix + "X", value[0]);
        parameters.setFloat(prefix + "Y", value[1]);
        parameters.setFloat(prefix + "Z", value[2]);
    }

    private static int bool(boolean value) {
        return value ? 1 : 0;
    }

    private static long address(NgxResourceVK resource) {
        return resource == null ? 0L : resource.nativeAddress();
    }

    private static long address(FloatBuffer value) {
        return value == null ? 0L : MemoryUtil.memAddress(value);
    }

    private static long requirePointer(NgxFeature feature) {
        if (feature == null || feature.nativePointer == 0) {
            throw new IllegalArgumentException("NGX feature is not valid");
        }
        return feature.nativePointer;
    }

    private static long requirePointer(NgxParameters parameters) {
        if (parameters == null || parameters.nativePointer == 0) {
            throw new IllegalArgumentException("NGX parameters are not allocated");
        }
        return parameters.nativePointer;
    }
}
