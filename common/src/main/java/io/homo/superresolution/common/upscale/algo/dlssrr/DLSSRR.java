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

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.InputResourceType;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesPreprocessor;
import io.homo.superresolution.common.upscale.interoplayer.GlVulkanInteropAlgorithm;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanTimestampProfiler;
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
import java.util.IdentityHashMap;
import java.util.Map;

/** NVIDIA DLSS Ray Reconstruction implementation. */
public class DLSSRR extends GlVulkanInteropAlgorithm {
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
    private final Map<InFlightFrameResourcesSet, NgxDispatchResources> dispatchResources = new IdentityHashMap<>();
    private final Map<InFlightFrameResourcesSet, SupplementalInputs> supplementalInputs = new IdentityHashMap<>();

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
                recreateNgxContext(requestedRoughnessMode, requestedDepthType);
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

    private boolean hasRequiredInputResources(InputResourceSet resources) {
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
            recreateNgxContext(roughnessMode, depthType);
        }
    }

    @Override
    protected void onBeforeInteropResourcesDestroyed() {
        destroyDispatchResources();
        destroyNgxContext();
        destroySupplementalInputs();
    }

    @Override
    protected void processAdditionalInputResources(InFlightFrameResourcesSet inFlight, DispatchResource dispatchResource) {
        SupplementalInputs inputs = supplementalInputs.computeIfAbsent(inFlight, key -> new SupplementalInputs());
        InputResourceSet resources = dispatchResource.resources();
        inputs.present.clear();
        for (InputResourceType type : SUPPLEMENTAL_TYPES) {
            ITexture source = resources.get(type);
            if (source == null) {
                inputs.destroy(type);
                continue;
            }
            inputs.present.add(type);
            VulkanTexture existing = inputs.vkTextures.get(type);
            if (existing != null
                    && existing.getTextureFormat() == source.getTextureFormat()
                    && existing.getWidth() == source.getWidth()
                    && existing.getHeight() == source.getHeight()) {
                continue;
            }
            inputs.destroy(type);
            VulkanTexture vkTexture = RenderSystems.vulkan().device().createTextureExportable(
                    TextureDescription.create()
                            .type(source.getTextureType())
                            .usages(TextureUsages.create().sampler().storage().transferSource().transferDestination())
                            .format(source.getTextureFormat())
                            .width(source.getWidth())
                            .height(source.getHeight())
                            .label("DLSSRR-%s-%s".formatted(type, System.identityHashCode(inFlight)))
                            .build());
            inputs.vkTextures.put(type, vkTexture);
            inputs.glTextures.put(type, RenderSystems.opengl().device().createTextureImportable(vkTexture));
        }
        inputs.exposurePresent = resources.has(InputResourceType.Exposure);
        if (inputs.present.isEmpty()) {
            return;
        }
        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        try {
            commandBuffer.begin();
            for (InputResourceType type : inputs.present) {
                ITexture source = resources.get(type);
                GlImportableTexture2D destination = inputs.glTextures.get(type);
                if (type == InputResourceType.SpecularMotionVectors) {
                    InteropResourcesPreprocessor.flipMotionVectorY(commandBuffer, source, destination);
                } else {
                    InteropResourcesPreprocessor.flipY(commandBuffer, source, destination);
                }
            }
            commandBuffer.end();
            RenderSystems.current().device().submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();
        } finally {
            commandBuffer.destroy();
        }
    }

    @Override
    protected int[] getAdditionalInputSignalTextureHandles(InFlightFrameResourcesSet inFlight) {
        SupplementalInputs inputs = supplementalInputs.get(inFlight);
        if (inputs == null || inputs.present.isEmpty()) {
            return new int[0];
        }
        int[] handles = new int[inputs.present.size()];
        int index = 0;
        for (InputResourceType type : inputs.present) {
            handles[index++] = Math.toIntExact(inputs.glTextures.get(type).handle());
        }
        return handles;
    }

    @Override
    protected void dispatchVulkanUpscale(VulkanCommandBuffer commandBuffer, InFlightFrameResourcesSet frame) {
        if (feature == null || parameters == null || frame.frameData == null) {
            return;
        }
        NgxDispatchResources resources = dispatchResources.computeIfAbsent(frame, key -> new NgxDispatchResources());
        resources.update(frame, supplementalInputs.get(frame));
        NgxVKDLSSDEvalParams eval = resources.eval;
        eval.jitterOffsetX = frame.frameData.jitterOffset().x;
        eval.jitterOffsetY = frame.frameData.jitterOffset().y;
        eval.renderSubrectDimensions.width = frame.frameData.renderWidth();
        eval.renderSubrectDimensions.height = frame.frameData.renderHeight();
        eval.motionVectorScaleX = frame.frameData.renderSize().x;
        eval.motionVectorScaleY = frame.frameData.renderSize().y;
        eval.reset = consumeHistoryReset() ? 1 : 0;
        eval.preExposure = frame.frameData.preExposure();
        eval.exposureScale = 1.0f;
        eval.frameTimeDeltaInMsec = frame.frameData.frameTimeDelta();
        VulkanTimestampProfiler profiler = RenderSystems.vulkan().device().timestampProfiler();
        int timestampSlot = profiler == null
                ? -1
                : profiler.beginRegion(commandBuffer.getNativeCommandBuffer(), PerformanceTracker.VK_UPSCALE);
        int result = NgxVulkan.evaluateDLSSD(commandBuffer.getNativeCommandBuffer().address(), feature, parameters, eval);
        if (timestampSlot >= 0) {
            profiler.endRegion(commandBuffer.getNativeCommandBuffer(), timestampSlot);
        }
        if (!NgxConstants.succeeded(result)) {
            SuperResolution.LOGGER.error("NGX DLSS RR evaluation failed. Result: {}", result);
        }
    }

    private void recreateNgxContext(int requestedRoughnessMode, int requestedDepthType) {
        VulkanDevice device = RenderSystems.vulkan().device();
        if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) {
            NgxInitializer.shutdown();
            if (!NgxInitializer.initializeIfSupported(NgxConstants.FEATURE_RAY_RECONSTRUCTION)) {
                throw new IllegalStateException("NGX is unavailable for the current GPU");
            }
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
                    | (initDesc.isMotionJittered() ? NgxConstants.DLSS_FLAG_MV_JITTERED : 0);
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
            newFeature.close();
            newParameters.close();
            throw error;
        } finally {
            commandBuffer.destroy();
        }
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
                SuperResolution.LOGGER.error("Failed to release DLSS RR feature: {}", result);
            }
            feature = null;
        }
        if (parameters != null) {
            int result = parameters.destroy();
            if (!NgxConstants.succeeded(result)) {
                SuperResolution.LOGGER.error("Failed to destroy DLSS RR parameters: {}", result);
            }
            parameters = null;
        }
    }

    private void destroyDispatchResources() {
        for (NgxDispatchResources value : dispatchResources.values()) {
            value.close();
        }
        dispatchResources.clear();
    }

    private void destroySupplementalInputs() {
        for (SupplementalInputs inputs : supplementalInputs.values()) {
            inputs.destroyAll();
        }
        supplementalInputs.clear();
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

    private static final class SupplementalInputs {
        private final EnumMap<InputResourceType, GlImportableTexture2D> glTextures = new EnumMap<>(InputResourceType.class);
        private final EnumMap<InputResourceType, VulkanTexture> vkTextures = new EnumMap<>(InputResourceType.class);
        private final EnumSet<InputResourceType> present = EnumSet.noneOf(InputResourceType.class);
        private boolean exposurePresent;

        private void destroy(InputResourceType type) {
            GlImportableTexture2D gl = glTextures.remove(type);
            VulkanTexture vk = vkTextures.remove(type);
            if (gl != null) {
                gl.destroy();
            }
            if (vk != null) {
                vk.destroy();
            }
        }

        private void destroyAll() {
            for (InputResourceType type : SUPPLEMENTAL_TYPES) {
                destroy(type);
            }
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

        private void update(InFlightFrameResourcesSet frame, SupplementalInputs supplemental) {
            EnumMap<InputResourceType, VulkanTexture> presentTextures = new EnumMap<>(InputResourceType.class);
            presentTextures.put(InputResourceType.Color, frame.inputColorVkTexture);
            presentTextures.put(InputResourceType.Depth, frame.inputDepthVkTexture);
            presentTextures.put(InputResourceType.MotionVectors, frame.inputMotionVectorsVkTexture);
            if (supplemental != null) {
                if (supplemental.exposurePresent) {
                    presentTextures.put(InputResourceType.Exposure, frame.inputExposureVkTexture);
                }
                for (InputResourceType type : supplemental.present) {
                    VulkanTexture texture = supplemental.vkTextures.get(type);
                    if (texture != null) {
                        presentTextures.put(type, texture);
                    }
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
                output = createResource(frame.outputColorVkTexture, true);
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
            frame.frameData.viewMatrix().get(0, worldToView);
            worldToView.limit(16);
            worldToView.position(0);
            viewToClip.clear();
            frame.frameData.projectionMatrix().get(0, viewToClip);
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
