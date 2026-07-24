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
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeState;
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
        NgxFrameGenerationAdapter.initialize();
        initialized = true;
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        StreamlineFrameGenerationAdapter.shutdown();
        NgxFrameGenerationAdapter.shutdown();
        FGConstantsFeature.shutdown();
        initialized = false;
    }

    public static synchronized FramePresentPlan prepareFrame(
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
            disableFrameGeneration();
            return FramePresentPlan.none();
        }

        FGConstants constants = FGConstantsFeature.getConstants(frameResources.logicalFrameIndex());
        if (constants == null) {
            disableFrameGeneration();
            return FramePresentPlan.none();
        }

        FrameGenerationBackend backend = activeBackend();
        if (backend == FrameGenerationBackend.STREAMLINE) {
            StreamlineTypes.FrameToken token = Streamline.currentFrame();
            if (token == null
                    || token.nativeHandle == 0L
                    || token.frameIndex != frameResources.logicalFrameIndex()) {
                StreamlineFrameGenerationAdapter.disable();
                return FramePresentPlan.none();
            }
            boolean prepared = StreamlineFrameGenerationAdapter.prepareFrame(
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
            return prepared ? FramePresentPlan.streamline() : FramePresentPlan.none();
        }

        if (backend == FrameGenerationBackend.NGX) {
            NgxFrameGenerationAdapter.PrepareResult result = NgxFrameGenerationAdapter.prepareFrame(
                    frameResources,
                    constants,
                    mode,
                    colorWidth,
                    colorHeight,
                    commandBuffer
            );
            return result == null
                    ? FramePresentPlan.none()
                    : FramePresentPlan.generated(result.generatedFrames(), result.realFrame());
        }

        disableFrameGeneration();
        return FramePresentPlan.none();
    }

    public static synchronized void finishPresent(
            FrameResources frameResources,
            FramePresentPlan plan
    ) {
        if (activeBackend() == FrameGenerationBackend.STREAMLINE) {
            StreamlineFrameGenerationAdapter.finishPresent(
                    frameResources,
                    plan != null && plan.frameGenerationActive()
            );
        }
    }

    public static synchronized void disableFrameGeneration() {
        StreamlineFrameGenerationAdapter.disable();
        NgxFrameGenerationAdapter.disable();
    }

    public static void invalidateHistory() {
        FGConstantsFeature.invalidateHistory();
    }

    /**
     * Number of interpolated frames the presentation layer must present itself for
     * the next frame. Zero when disabled or when Streamline presents them.
     */
    public static synchronized int plannedGeneratedFrameCount() {
        if (!initialized || activeBackend() != FrameGenerationBackend.NGX) {
            return 0;
        }
        FrameGenerationMode mode = displayedMode();
        if (!mode.isEnabled()) {
            return 0;
        }
        return Math.min(
                Math.max(1, mode.generatedFrameCount()),
                NgxFrameGenerationAdapter.supportedGeneratedFrameCount()
        );
    }

    public static boolean isSupported() {
        return dependenciesSatisfied() && backendAvailable();
    }

    public static boolean isFrameGenerationEnabled() {
        return displayedMode().isEnabled();
    }

    public static void setFrameGenerationMode(FrameGenerationMode mode) {
        FrameGenerationMode selected = mode;
        if (selected == null || !isHardwareModeSupported(selected)) {
            selected = FrameGenerationMode.OFF;
        }
        SuperResolutionConfig.setFrameGenerationMode(selected);
        if (!displayedMode().isEnabled()) {
            disableFrameGeneration();
        }
    }

    public static FrameGenerationMode displayedMode() {
        FrameGenerationMode mode = SuperResolutionConfig.getFrameGenerationMode();
        return isModeSupported(mode) ? mode : FrameGenerationMode.OFF;
    }

    public static FrameGenerationMode[] availableModes() {
        if (!isSupported()) {
            return new FrameGenerationMode[]{FrameGenerationMode.OFF};
        }
        return availableModesForMaximum(supportedGeneratedFrameCount());
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

    static FrameGenerationBackend activeBackend() {
        // The frame_generation/provider config is applied at startup: it decides whether
        // Streamline is initialized (VulkanPresentationFeature.shouldInitializeStreamline).
        // At runtime the backend follows Streamline's actual init state, so changing the
        // provider only takes effect after a restart. Streamline runs frame generation when
        // it is initialized (Windows + provider != NGX); otherwise the cross-platform NVNGX
        // path runs it.
        return Streamline.isInitialized()
                ? FrameGenerationBackend.STREAMLINE
                : FrameGenerationBackend.NGX;
    }

    private static boolean backendAvailable() {
        return switch (activeBackend()) {
            case STREAMLINE -> StreamlineFrameGenerationAdapter.isAvailable();
            case NGX -> NgxFrameGenerationAdapter.isAvailable();
            case NONE -> false;
        };
    }

    private static int supportedGeneratedFrameCount() {
        return switch (activeBackend()) {
            case STREAMLINE -> StreamlineFrameGenerationAdapter.supportedGeneratedFrameCount();
            case NGX -> NgxFrameGenerationAdapter.supportedGeneratedFrameCount();
            case NONE -> 0;
        };
    }

    static boolean dependenciesSatisfied() {
        if (!SuperResolutionConfig.isEnableVulkanPresentation()
                || SuperResolutionConfig.getInteropSyncMode() != InteropSyncMode.LowLatency) {
            return false;
        }
        if (activeBackend() == FrameGenerationBackend.STREAMLINE) {
            // Streamline DLSS-G requires Reflex to drive its present pacing.
            return SuperResolutionConfig.getLowLatencyMode() == LowLatencyMode.NVReflex
                    && SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF;
        }
        return activeBackend() == FrameGenerationBackend.NGX;
    }

    // FG only under shader_compat + loaded pack; vanilla/hack breaks UI presentation
    static boolean isShaderEnvironmentCompatible() {
        if (!SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT)) {
            return false;
        }
        SRWorkModeState state = SRWorkModeManager.getCurrentState();
        return state.shaderPackInUse() && !state.shaderPackLoading();
    }

    private static boolean isModeSupported(FrameGenerationMode mode) {
        return isHardwareModeSupported(mode) && (mode == FrameGenerationMode.OFF || isShaderEnvironmentCompatible());
    }

    private static boolean isHardwareModeSupported(FrameGenerationMode mode) {
        if (mode == null || mode == FrameGenerationMode.OFF) {
            return mode == FrameGenerationMode.OFF;
        }
        return isSupported()
                && mode.generatedFrameCount() <= supportedGeneratedFrameCount();
    }

    enum FrameGenerationBackend {
        NONE,
        STREAMLINE,
        NGX
    }
}
