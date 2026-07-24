/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.ngx.NgxConstants;
import io.homo.superresolution.core.ngx.NgxDLSSFGCreateParams;
import io.homo.superresolution.core.ngx.NgxDLSSFGOptEvalParams;
import io.homo.superresolution.core.ngx.NgxFeature;
import io.homo.superresolution.core.ngx.NgxImageSubresourceRange;
import io.homo.superresolution.core.ngx.NgxInitializer;
import io.homo.superresolution.core.ngx.NgxParameters;
import io.homo.superresolution.core.ngx.NgxResourceVK;
import io.homo.superresolution.core.ngx.NgxVKDLSSFGEvalParams;
import io.homo.superresolution.core.ngx.NgxVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.vulkan.VK10.*;

final class NgxFrameGenerationAdapter {
    private static final int MIN_WIDTH_OR_HEIGHT = 128;
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();

    private static boolean supportQueried;
    private static int maxGeneratedFrameCount;
    private static NgxParameters ngxParameters;
    private static NgxFeature ngxFeature;
    private static FeatureKey featureKey;
    private static final List<VulkanTexture> interpolatedTextures = new ArrayList<>();
    private static final NgxDLSSFGOptEvalParams optEvalParams = new NgxDLSSFGOptEvalParams();
    private static FloatBuffer cameraViewToClip;
    private static FloatBuffer clipToCameraView;
    private static FloatBuffer clipToLensClip;
    private static FloatBuffer clipToPrevClip;
    private static FloatBuffer prevClipToClip;

    private NgxFrameGenerationAdapter() {
    }

    static synchronized void initialize() {
        // Support is queried lazily so the NGX runtime is only initialized when this
        // backend is actually selected (on Windows Streamline usually is).
        supportQueried = false;
        maxGeneratedFrameCount = 0;
        REPORTED_FAILURES.clear();
    }

    static synchronized void shutdown() {
        releaseFeature(true);
        freeMatrixBuffers();
        supportQueried = false;
        maxGeneratedFrameCount = 0;
        REPORTED_FAILURES.clear();
    }

    static synchronized boolean isAvailable() {
        if (!supportQueried) {
            refreshSupport();
        }
        return maxGeneratedFrameCount > 0;
    }

    static synchronized int supportedGeneratedFrameCount() {
        if (!supportQueried) {
            refreshSupport();
        }
        return maxGeneratedFrameCount;
    }

    static synchronized int minimumWidthOrHeight() {
        return MIN_WIDTH_OR_HEIGHT;
    }

    /**
     * Records DLSS-G evaluation into the presentation command buffer and returns the
     * interpolated frames to present before the real frame, or null when frame
     * generation cannot run for this frame.
     */
    static synchronized List<VulkanTexture> prepareFrame(
            FrameResources frameResources,
            FGConstants constants,
            FrameGenerationMode mode,
            int colorWidth,
            int colorHeight,
            long commandBuffer
    ) {
        VulkanTexture backbuffer = frameResources.finalColorVulkanTexture();
        VulkanTexture hudless = frameResources.hudlessColorVulkanTexture();
        VulkanTexture depth = frameResources.depthVulkanTexture();
        VulkanTexture motionVectors = frameResources.motionVectorVulkanTexture();
        if (!isAvailable()
                || constants == null
                || backbuffer == null
                || hudless == null
                || depth == null
                || motionVectors == null
                || backbuffer.getWidth() != colorWidth
                || backbuffer.getHeight() != colorHeight
                || hudless.getWidth() != colorWidth
                || hudless.getHeight() != colorHeight
                || depth.getWidth() != motionVectors.getWidth()
                || depth.getHeight() != motionVectors.getHeight()
                || colorWidth < MIN_WIDTH_OR_HEIGHT
                || colorHeight < MIN_WIDTH_OR_HEIGHT) {
            return null;
        }

        int generatedFrameCount = Math.min(
                Math.max(1, mode.generatedFrameCount()),
                supportedGeneratedFrameCount()
        );

        try {
            ensureFeature(backbuffer, depth);
            ensureInterpolatedTextures(generatedFrameCount, backbuffer);
        } catch (RuntimeException | Error e) {
            reportFailureOnce("feature-create", "Failed to create the NGX DLSS-G feature", e);
            releaseFeature(false);
            return null;
        }

        recordTransitionsToGeneral(commandBuffer, backbuffer, hudless, depth, motionVectors, generatedFrameCount);
        fillOptEvalParams(constants, generatedFrameCount);

        for (int frameIndex = 1; frameIndex <= generatedFrameCount; frameIndex++) {
            optEvalParams.multiFrameCount = generatedFrameCount;
            optEvalParams.multiFrameIndex = frameIndex;
            VulkanTexture output = interpolatedTextures.get(frameIndex - 1);
            int result;
            try (
                    NgxResourceVK backbufferResource = createResource(backbuffer, false);
                    NgxResourceVK depthResource = createResource(depth, false);
                    NgxResourceVK motionVectorsResource = createResource(motionVectors, false);
                    NgxResourceVK hudlessResource = createResource(hudless, false);
                    NgxResourceVK outputResource = createResource(output, true)
            ) {
                NgxVKDLSSFGEvalParams evalParams = new NgxVKDLSSFGEvalParams();
                evalParams.backbuffer = backbufferResource;
                evalParams.depth = depthResource;
                evalParams.motionVectors = motionVectorsResource;
                evalParams.hudless = hudlessResource;
                evalParams.outputInterpolatedFrame = outputResource;
                result = NgxVulkan.evaluateDLSSFG(
                        commandBuffer,
                        ngxFeature,
                        ngxParameters,
                        evalParams,
                        optEvalParams
                );
            }
            if (!NgxConstants.succeeded(result)) {
                reportFailureOnce(
                        "evaluate-" + result,
                        "NGX DLSS-G evaluation failed for frame "
                                + frameResources.logicalFrameIndex() + " with result=" + result,
                        null
                );
                return null;
            }
            output.setCurrentLayout(VK_IMAGE_LAYOUT_GENERAL);
        }

        return List.copyOf(interpolatedTextures.subList(0, generatedFrameCount));
    }

    static synchronized void disable() {
        // The feature stays resident so toggling FG (menus, screenshots) does not
        // pay the create hitch; VRAM is reclaimed on shutdown or size change.
    }

    private static synchronized void refreshSupport() {
        supportQueried = true;
        maxGeneratedFrameCount = 0;
        if (!NgxInitializer.initializeIfSupported()) {
            return;
        }
        NgxParameters capabilities = new NgxParameters();
        int capabilitiesResult = NgxVulkan.getCapabilityParameters(capabilities);
        if (!NgxConstants.succeeded(capabilitiesResult)) {
            reportFailureOnce(
                    "capability-" + capabilitiesResult,
                    "NVSDK_NGX_VULKAN_GetCapabilityParameters failed with result=" + capabilitiesResult,
                    null
            );
            return;
        }
        try {
            int[] available = new int[1];
            int availableResult = capabilities.getInt(NgxConstants.FRAME_GENERATION_AVAILABLE, available);
            if (!NgxConstants.succeeded(availableResult) || available[0] == 0) {
                logUnavailableReason(capabilities);
                return;
            }
            long[] multiFrameCountMax = new long[1];
            int multiFrameResult = capabilities.getUnsignedInt(
                    NgxConstants.DLSSFG_MULTI_FRAME_COUNT_MAX,
                    multiFrameCountMax
            );
            maxGeneratedFrameCount = NgxConstants.succeeded(multiFrameResult) && multiFrameCountMax[0] > 1
                    ? (int) multiFrameCountMax[0]
                    : 1;
            SuperResolution.LOGGER.info(
                    "NGX DLSS-G is available, maximum generated frames per rendered frame: {}",
                    maxGeneratedFrameCount
            );
        } finally {
            capabilities.destroy();
        }
    }

    private static void logUnavailableReason(NgxParameters capabilities) {
        int[] initResult = new int[1];
        int[] needsDriver = new int[1];
        long[] driverMajor = new long[1];
        long[] driverMinor = new long[1];
        capabilities.getInt(NgxConstants.FRAME_GENERATION_FEATURE_INIT_RESULT, initResult);
        capabilities.getInt(NgxConstants.FRAME_GENERATION_NEEDS_UPDATED_DRIVER, needsDriver);
        capabilities.getUnsignedInt(NgxConstants.FRAME_GENERATION_MIN_DRIVER_VERSION_MAJOR, driverMajor);
        capabilities.getUnsignedInt(NgxConstants.FRAME_GENERATION_MIN_DRIVER_VERSION_MINOR, driverMinor);
        reportFailureOnce(
                "unavailable",
                "NGX DLSS-G is not available on this system. FeatureInitResult=" + initResult[0]
                        + ", needsUpdatedDriver=" + needsDriver[0]
                        + ", minimum driver=" + driverMajor[0] + "." + driverMinor[0],
                null
        );
    }

    private static void ensureFeature(VulkanTexture backbuffer, VulkanTexture depth) {
        FeatureKey desired = new FeatureKey(
                backbuffer.getWidth(),
                backbuffer.getHeight(),
                backbuffer.getTextureFormat().vk(),
                depth.getWidth(),
                depth.getHeight()
        );
        if (ngxFeature != null && ngxFeature.isValid() && desired.equals(featureKey)) {
            return;
        }
        releaseFeature(false);

        VulkanDevice device = RenderSystems.vulkan().device();
        NgxParameters parameters = new NgxParameters();
        int parametersResult = NgxVulkan.getCapabilityParameters(parameters);
        requireNgxSuccess("NVSDK_NGX_VULKAN_GetCapabilityParameters", parametersResult);

        NgxFeature feature = new NgxFeature();
        VulkanCommandBuffer commandBuffer = device.createCommandBuffer();
        try {
            NgxDLSSFGCreateParams createParams = new NgxDLSSFGCreateParams();
            createParams.width = desired.colorWidth();
            createParams.height = desired.colorHeight();
            createParams.nativeBackbufferFormat = desired.backbufferFormat();
            createParams.renderWidth = desired.renderWidth();
            createParams.renderHeight = desired.renderHeight();
            createParams.dynamicResolutionScaling = false;

            commandBuffer.begin();
            int createResult = NgxVulkan.createDLSSFG(
                    commandBuffer.getNativeCommandBuffer().address(),
                    1,
                    1,
                    feature,
                    parameters,
                    createParams
            );
            commandBuffer.end();
            requireNgxSuccess("NGX_VK_CREATE_DLSSG", createResult);

            device.submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();

            ngxParameters = parameters;
            ngxFeature = feature;
            featureKey = desired;
            SuperResolution.LOGGER.info(
                    "Created NGX DLSS-G feature: color {}x{} (VkFormat {}), render {}x{}",
                    desired.colorWidth(),
                    desired.colorHeight(),
                    desired.backbufferFormat(),
                    desired.renderWidth(),
                    desired.renderHeight()
            );
        } catch (RuntimeException | Error e) {
            feature.close();
            parameters.close();
            throw e;
        } finally {
            commandBuffer.destroy();
        }
    }

    private static synchronized void releaseFeature(boolean quiet) {
        if (ngxFeature == null && ngxParameters == null && interpolatedTextures.isEmpty()) {
            featureKey = null;
            return;
        }
        try {
            VulkanDevice device = RenderSystems.vulkan() != null ? RenderSystems.vulkan().device() : null;
            if (device != null) {
                device.getMainQueue().waitIdle();
            }
        } catch (Throwable throwable) {
            if (!quiet) {
                SuperResolution.LOGGER.warn("Failed to drain the queue before releasing DLSS-G", throwable);
            }
        }
        if (ngxFeature != null) {
            int result = ngxFeature.release();
            if (!NgxConstants.succeeded(result) && !quiet) {
                SuperResolution.LOGGER.warn("Failed to release the NGX DLSS-G feature. Result: {}", result);
            }
            ngxFeature = null;
        }
        if (ngxParameters != null) {
            int result = ngxParameters.destroy();
            if (!NgxConstants.succeeded(result) && !quiet) {
                SuperResolution.LOGGER.warn("Failed to destroy the NGX DLSS-G parameters. Result: {}", result);
            }
            ngxParameters = null;
        }
        for (VulkanTexture texture : interpolatedTextures) {
            texture.destroy();
        }
        interpolatedTextures.clear();
        featureKey = null;
    }

    private static void ensureInterpolatedTextures(int count, VulkanTexture backbuffer) {
        boolean matches = interpolatedTextures.size() >= count;
        if (matches) {
            VulkanTexture first = interpolatedTextures.get(0);
            matches = first.getWidth() == backbuffer.getWidth()
                    && first.getHeight() == backbuffer.getHeight()
                    && first.getTextureFormat() == backbuffer.getTextureFormat();
        }
        if (matches) {
            return;
        }
        VulkanDevice device = RenderSystems.vulkan().device();
        device.getMainQueue().waitIdle();
        for (VulkanTexture texture : interpolatedTextures) {
            texture.destroy();
        }
        interpolatedTextures.clear();
        for (int i = 0; i < count; i++) {
            TextureDescription description = TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .format(backbuffer.getTextureFormat())
                    .size(backbuffer.getWidth(), backbuffer.getHeight())
                    .usages(TextureUsages.create()
                            .sampler()
                            .storage()
                            .transferSource()
                            .transferDestination())
                    .label("SRDlssGInterpolated-" + i)
                    .build();
            interpolatedTextures.add((VulkanTexture) device.createTexture(description));
        }
    }

    private static void recordTransitionsToGeneral(
            long commandBuffer,
            VulkanTexture backbuffer,
            VulkanTexture hudless,
            VulkanTexture depth,
            VulkanTexture motionVectors,
            int generatedFrameCount
    ) {
        List<VulkanTexture> textures = new ArrayList<>();
        addIfNotGeneral(textures, backbuffer);
        addIfNotGeneral(textures, hudless);
        addIfNotGeneral(textures, depth);
        addIfNotGeneral(textures, motionVectors);
        for (int i = 0; i < generatedFrameCount; i++) {
            addIfNotGeneral(textures, interpolatedTextures.get(i));
        }
        if (textures.isEmpty()) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barriers = VkImageMemoryBarrier.calloc(textures.size(), stack);
            for (int i = 0; i < textures.size(); i++) {
                VulkanTexture texture = textures.get(i);
                int oldLayout = texture.getCurrentLayout();
                barriers.get(i)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .srcAccessMask(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED
                                ? 0
                                : VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT)
                        .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT)
                        .oldLayout(oldLayout)
                        .newLayout(VK_IMAGE_LAYOUT_GENERAL)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(texture.handle())
                        .subresourceRange(range -> range
                                .aspectMask(texture.getAspectMask())
                                .baseMipLevel(0)
                                .levelCount(texture.getMipmapSettings().getLevels())
                                .baseArrayLayer(0)
                                .layerCount(1));
            }
            vkCmdPipelineBarrier(
                    new VkCommandBuffer(commandBuffer, RenderSystems.vulkan().device().getVkDevice()),
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    0,
                    null,
                    null,
                    barriers
            );
        }
        for (VulkanTexture texture : textures) {
            texture.setCurrentLayout(VK_IMAGE_LAYOUT_GENERAL);
        }
    }

    private static void addIfNotGeneral(List<VulkanTexture> textures, VulkanTexture texture) {
        if (texture.getCurrentLayout() != VK_IMAGE_LAYOUT_GENERAL) {
            textures.add(texture);
        }
    }

    private static void fillOptEvalParams(FGConstants constants, int generatedFrameCount) {
        ensureMatrixBuffers();
        putMatrix(cameraViewToClip, constants.cameraViewToClip());
        putMatrix(clipToCameraView, constants.clipToCameraView());
        putMatrix(clipToLensClip, constants.clipToLensClip());
        putMatrix(clipToPrevClip, constants.clipToPrevClip());
        putMatrix(prevClipToClip, constants.prevClipToClip());
        optEvalParams.cameraViewToClip = cameraViewToClip;
        optEvalParams.clipToCameraView = clipToCameraView;
        optEvalParams.clipToLensClip = clipToLensClip;
        optEvalParams.clipToPrevClip = clipToPrevClip;
        optEvalParams.prevClipToClip = prevClipToClip;
        optEvalParams.jitterOffset[0] = constants.jitterOffsetX();
        optEvalParams.jitterOffset[1] = constants.jitterOffsetY();
        optEvalParams.motionVectorScale[0] = constants.motionVectorScaleX();
        optEvalParams.motionVectorScale[1] = constants.motionVectorScaleY();
        optEvalParams.cameraPinholeOffset[0] = constants.cameraPinholeOffsetX();
        optEvalParams.cameraPinholeOffset[1] = constants.cameraPinholeOffsetY();
        optEvalParams.cameraPosition[0] = constants.cameraPosX();
        optEvalParams.cameraPosition[1] = constants.cameraPosY();
        optEvalParams.cameraPosition[2] = constants.cameraPosZ();
        optEvalParams.cameraUp[0] = constants.cameraUpX();
        optEvalParams.cameraUp[1] = constants.cameraUpY();
        optEvalParams.cameraUp[2] = constants.cameraUpZ();
        optEvalParams.cameraRight[0] = constants.cameraRightX();
        optEvalParams.cameraRight[1] = constants.cameraRightY();
        optEvalParams.cameraRight[2] = constants.cameraRightZ();
        optEvalParams.cameraForward[0] = constants.cameraFwdX();
        optEvalParams.cameraForward[1] = constants.cameraFwdY();
        optEvalParams.cameraForward[2] = constants.cameraFwdZ();
        optEvalParams.cameraNear = constants.cameraNear();
        optEvalParams.cameraFar = constants.cameraFar();
        optEvalParams.cameraFov = constants.cameraFov();
        optEvalParams.cameraAspectRatio = constants.cameraAspectRatio();
        optEvalParams.colorBuffersHdr = false;
        optEvalParams.depthInverted = constants.depthInverted() != 0;
        optEvalParams.cameraMotionIncluded = constants.cameraMotionIncluded() != 0;
        optEvalParams.reset = constants.reset() != 0;
        optEvalParams.automodeOverrideReset = false;
        optEvalParams.notRenderingGameFrames = false;
        optEvalParams.orthoProjection = constants.orthographicProjection() != 0;
        optEvalParams.motionVectorsInvalidValue = constants.motionVectorsInvalidValue();
        optEvalParams.motionVectorsDilated = constants.motionVectorsDilated() != 0;
        optEvalParams.motionVectorsJittered = constants.motionVectorsJittered() != 0;
        optEvalParams.menuDetectionEnabled = false;
        optEvalParams.minRelativeLinearDepthObjectSeparation =
                constants.minRelativeLinearDepthObjectSeparation();
        optEvalParams.multiFrameCount = generatedFrameCount;
        optEvalParams.multiFrameIndex = 1;
    }

    private static NgxResourceVK createResource(VulkanTexture texture, boolean readWrite) {
        NgxImageSubresourceRange subresourceRange = new NgxImageSubresourceRange();
        subresourceRange.aspectMask = texture.getAspectMask();
        subresourceRange.baseMipLevel = 0;
        subresourceRange.levelCount = texture.getMipmapSettings().getLevels();
        subresourceRange.baseArrayLayer = 0;
        subresourceRange.layerCount = 1;
        return NgxVulkan.createImageViewResourceVK(
                texture.getImageView(),
                texture.handle(),
                subresourceRange,
                texture.getTextureFormat().vk(),
                texture.getWidth(),
                texture.getHeight(),
                readWrite
        );
    }

    private static void ensureMatrixBuffers() {
        if (cameraViewToClip == null) {
            cameraViewToClip = MemoryUtil.memAllocFloat(16);
            clipToCameraView = MemoryUtil.memAllocFloat(16);
            clipToLensClip = MemoryUtil.memAllocFloat(16);
            clipToPrevClip = MemoryUtil.memAllocFloat(16);
            prevClipToClip = MemoryUtil.memAllocFloat(16);
        }
    }

    private static void freeMatrixBuffers() {
        if (cameraViewToClip != null) {
            MemoryUtil.memFree(cameraViewToClip);
            MemoryUtil.memFree(clipToCameraView);
            MemoryUtil.memFree(clipToLensClip);
            MemoryUtil.memFree(clipToPrevClip);
            MemoryUtil.memFree(prevClipToClip);
            cameraViewToClip = null;
            clipToCameraView = null;
            clipToLensClip = null;
            clipToPrevClip = null;
            prevClipToClip = null;
        }
    }

    private static void putMatrix(FloatBuffer buffer, float[] values) {
        buffer.clear();
        buffer.put(values);
        buffer.flip();
    }

    private static void requireNgxSuccess(String operation, int result) {
        if (!NgxConstants.succeeded(result)) {
            throw new IllegalStateException(operation + " failed. NGX result: " + result);
        }
    }

    private static void reportFailureOnce(String key, String message, Throwable throwable) {
        if (!REPORTED_FAILURES.add(key)) {
            return;
        }
        if (throwable == null) {
            SuperResolution.LOGGER.warn(message);
        } else {
            SuperResolution.LOGGER.warn(message, throwable);
        }
    }

    private record FeatureKey(
            int colorWidth,

            int colorHeight,

            int backbufferFormat,

            int renderWidth,

            int renderHeight
    ) {
    }
}
