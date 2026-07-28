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

import io.homo.superresolution.api.registry.BackendGroup;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationProvider;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeState;
import io.homo.superresolution.core.streamline.Streamline;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend for frame generation. Owns the backend-agnostic policy (mode gating,
 * shader-environment checks, per-frame constants) and dispatches the actual work to a
 * concrete {@link FrameGenerationProvider} picked by {@link BackendNegotiator} from the
 * algorithm group ({@code frame_generation/provider}) and optional concrete backend
 * preference ({@code frame_generation/backend}).
 * <p>
 * The FG group and concrete backend preference are latched at startup — together with the
 * LL group configuration they decide whether Streamline is initialized (see
 * {@code VulkanPresentationFeature.shouldInitializeStreamline}) — so changing either takes
 * effect after a restart. Runtime availability and low-latency bindings are still
 * re-negotiated as needed.
 */
public final class FrameGeneration {
    private static final Map<String, FrameGenerationProvider> providers = new LinkedHashMap<>();
    private static BackendNegotiator.Resolution loggedResolution;
    private static @Nullable Boolean startupStreamlineRequested;
    private static @Nullable Boolean loggedRestartStreamlineRequest;
    private static @Nullable String startupPreferredFgBackendId;
    private static boolean initialized;

    static {
        FrameGenerationDescriptions.register();
    }

    private FrameGeneration() {
    }

    public static synchronized void initialize() {
        if (initialized || !VulkanPresentationFeature.isRequested()) {
            return;
        }
        FGConstantsFeature.initialize();
        FGConstantsFeature.register();
        startupStreamlineRequested = VulkanPresentationFeature.shouldInitializeStreamline();
        startupPreferredFgBackendId = SuperResolutionConfig.getFrameGenerationBackend();
        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
            if (description.isAutomatic() || !FrameGenerationRegistry.isSupported(description)) {
                continue;
            }
            FrameGenerationProvider provider = description.createProvider();
            if (provider != null) {
                providers.put(description.getId(), provider);
                provider.initialize();
            }
        }
        initialized = true;
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        for (FrameGenerationProvider provider : providers.values()) {
            provider.shutdown();
        }
        providers.clear();
        loggedResolution = null;
        startupStreamlineRequested = null;
        loggedRestartStreamlineRequest = null;
        startupPreferredFgBackendId = null;
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
        FrameGenerationProvider provider = activeProvider();
        if (!initialized
                || provider == null
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

        return provider.prepareFrame(
                frameResources,
                constants,
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
            FramePresentPlan plan
    ) {
        FrameGenerationProvider provider = activeProvider();
        if (provider != null) {
            provider.finishPresent(frameResources, plan != null && plan.frameGenerationActive());
        }
    }

    public static synchronized void disableFrameGeneration() {
        for (FrameGenerationProvider provider : providers.values()) {
            provider.disable();
        }
    }

    public static void invalidateHistory() {
        FGConstantsFeature.invalidateHistory();
    }

    /**
     * Number of interpolated frames the presentation layer must present itself for the
     * next frame. Zero when disabled or when the active backend presents them.
     */
    public static synchronized int plannedGeneratedFrameCount() {
        if (!initialized) {
            return 0;
        }
        FrameGenerationMode mode = displayedMode();
        if (!mode.isEnabled()) {
            return 0;
        }
        FrameGenerationProvider provider = activeProvider();
        return provider == null ? 0 : provider.presentationManagedGeneratedFrameCount(mode);
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

    /**
     * The configured entry, falling back to the automatic one when the id is unknown.
     * The returned description may be an algorithm group representative (automatic with a
     * group) rather than a concrete backend.
     */
    public static FrameGenerationDescription mode() {
        FrameGenerationDescription description =
                FrameGenerationRegistry.getDescriptionById(SuperResolutionConfig.getFrameGenerationProvider());
        return description != null
                ? description
                : FrameGenerationRegistry.getDescriptionById(FrameGenerationDescriptions.AUTO_ID);
    }

    /**
     * Id of the FG backend actually in use, as resolved by the negotiator for the
     * currently configured FG and LL groups. Empty when nothing is usable.
     */
    public static synchronized String activeId() {
        String id = activeResolution().fgBackendId();
        return id == null ? "" : id;
    }

    /**
     * Id of the LL backend the negotiator picked to pair with the current FG choice.
     * Empty when no LL backend is active. Callers on the LL side (mainly {@link LowLatency})
     * use this to switch providers when the FG side's binding constraints flip.
     */
    public static synchronized String activeLowLatencyBackendId() {
        String id = activeResolution().lowLatencyBackendId();
        return id == null ? "" : id;
    }

    /**
     * Runs the negotiator with the current configuration. Cheap enough to invoke per frame
     * (a few map iterations); callers that hit it repeatedly per frame should cache locally.
     */
    private static synchronized BackendNegotiator.Resolution activeResolution() {
        String fgGroupId = configuredFgGroupId();
        BackendNegotiator.Resolution resolution = fgGroupId == null || fgGroupId.isEmpty()
                ? BackendNegotiator.Resolution.EMPTY
                : BackendNegotiator.resolve(
                        fgGroupId,
                        LowLatency.configuredGroupId(),
                        startupPreferredFgBackendId != null
                                ? startupPreferredFgBackendId
                                : SuperResolutionConfig.getFrameGenerationBackend(),
                        providers::get
                );
        logResolution(resolution);
        logRestartRequirement();
        return resolution;
    }

    private static void logResolution(BackendNegotiator.Resolution resolution) {
        if (!resolution.equals(loggedResolution)) {
            SuperResolution.LOGGER.info(
                    "Frame generation backend negotiation: fg={}, lowLatency={}",
                    resolution.fgBackendId(),
                    resolution.lowLatencyBackendId()
            );
            loggedResolution = resolution;
        }
    }

    private static void logRestartRequirement() {
        if (startupStreamlineRequested == null) {
            return;
        }
        boolean streamlineRequested = VulkanPresentationFeature.shouldInitializeStreamline();
        if (streamlineRequested == startupStreamlineRequested
                || Boolean.valueOf(streamlineRequested).equals(loggedRestartStreamlineRequest)
                || (!streamlineRequested && !Streamline.isInterposerLoaded())) {
            return;
        }
        SuperResolution.LOGGER.warn(
                "Streamline backend selection changed after startup; restart the game to {} the Streamline interposer.",
                streamlineRequested ? "load" : "unload"
        );
        loggedRestartStreamlineRequest = streamlineRequested;
    }

    private static synchronized @Nullable FrameGenerationProvider activeProvider() {
        String id = activeId();
        return id.isEmpty() ? null : providers.get(id);
    }

    /**
     * Derives the FG group id passed to the negotiator from the configured entry. Automatic
     * with no group means "any group"; automatic with a group means "this group only";
     * a concrete backend selection restricts to that backend's group.
     */
    private static @Nullable String configuredFgGroupId() {
        FrameGenerationDescription description = mode();
        if (description == null) {
            return null;
        }
        BackendGroup group = description.getGroup();
        if (description.isAutomatic()) {
            return group != null ? group.getId() : BackendNegotiator.AUTO_FG_GROUP;
        }
        return group != null ? group.getId() : null;
    }

    private static boolean backendAvailable() {
        FrameGenerationProvider provider = activeProvider();
        return provider != null && provider.isAvailable();
    }

    private static int supportedGeneratedFrameCount() {
        FrameGenerationProvider provider = activeProvider();
        return provider == null ? 0 : provider.supportedGeneratedFrameCount();
    }

    static boolean dependenciesSatisfied() {
        if (!SuperResolutionConfig.isEnableVulkanPresentation()
                || SuperResolutionConfig.getInteropSyncMode() != InteropSyncMode.LowLatency) {
            return false;
        }
        FrameGenerationProvider provider = activeProvider();
        return provider != null && provider.dependenciesSatisfied();
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
}
