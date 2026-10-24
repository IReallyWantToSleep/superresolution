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

package io.homo.superresolution.common.upscale.algo.dlssrr;

import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.interoplayer.*;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
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
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.FormatSource.SourceTexture;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.SizeSource.SourceSize;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.Optional;
import static io.homo.superresolution.api.interop.InteropResourceType.OutputColor;

/** NVIDIA DLSS Ray Reconstruction implementation. */
public class DLSSRR extends VulkanInteropAlgorithm {
    private static final EnumSet<InputResourceType> SUPPLEMENTAL_TYPES = EnumSet.of(
            InputResourceType.DiffuseAlbedo,
            InputResourceType.SpecularAlbedo,
            InputResourceType.Normals,
            InputResourceType.Roughness,
            InputResourceType.NormalRoughness,
            InputResourceType.SpecularMotionVectors,
            InputResourceType.SpecularHitDistance,
            InputResourceType.TransparencyLayer,
            InputResourceType.TransparencyLayerOpacity,
            InputResourceType.ColorBeforeTransparency,
            InputResourceType.ScreenSpaceSubsurfaceScatteringGuide,
            InputResourceType.DepthOfFieldGuide
    );

    private NgxFeature feature;
    private NgxParameters parameters;
    private int roughnessMode = -1;
    private int depthType = -1;
    private NgxDispatchResources dispatchResources;
    private InteropResourceAccess algorithmResources;

    @Override
    protected List<InteropResourceRequirement> getInteropResourceRequirements() {
        List<InteropResourceRequirement> requirements = new ArrayList<>(super.getInteropResourceRequirements());
        for (InputResourceType type : SUPPLEMENTAL_TYPES) {
            requirements.add(InteropResourceRequirement.input(
                    InteropResourceType.fromInput(type), Optional, SourceSize, SourceTexture, null));
        }
        return List.copyOf(requirements);
    }

    private boolean isVulkanInteropReady() {
        return feature != null && feature.isValid() && parameters != null && parameters.isValid();
    }

    @Override
    protected boolean validateExecutionResources(InteropResourceSet resources) {
        return resources.has(InteropResourceType.DiffuseAlbedo)
                && resources.has(InteropResourceType.SpecularAlbedo)
                && (resources.has(InteropResourceType.NormalRoughness)
                    || (resources.has(InteropResourceType.Normals)
                        && resources.has(InteropResourceType.Roughness)))
                && (resources.has(InteropResourceType.SpecularMotionVectors)
                    || resources.has(InteropResourceType.SpecularHitDistance));
    }

    @Override
    protected void createAlgorithmContext(InteropResourceAccess resources) {
        algorithmResources = resources;
        if (validateExecutionResources(resources.resources())) {
            roughnessMode = resources.resources().has(InteropResourceType.NormalRoughness)
                    ? NgxConstants.DLSS_ROUGHNESS_MODE_PACKED
                    : NgxConstants.DLSS_ROUGHNESS_MODE_UNPACKED;
            depthType = NgxConstants.DLSS_DEPTH_TYPE_HARDWARE;
            recreateNgxContext(roughnessMode, depthType);
            dispatchResources = new NgxDispatchResources();
        }
    }

    @Override
    protected void destroyAlgorithmContext() {
        destroyNgxContext();
        destroyDispatchResources();
        algorithmResources = null;
        roughnessMode = -1;
        depthType = -1;
    }

    @Override
    protected boolean dispatchAlgorithm(InteropDispatchResource<VulkanCommandContext> frame) {
        if (!isVulkanInteropReady() || dispatchResources == null) {
            return false;
        }
        VulkanCommandBuffer commandBuffer = frame.commandContext().commandBuffer();
        NgxDispatchResources resources = dispatchResources;
        resources.update(frame);
        NgxVKDLSSDEvalParams eval = resources.eval;
        eval.jitterOffsetX = frame.frameData().jitterOffset().x;
        eval.jitterOffsetY = frame.frameData().jitterOffset().y;
        eval.renderSubrectDimensions.width = frame.frameData().renderWidth();
        eval.renderSubrectDimensions.height = frame.frameData().renderHeight();
        eval.motionVectorScaleX = frame.frameData().renderSize().x;
        eval.motionVectorScaleY = frame.frameData().renderSize().y;
        eval.reset = consumeHistoryReset() ? 1 : 0;
        eval.preExposure = frame.frameData().preExposure();
        eval.exposureScale = 1.0f;
        eval.frameTimeDeltaInMsec = frame.frameData().frameTimeDelta();
        int result = NgxVulkan.evaluateDLSSD(commandBuffer.getNativeCommandBuffer().address(), feature, parameters, eval);
        if (!NgxConstants.succeeded(result)) {
            SuperResolution.LOGGER.error("NGX DLSS RR evaluation failed. Result: {}", result);
            return false;
        }
        return true;
    }

    private void recreateNgxContext(int requestedRoughnessMode, int requestedDepthType) {
        if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) {
            NgxInitializer.shutdown();
            if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) {
                throw new IllegalStateException("NGX is unavailable for the current GPU");
            }
        }
        NgxParameters newParameters = new NgxParameters();
        parameters = newParameters;
        requireSuccess("NVSDK_NGX_VULKAN_GetCapabilityParameters", NgxVulkan.getCapabilityParameters(newParameters));
        NgxFeature newFeature = new NgxFeature();
        feature = newFeature;
        configurePreset(newParameters);
        NgxDLSSDCreateParams create = new NgxDLSSDCreateParams();
        create.feature.width = algorithmResources.executionTexture(InteropResourceType.Color).getWidth();
        create.feature.height = algorithmResources.executionTexture(InteropResourceType.Color).getHeight();
        create.feature.targetWidth = algorithmResources.outputTexture().getWidth();
        create.feature.targetHeight = algorithmResources.outputTexture().getHeight();
        create.featureCreateFlags =
                NgxConstants.DLSS_FLAG_MV_LOW_RES
                | NgxConstants.DLSS_FLAG_HDR
                | (initDesc.isMotionJittered() ? NgxConstants.DLSS_FLAG_MV_JITTERED : 0)
                | (initDesc.isDepthInverted() ? NgxConstants.DLSS_FLAG_DEPTH_INVERTED : 0);
        create.roughnessMode = requestedRoughnessMode;
        create.depthType = requestedDepthType;
        executeInteropCommands(commandContext -> requireSuccess(
                "NGX_VULKAN_CREATE_DLSSD", NgxVulkan.createDLSSD(
                        commandContext.nativeHandle(), 1, 1, newFeature, newParameters, create)));
    }

    private static void configurePreset(NgxParameters parameters) {
        int preset = SuperResolutionConfig.SPECIAL.DLSSRR.RENDER_PRESET.get().getCode();
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_DLAA, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_QUALITY, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_BALANCED, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_PERFORMANCE, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_ULTRA_PERFORMANCE, preset);
        parameters.setInt(NgxConstants.RAY_RECONSTRUCTION_PRESET_ULTRA_QUALITY, preset);
    }

    private void destroyNgxContext() {
        if (feature != null) {
            int result = feature.release();
            if (!NgxConstants.succeeded(result)) {
                throw new IllegalStateException("Failed to release DLSS RR feature: " + result);
            }
            feature = null;
        }
        if (parameters != null) {
            int result = parameters.destroy();
            if (!NgxConstants.succeeded(result)) {
                throw new IllegalStateException("Failed to destroy DLSS RR parameters: " + result);
            }
            parameters = null;
        }
    }

    private void destroyDispatchResources() {
        if (dispatchResources != null) {
            dispatchResources.close();
            dispatchResources = null;
        }
    }

    private static NgxResourceVK createResource(VulkanTexture texture, boolean readWrite) {
        NgxImageSubresourceRange range = new NgxImageSubresourceRange();
        range.aspectMask = texture.getAspectMask();
        range.baseMipLevel = 0;
        range.levelCount = texture.getMipmapSettings().getLevels();
        range.baseArrayLayer = 0;
        range.layerCount = 1;
        return NgxVulkan.createImageViewResourceVK(texture.getImageView(), texture.handle(), range,
                texture.getTextureFormat().vk(), texture.getWidth(), texture.getHeight(), readWrite);
    }

    private static void requireSuccess(String operation, int result) {
        if (!NgxConstants.succeeded(result)) {
            throw new IllegalStateException(operation + " failed. NGX result: " + result);
        }
    }

    private static final class NgxDispatchResources implements AutoCloseable {
        private final NgxVKDLSSDEvalParams eval = new NgxVKDLSSDEvalParams();
        private final EnumMap<InputResourceType, NgxResourceVK> resources = new EnumMap<>(InputResourceType.class);
        private final EnumMap<InputResourceType, VulkanTexture> sourceTextures = new EnumMap<>(InputResourceType.class);
        private final FloatBuffer worldToView = MemoryUtil.memAllocFloat(16);
        private final FloatBuffer viewToClip = MemoryUtil.memAllocFloat(16);
        private NgxResourceVK output;

        private NgxDispatchResources() {
            eval.worldToViewMatrix = worldToView;
            eval.viewToClipMatrix = viewToClip;
        }

        private void update(InteropDispatchResource<VulkanCommandContext> frame) {
            EnumMap<InputResourceType, VulkanTexture> presentTextures = new EnumMap<>(InputResourceType.class);
            for (InteropResourceType type : frame.resources().types()) {
                if (type.isInput()) {
                    presentTextures.put(type.inputType(), (VulkanTexture) frame.texture(type));
                }
            }
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
            if (output == null) {
                output = createResource((VulkanTexture) frame.outputTexture(), true);
            }
            eval.feature.inputColor = resources.get(InputResourceType.Color);
            eval.feature.output = output;
            eval.depth = resources.get(InputResourceType.Depth);
            eval.motionVectors = resources.get(InputResourceType.MotionVectors);
            eval.diffuseAlbedo = resources.get(InputResourceType.DiffuseAlbedo);
            eval.specularAlbedo = resources.get(InputResourceType.SpecularAlbedo);
            boolean packedRoughness = resources.containsKey(InputResourceType.NormalRoughness);
            eval.normals = packedRoughness
                    ? resources.get(InputResourceType.NormalRoughness)
                    : resources.get(InputResourceType.Normals);
            eval.roughness = packedRoughness ? null : resources.get(InputResourceType.Roughness);
            eval.motionVectorsReflections = resources.get(InputResourceType.SpecularMotionVectors);
            eval.specularHitDistance = resources.get(InputResourceType.SpecularHitDistance);
            eval.exposureTexture = resources.get(InputResourceType.Exposure);
            eval.transparencyLayer = resources.get(InputResourceType.TransparencyLayer);
            eval.transparencyLayerOpacity = resources.get(InputResourceType.TransparencyLayerOpacity);
            eval.colorBeforeTransparency = resources.get(InputResourceType.ColorBeforeTransparency);
            eval.screenSpaceSubsurfaceScatteringGuide = resources.get(InputResourceType.ScreenSpaceSubsurfaceScatteringGuide);
            eval.depthOfFieldGuide = resources.get(InputResourceType.DepthOfFieldGuide);
            worldToView.clear();
            frame.frameData().viewMatrix().get(0, worldToView);
            worldToView.limit(16);
            worldToView.position(0);
            viewToClip.clear();
            frame.frameData().projectionMatrix().get(0, viewToClip);
            viewToClip.limit(16);
            viewToClip.position(0);
        }

        @Override
        public void close() {
            for (NgxResourceVK value : resources.values()) {
                value.close();
            }
            resources.clear();
            sourceTextures.clear();
            if (output != null) {
                output.close();
                output = null;
            }
            eval.feature.inputColor = null;
            eval.feature.output = null;
            MemoryUtil.memFree(worldToView);
            MemoryUtil.memFree(viewToClip);
        }
    }
}
