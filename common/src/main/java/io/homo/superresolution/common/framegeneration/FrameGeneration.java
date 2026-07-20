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

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.lowlatency.LowLatencyMode;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineTypes;

import java.util.ArrayList;
import java.util.List;

public final class FrameGeneration {
    private static boolean initialized;

    private FrameGeneration() {
    }

    public static synchronized void initialize() {
        if (initialized || !VulkanPresentationFeature.isRequested()) {
            return;
        }
        FGConstantsFeature.initialize();
        FGConstantsFeature.register();
        StreamlineFrameGenerationAdapter.initialize();
        initialized = true;
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        StreamlineFrameGenerationAdapter.shutdown();
        FGConstantsFeature.shutdown();
        initialized = false;
    }

    public static synchronized boolean prepareFrame(
            FrameResources frameResources,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer
    ) {
        FrameGenerationMode mode = displayedMode();
        if (!initialized
                || !mode.isEnabled()
                || frameResources == null
                || commandBuffer == 0L
                || !frameResources.hasHudlessColor()
                || !frameResources.hasDepth()
                || !frameResources.hasMotionVector()) {
            StreamlineFrameGenerationAdapter.disable();
            return false;
        }

        StreamlineTypes.FrameToken token = Streamline.currentFrame();
        FGConstants constants = FGConstantsFeature.getConstants(frameResources.logicalFrameIndex());
        if (token == null
                || token.nativeHandle == 0L
                || token.frameIndex != frameResources.logicalFrameIndex()
                || constants == null) {
            StreamlineFrameGenerationAdapter.disable();
            return false;
        }

        return StreamlineFrameGenerationAdapter.prepareFrame(
                frameResources,
                constants,
                token,
                mode,
                colorWidth,
                colorHeight,
                colorFormat,
                backBufferCount,
                commandBuffer
        );
    }

    public static synchronized void finishPresent(
            FrameResources frameResources,
            boolean frameGenerationEnabled
    ) {
        StreamlineFrameGenerationAdapter.finishPresent(frameResources, frameGenerationEnabled);
    }

    public static synchronized void disableFrameGeneration() {
        StreamlineFrameGenerationAdapter.disable();
    }

    public static void invalidateHistory() {
        FGConstantsFeature.invalidateHistory();
    }

    public static boolean isSupported() {
        return dependenciesSatisfied() && StreamlineFrameGenerationAdapter.isAvailable();
    }

    public static boolean isFrameGenerationEnabled() {
        return displayedMode().isEnabled();
    }

    public static void setFrameGenerationMode(FrameGenerationMode mode) {
        FrameGenerationMode selected = mode;
        if (selected == null || !isModeSupported(selected)) {
            selected = FrameGenerationMode.OFF;
        }
        SuperResolutionConfig.setFrameGenerationMode(selected);
        if (!selected.isEnabled()) {
            disableFrameGeneration();
        }
    }

    public static FrameGenerationMode displayedMode() {
        FrameGenerationMode mode = SuperResolutionConfig.getFrameGenerationMode();
        return isModeSupported(mode) ? mode : FrameGenerationMode.OFF;
    }

    public static FrameGenerationMode[] availableModes() {
        if (!dependenciesSatisfied() || !StreamlineFrameGenerationAdapter.isAvailable()) {
            return new FrameGenerationMode[]{FrameGenerationMode.OFF};
        }
        return availableModesForMaximum(StreamlineFrameGenerationAdapter.supportedGeneratedFrameCount());
    }

    static FrameGenerationMode[] availableModesForMaximum(int maximumGeneratedFrames) {
        List<FrameGenerationMode> modes = new ArrayList<>();
        modes.add(FrameGenerationMode.OFF);
        for (FrameGenerationMode mode : FrameGenerationMode.values()) {
            if (mode != FrameGenerationMode.OFF
                    && mode.generatedFrameCount() <= maximumGeneratedFrames) {
                modes.add(mode);
            }
        }
        return modes.toArray(FrameGenerationMode[]::new);
    }

    static boolean dependenciesSatisfied() {
        return SuperResolutionConfig.isEnableVulkanPresentation()
                && SuperResolutionConfig.getLowLatencyMode() == LowLatencyMode.NVReflex
                && SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF
                && SuperResolutionConfig.getInteropSyncMode() == InteropSyncMode.LowLatency;
    }

    private static boolean isModeSupported(FrameGenerationMode mode) {
        if (mode == null || mode == FrameGenerationMode.OFF) {
            return mode == FrameGenerationMode.OFF;
        }
        return isSupported()
                && mode.generatedFrameCount()
                <= StreamlineFrameGenerationAdapter.supportedGeneratedFrameCount();
    }
}
