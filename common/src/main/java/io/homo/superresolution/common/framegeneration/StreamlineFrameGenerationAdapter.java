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
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineApiErrorListener;
import io.homo.superresolution.core.streamline.StreamlineSession;
import io.homo.superresolution.core.streamline.StreamlineTypes;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.vulkan.VK10.*;

final class StreamlineFrameGenerationAdapter {
    private static final int VIEWPORT = 0;
    private static final int BUFFER_TYPE_DEPTH = 0;
    private static final int BUFFER_TYPE_MOTION_VECTORS = 1;
    private static final int BUFFER_TYPE_HUDLESS_COLOR = 2;
    private static final int FLAG_RETAIN_RESOURCES_WHEN_OFF = 1 << 3;
    private static final int QUEUE_MODE_BLOCK_NO_CLIENT_QUEUES = 1;
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();
    private static final StreamlineApiErrorListener API_ERROR_LISTENER =
            result -> reportFailureOnce("api-" + result, "DLSS-G Vulkan API error, VkResult=" + result, null);

    private static DlssGOptionsKey lastAppliedOptions;
    private static boolean supportQueried;
    private static int maxGeneratedFrameCount;
    private static int minimumWidthOrHeight;
    private static StreamlineSession observedSession;

    private StreamlineFrameGenerationAdapter() {
    }

    static synchronized void initialize() {
        lastAppliedOptions = null;
        supportQueried = false;
        maxGeneratedFrameCount = 0;
        minimumWidthOrHeight = 0;
        observedSession = null;
        REPORTED_FAILURES.clear();
        refreshSupport();
    }

    static synchronized void shutdown() {
        disable();
        lastAppliedOptions = null;
        supportQueried = false;
        maxGeneratedFrameCount = 0;
        minimumWidthOrHeight = 0;
        observedSession = null;
        REPORTED_FAILURES.clear();
    }

    static synchronized boolean isAvailable() {
        if (!supportQueried) {
            refreshSupport();
        }
        return sessionOrNull() != null
                && maxGeneratedFrameCount > 0;
    }

    static synchronized int supportedGeneratedFrameCount() {
        if (!supportQueried) {
            refreshSupport();
        }
        return maxGeneratedFrameCount;
    }

    static synchronized int minimumWidthOrHeight() {
        if (!supportQueried) {
            refreshSupport();
        }
        return minimumWidthOrHeight;
    }

    static synchronized boolean prepareFrame(
            FrameResources frameResources,
            FGConstants constants,
            StreamlineTypes.FrameToken token,
            FrameGenerationMode mode,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer
    ) {
        StreamlineSession session = sessionOrNull();
        VulkanTexture hudlessColor = frameResources.hudlessColorVulkanTexture();
        VulkanTexture depth = frameResources.depthVulkanTexture();
        VulkanTexture motionVectors = frameResources.motionVectorVulkanTexture();
        if (session == null
                || constants == null
                || token == null
                || token.nativeHandle == 0L
                || token.frameIndex != frameResources.logicalFrameIndex()
                || hudlessColor == null
                || depth == null
                || motionVectors == null
                || hudlessColor.getWidth() != colorWidth
                || hudlessColor.getHeight() != colorHeight
                || depth.getWidth() != motionVectors.getWidth()
                || depth.getHeight() != motionVectors.getHeight()
                || colorWidth < minimumWidthOrHeight()
                || colorHeight < minimumWidthOrHeight()) {
            disable();
            return false;
        }

        int constantsResult = session.setConstants(
                toStreamlineConstants(constants),
                token,
                new StreamlineTypes.Viewport(VIEWPORT)
        );
        if (constantsResult != 0) {
            reportResultFailure("slSetConstants", constantsResult, frameResources.logicalFrameIndex());
            disable();
            return false;
        }

        StreamlineTypes.ResourceTag[] tags = {
                createTag(depth, BUFFER_TYPE_DEPTH),
                createTag(motionVectors, BUFFER_TYPE_MOTION_VECTORS),
                createTag(hudlessColor, BUFFER_TYPE_HUDLESS_COLOR)
        };
        int tagResult = session.setTagForFrame(
                token,
                new StreamlineTypes.Viewport(VIEWPORT),
                tags,
                commandBuffer
        );
        if (tagResult != 0) {
            reportResultFailure("slSetTagForFrame", tagResult, frameResources.logicalFrameIndex());
            disable();
            return false;
        }

        boolean applied = applyOptions(new DlssGOptionsKey(
                mode.nativeMode(),
                Math.max(1, mode.generatedFrameCount()),
                backBufferCount,
                depth.getWidth(),
                depth.getHeight(),
                colorWidth,
                colorHeight,
                colorFormat,
                motionVectors.getTextureFormat().vk(),
                depth.getTextureFormat().vk(),
                hudlessColor.getTextureFormat().vk()
        ));
        if (!applied) {
            disable();
        }
        return applied;
    }

    static synchronized void finishPresent(FrameResources frameResources, boolean frameGenerationEnabled) {
        if (!frameGenerationEnabled || frameResources == null || !frameResources.isSubmitted()) {
            return;
        }
        StreamlineSession session = sessionOrNull();
        if (session == null) {
            reportFailureOnce(
                    "finish-present-session",
                    "Streamline session became unavailable after a DLSS-G present",
                    null
            );
            return;
        }

        StreamlineTypes.DlssGState state = new StreamlineTypes.DlssGState();
        int result;
        try {
            result = session.dlssGGetState(
                    new StreamlineTypes.Viewport(VIEWPORT),
                    state,
                    null
            );
        } catch (Throwable throwable) {
            reportFailureOnce("finish-present-exception", "slDLSSGGetState threw an exception", throwable);
            return;
        }
        if (result != 0) {
            reportResultFailure("slDLSSGGetState", result, frameResources.logicalFrameIndex());
            return;
        }

        updateSupport(state);
        if (state.inputsProcessingCompletionFence != 0L
                && state.lastPresentInputsProcessingCompletionFenceValue > 0L) {
            frameResources.setDlssGInputCompletion(
                    state.inputsProcessingCompletionFence,
                    state.lastPresentInputsProcessingCompletionFenceValue
            );
        } else {
            frameResources.clearDlssGInputCompletion();
        }
    }

    static synchronized void disable() {
        applyOptions(DlssGOptionsKey.off());
    }

    private static synchronized void refreshSupport() {
        maxGeneratedFrameCount = 0;
        minimumWidthOrHeight = 0;
        StreamlineSession session = sessionOrNull();
        supportQueried = true;
        if (session == null) {
            return;
        }
        try {
            StreamlineTypes.DlssGState state = new StreamlineTypes.DlssGState();
            int result = session.dlssGGetState(
                    new StreamlineTypes.Viewport(VIEWPORT),
                    state,
                    null
            );
            if (result == 0) {
                updateSupport(state);
            } else {
                reportFailureOnce(
                        "support-" + result,
                        "slDLSSGGetState support query failed with result=" + result,
                        null
                );
            }
        } catch (Throwable throwable) {
            reportFailureOnce("support-exception", "Failed to query DLSS-G support", throwable);
        }
    }

    private static void updateSupport(StreamlineTypes.DlssGState state) {
        maxGeneratedFrameCount = Math.max(0, state.numFramesToGenerateMax);
        minimumWidthOrHeight = Math.max(0, state.minWidthOrHeight);
    }

    private static boolean applyOptions(DlssGOptionsKey desired) {
        StreamlineSession session = sessionOrNull();
        if (session == null) {
            lastAppliedOptions = null;
            return desired.mode() == StreamlineTypes.DlssGMode.OFF;
        }
        if (desired.equals(lastAppliedOptions)) {
            return true;
        }

        StreamlineTypes.DlssGOptions options = new StreamlineTypes.DlssGOptions();
        options.mode = desired.mode();
        options.numFramesToGenerate = desired.numFramesToGenerate();
        options.numBackBuffers = desired.numBackBuffers();
        options.motionVectorDepthWidth = desired.motionVectorDepthWidth();
        options.motionVectorDepthHeight = desired.motionVectorDepthHeight();
        options.colorWidth = desired.colorWidth();
        options.colorHeight = desired.colorHeight();
        options.colorBufferFormat = desired.colorBufferFormat();
        options.motionVectorBufferFormat = desired.motionVectorBufferFormat();
        options.depthBufferFormat = desired.depthBufferFormat();
        options.hudLessBufferFormat = desired.hudlessBufferFormat();
        options.flags = FLAG_RETAIN_RESOURCES_WHEN_OFF;
        options.queueParallelismMode = QUEUE_MODE_BLOCK_NO_CLIENT_QUEUES;
        options.enableUserInterfaceRecomposition = 0;
        options.onApiError = API_ERROR_LISTENER;

        int result;
        try {
            result = session.dlssGSetOptions(new StreamlineTypes.Viewport(VIEWPORT), options);
        } catch (Throwable throwable) {
            reportFailureOnce("options-exception", "slDLSSGSetOptions threw an exception", throwable);
            return false;
        }
        if (result != 0) {
            reportFailureOnce(
                    "options-" + result,
                    "slDLSSGSetOptions failed with result=" + result,
                    null
            );
            return false;
        }
        lastAppliedOptions = desired;
        return true;
    }

    private static synchronized StreamlineSession sessionOrNull() {
        if (!Streamline.isInitialized()) {
            observedSession = null;
            lastAppliedOptions = null;
            supportQueried = false;
            return null;
        }
        StreamlineSession session = Streamline.session();
        if (session == null || session.isClosed()) {
            observedSession = null;
            lastAppliedOptions = null;
            supportQueried = false;
            return null;
        }
        if (observedSession != session) {
            observedSession = session;
            lastAppliedOptions = null;
            supportQueried = false;
        }
        return session;
    }

    private static StreamlineTypes.ResourceTag createTag(VulkanTexture texture, int bufferType) {
        StreamlineTypes.ResourceTag tag = new StreamlineTypes.ResourceTag();
        tag.resource = createResource(texture);
        tag.type = bufferType;
        tag.lifecycle = StreamlineTypes.ResourceLifecycle.VALID_UNTIL_PRESENT;
        tag.extent = new StreamlineTypes.Extent();
        tag.extent.width = texture.getWidth();
        tag.extent.height = texture.getHeight();
        return tag;
    }

    private static StreamlineTypes.Resource createResource(VulkanTexture texture) {
        StreamlineTypes.Resource resource = new StreamlineTypes.Resource();
        resource.type = StreamlineTypes.ResourceType.TEX_2D;
        resource.nativeHandle = texture.handle();
        resource.memory = texture.getImageMemory();
        resource.view = texture.getImageView();
        resource.state = texture.getCurrentLayout();
        resource.width = texture.getWidth();
        resource.height = texture.getHeight();
        resource.nativeFormat = texture.getTextureFormat().vk();
        resource.mipLevels = texture.getMipmapSettings().getLevels();
        resource.arrayLayers = 1;
        resource.usage = toVulkanImageUsage(texture);
        return resource;
    }

    private static int toVulkanImageUsage(VulkanTexture texture) {
        int usage = 0;
        for (TextureUsage textureUsage : texture.getTextureUsages().getUsages()) {
            switch (textureUsage) {
                case Sampler -> usage |= VK_IMAGE_USAGE_SAMPLED_BIT;
                case Storage -> usage |= VK_IMAGE_USAGE_STORAGE_BIT;
                case AttachmentColor -> usage |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                case AttachmentDepth -> usage |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
                case TransferSource -> usage |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
                case TransferDestination -> usage |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
            }
        }
        return usage;
    }

    private static StreamlineTypes.Constants toStreamlineConstants(FGConstants source) {
        StreamlineTypes.Constants constants = new StreamlineTypes.Constants();
        constants.cameraViewToClip = source.cameraViewToClip();
        constants.clipToCameraView = source.clipToCameraView();
        constants.clipToLensClip = source.clipToLensClip();
        constants.clipToPrevClip = source.clipToPrevClip();
        constants.prevClipToClip = source.prevClipToClip();
        constants.jitterOffsetX = source.jitterOffsetX();
        constants.jitterOffsetY = source.jitterOffsetY();
        constants.motionVectorScaleX = source.motionVectorScaleX();
        constants.motionVectorScaleY = source.motionVectorScaleY();
        constants.cameraPinholeOffsetX = source.cameraPinholeOffsetX();
        constants.cameraPinholeOffsetY = source.cameraPinholeOffsetY();
        constants.cameraPosX = source.cameraPosX();
        constants.cameraPosY = source.cameraPosY();
        constants.cameraPosZ = source.cameraPosZ();
        constants.cameraUpX = source.cameraUpX();
        constants.cameraUpY = source.cameraUpY();
        constants.cameraUpZ = source.cameraUpZ();
        constants.cameraRightX = source.cameraRightX();
        constants.cameraRightY = source.cameraRightY();
        constants.cameraRightZ = source.cameraRightZ();
        constants.cameraFwdX = source.cameraFwdX();
        constants.cameraFwdY = source.cameraFwdY();
        constants.cameraFwdZ = source.cameraFwdZ();
        constants.cameraNear = source.cameraNear();
        constants.cameraFar = source.cameraFar();
        constants.cameraFov = source.cameraFov();
        constants.cameraAspectRatio = source.cameraAspectRatio();
        constants.motionVectorsInvalidValue = source.motionVectorsInvalidValue();
        constants.depthInverted = source.depthInverted();
        constants.cameraMotionIncluded = source.cameraMotionIncluded();
        constants.motionVectors3D = source.motionVectors3D();
        constants.reset = source.reset();
        constants.orthographicProjection = source.orthographicProjection();
        constants.motionVectorsDilated = source.motionVectorsDilated();
        constants.motionVectorsJittered = source.motionVectorsJittered();
        constants.minRelativeLinearDepthObjectSeparation =
                source.minRelativeLinearDepthObjectSeparation();
        return constants;
    }

    private static void reportResultFailure(String operation, int result, int frameIndex) {
        reportFailureOnce(
                operation + "-" + result,
                operation + " failed for frame " + frameIndex + " with result=" + result,
                null
        );
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

    record DlssGOptionsKey(
            int mode,

            int numFramesToGenerate,

            int numBackBuffers,

            int motionVectorDepthWidth,

            int motionVectorDepthHeight,

            int colorWidth,

            int colorHeight,

            int colorBufferFormat,

            int motionVectorBufferFormat,

            int depthBufferFormat,

            int hudlessBufferFormat
    ) {
        static DlssGOptionsKey off() {
            return new DlssGOptionsKey(
                    StreamlineTypes.DlssGMode.OFF,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
