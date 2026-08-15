/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.homo.superresolution.common.upscale.dlssrr;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.ngx.NgxConstants;
import io.homo.superresolution.core.ngx.NgxDLSSDCreateParams;
import io.homo.superresolution.core.ngx.NgxFeature;
import io.homo.superresolution.core.ngx.NgxImageSubresourceRange;
import io.homo.superresolution.core.ngx.NgxInitializer;
import io.homo.superresolution.core.ngx.NgxParameters;
import io.homo.superresolution.core.ngx.NgxResourceVK;
import io.homo.superresolution.core.ngx.NgxVKDLSSDEvalParams;
import io.homo.superresolution.core.ngx.NgxVulkan;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** NVIDIA DLSS Ray Reconstruction implementation. */
public class DLSSRR extends DLSSRRVulkanInteropAlgorithm {
    private NgxFeature feature;
    private NgxParameters parameters;
    private int roughnessMode = -1;
    private int depthType = -1;
    private final Map<InFlightFrameResourcesSet, NgxDispatchResources> dispatchResources = new IdentityHashMap<>();

    @Override
    protected boolean isVulkanInteropReady() {
        return feature != null && feature.isValid() && parameters != null && parameters.isValid();
    }

    @Override
    public boolean dispatch(DispatchResource resource) {
        if (resource.resources() == null || !hasRequiredInputResources(resource.resources())) {
            return false;
        }

        int requestedRoughnessMode = resource.resources().has(InputResourceType.NormalRoughness)
                ? NgxConstants.DLSS_ROUGHNESS_MODE_PACKED
                : NgxConstants.DLSS_ROUGHNESS_MODE_UNPACKED;
        int requestedDepthType = NgxConstants.DLSS_DEPTH_TYPE_HARDWARE;
        if (!isVulkanInteropReady()
                || roughnessMode != requestedRoughnessMode
                || depthType != requestedDepthType) {
            RenderSystems.vulkan().device().getMainQueue().waitIdle();
            destroyDispatchResources();
            destroyNgxContext();
            try {
                recreateNgxContext(initDesc, requestedRoughnessMode, requestedDepthType);
                roughnessMode = requestedRoughnessMode;
                depthType = requestedDepthType;
            } catch (RuntimeException | Error error) {
                roughnessMode = -1;
                depthType = -1;
                throw error;
            }
        }
        return super.dispatch(resource);
    }

    @Override
    protected boolean hasRequiredInputResources(InputResourceSet resources) {
        return resources.has(InputResourceType.Color)
                && resources.has(InputResourceType.Depth)
                && resources.has(InputResourceType.MotionVectors)
                && resources.has(InputResourceType.DiffuseAlbedo)
                && resources.has(InputResourceType.SpecularAlbedo)
                && (resources.has(InputResourceType.NormalRoughness)
                    || (resources.has(InputResourceType.Normals) && resources.has(InputResourceType.Roughness)))
                && (resources.has(InputResourceType.SpecularMotionVectors)
                    || resources.has(InputResourceType.SpecularHitDistance));
    }

    @Override
    protected void onInteropResourcesCreated() {
        if (roughnessMode != -1 && depthType != -1) {
            recreateNgxContext(initDesc, roughnessMode, depthType);
        }
    }

    @Override
    protected void onBeforeInteropResourcesDestroyed() {
        destroyDispatchResources();
        destroyNgxContext();
    }

    @Override
    protected void dispatchVulkanUpscale(VulkanCommandBuffer commandBuffer, InFlightFrameResourcesSet frame) {
        if (feature == null || parameters == null || frame.frameData() == null) return;
        NgxDispatchResources resources = dispatchResources.computeIfAbsent(frame, NgxDispatchResources::new);
        resources.update(frame);
        NgxVKDLSSDEvalParams eval = resources.eval;
        eval.jitterOffsetX = frame.frameData().jitterOffset.x;
        eval.jitterOffsetY = frame.frameData().jitterOffset.y;
        eval.renderSubrectDimensions.width = frame.frameData().renderWidth;
        eval.renderSubrectDimensions.height = frame.frameData().renderHeight;
        eval.motionVectorScaleX = frame.frameData().renderSize.x;
        eval.motionVectorScaleY = frame.frameData().renderSize.y;
        eval.reset = consumeHistoryReset() ? 1 : 0;
        eval.preExposure = frame.frameData().preExposure;
        eval.exposureScale = 1.0f;
        eval.frameTimeDeltaInMsec = frame.frameData().frameTimeDelta;
        int result = NgxVulkan.evaluateDLSSD(commandBuffer.getNativeCommandBuffer().address(), feature, parameters, eval);
        if (!NgxConstants.succeeded(result)) SuperResolution.LOGGER.error("NGX DLSS RR evaluation failed. Result: {}", result);
    }

    private void recreateNgxContext(
            InitializationDescription description,
            int requestedRoughnessMode,
            int requestedDepthType
    ) {
        VulkanDevice device = RenderSystems.vulkan().device();
        if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) {
            NgxInitializer.shutdown();
            if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) throw new IllegalStateException("NGX is unavailable for the current GPU");
        }
        NgxParameters newParameters = new NgxParameters();
        requireSuccess("NVSDK_NGX_VULKAN_GetCapabilityParameters", NgxVulkan.getCapabilityParameters(newParameters));
        NgxFeature newFeature = new NgxFeature();
        VulkanCommandBuffer commandBuffer = device.createCommandBuffer();
        try {
            configurePreset(newParameters);
            NgxDLSSDCreateParams create = new NgxDLSSDCreateParams();
            create.feature.width = RenderHandlerManager.getRenderWidth();
            create.feature.height = RenderHandlerManager.getRenderHeight();
            create.feature.targetWidth = RenderHandlerManager.getScreenWidth();
            create.feature.targetHeight = RenderHandlerManager.getScreenHeight();
            create.featureCreateFlags = NgxConstants.DLSS_FLAG_MV_LOW_RES
                    | NgxConstants.DLSS_FLAG_HDR
                    | (description.isMotionJittered() ? NgxConstants.DLSS_FLAG_MV_JITTERED : 0);
            create.roughnessMode = requestedRoughnessMode;
            create.depthType = requestedDepthType;
            commandBuffer.begin();
            requireSuccess("NGX_VULKAN_CREATE_DLSSD", NgxVulkan.createDLSSD(
                    commandBuffer.getNativeCommandBuffer().address(), 1, 1, newFeature, newParameters, create));
            commandBuffer.end();
            device.submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();
            parameters = newParameters;
            feature = newFeature;
        } catch (RuntimeException | Error error) {
            newFeature.close(); newParameters.close(); throw error;
        } finally { commandBuffer.destroy(); }
    }

    private static void configurePreset(NgxParameters parameters) {
        int preset = SuperResolutionConfig.SPECIAL.DLSS.RENDER_PRESET.get().getCode();
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_DLAA, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_QUALITY, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_BALANCED, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_PERFORMANCE, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_ULTRA_PERFORMANCE, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_ULTRA_QUALITY, preset);
    }

    private void destroyNgxContext() {
        if (feature != null) { int result = feature.release(); if (!NgxConstants.succeeded(result)) SuperResolution.LOGGER.error("Failed to release DLSS RR feature: {}", result); feature = null; }
        if (parameters != null) { int result = parameters.destroy(); if (!NgxConstants.succeeded(result)) SuperResolution.LOGGER.error("Failed to destroy DLSS RR parameters: {}", result); parameters = null; }
    }

    private void destroyDispatchResources() {
        for (NgxDispatchResources value : dispatchResources.values()) value.close();
        dispatchResources.clear();
    }

    private final class NgxDispatchResources implements AutoCloseable {
        private final NgxVKDLSSDEvalParams eval = new NgxVKDLSSDEvalParams();
        private final EnumMap<InputResourceType, NgxResourceVK> resources = new EnumMap<>(InputResourceType.class);
        private final EnumMap<InputResourceType, VulkanTexture> sourceTextures = new EnumMap<>(InputResourceType.class);
        private final FloatBuffer worldToView = MemoryUtil.memAllocFloat(16);
        private final FloatBuffer viewToClip = MemoryUtil.memAllocFloat(16);

        private NgxDispatchResources(InFlightFrameResourcesSet frame) {
            eval.worldToViewMatrix = worldToView;
            eval.viewToClipMatrix = viewToClip;
            update(frame);
        }

        private void update(InFlightFrameResourcesSet frame) {
            Map<InputResourceType, VulkanTexture> presentTextures = frame.presentInputVkTextures();
            for (InputResourceType type : InputResourceType.values()) {
                if (!presentTextures.containsKey(type)) {
                    NgxResourceVK previous = resources.remove(type);
                    if (previous != null) {
                        previous.close();
                    }
                    sourceTextures.remove(type);
                }
            }
            for (Map.Entry<InputResourceType, VulkanTexture> entry : presentTextures.entrySet()) {
                VulkanTexture previousTexture = sourceTextures.get(entry.getKey());
                if (previousTexture != entry.getValue()) {
                    NgxResourceVK previous = resources.remove(entry.getKey());
                    if (previous != null) {
                        previous.close();
                    }
                    boolean readWrite = entry.getKey() != InputResourceType.Depth
                            && entry.getKey() != InputResourceType.Exposure;
                    resources.put(entry.getKey(), createResource(entry.getValue(), readWrite));
                    sourceTextures.put(entry.getKey(), entry.getValue());
                }
            }
            eval.feature.inputColor = resource(frame, InputResourceType.Color);
            eval.feature.output = createOutput(frame);
            eval.depth = resource(frame, InputResourceType.Depth);
            eval.motionVectors = resource(frame, InputResourceType.MotionVectors);
            eval.diffuseAlbedo = resource(frame, InputResourceType.DiffuseAlbedo);
            eval.specularAlbedo = resource(frame, InputResourceType.SpecularAlbedo);
            boolean packedRoughness = resource(frame, InputResourceType.NormalRoughness) != null;
            eval.normals = packedRoughness
                    ? resource(frame, InputResourceType.NormalRoughness)
                    : resource(frame, InputResourceType.Normals);
            eval.roughness = packedRoughness ? null : resource(frame, InputResourceType.Roughness);
            eval.motionVectorsReflections = resource(frame, InputResourceType.SpecularMotionVectors);
            eval.specularHitDistance = resource(frame, InputResourceType.SpecularHitDistance);
            eval.exposureTexture = resource(frame, InputResourceType.Exposure);
            eval.transparencyLayer = resource(frame, InputResourceType.TransparencyLayer);
            eval.transparencyLayerOpacity = resource(frame, InputResourceType.TransparencyLayerOpacity);
            eval.colorBeforeTransparency = resource(frame, InputResourceType.ColorBeforeTransparency);
            eval.screenSpaceSubsurfaceScatteringGuide = resource(frame, InputResourceType.ScreenSpaceSubsurfaceScatteringGuide);
            eval.depthOfFieldGuide = resource(frame, InputResourceType.DepthOfFieldGuide);
            worldToView.clear(); frame.frameData().viewMatrix.get(0, worldToView); worldToView.limit(16); worldToView.position(0);
            viewToClip.clear(); frame.frameData().projectionMatrix.get(0, viewToClip); viewToClip.limit(16); viewToClip.position(0);
        }

        private NgxResourceVK createOutput(InFlightFrameResourcesSet frame) {
            NgxResourceVK output = eval.feature.output;
            if (output == null) eval.feature.output = createResource(frame.outputColorVkTexture, true);
            return eval.feature.output;
        }
        private NgxResourceVK resource(InFlightFrameResourcesSet frame, InputResourceType type) {
            return frame.presentInputVkTextures().containsKey(type) ? resources.get(type) : null;
        }

        @Override
        public void close() {
            for (NgxResourceVK value : resources.values()) value.close();
            resources.clear();
            sourceTextures.clear();
            if (eval.feature.output != null) { eval.feature.output.close(); eval.feature.output = null; }
            MemoryUtil.memFree(worldToView); MemoryUtil.memFree(viewToClip);
        }
    }

    private static NgxResourceVK createResource(VulkanTexture texture, boolean readWrite) {
        NgxImageSubresourceRange range = new NgxImageSubresourceRange();
        range.aspectMask = texture.getAspectMask(); range.baseMipLevel = 0;
        range.levelCount = texture.getMipmapSettings().getLevels(); range.baseArrayLayer = 0; range.layerCount = 1;
        return NgxVulkan.createImageViewResourceVK(texture.getImageView(), texture.handle(), range,
                texture.getTextureFormat().vk(), texture.getWidth(), texture.getHeight(), readWrite);
    }

    private static void requireSuccess(String operation, int result) {
        if (!NgxConstants.succeeded(result)) throw new IllegalStateException(operation + " failed. NGX result: " + result);
    }
}
