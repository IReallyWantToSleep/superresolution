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

import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationProvider;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeState;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend for frame generation. Owns the backend-agnostic policy (mode gating,
 * shader-environment checks, per-frame constants) and dispatches the actual work to the
 * selected {@link FrameGenerationProvider}, which is chosen from
 * {@link FrameGenerationRegistry} by the {@code frame_generation/provider} config id.
 * <p>
 * The selection is applied at startup — it decides whether Streamline is initialized (see
 * {@code VulkanPresentationFeature.shouldInitializeStreamline}) — so changing it takes
 * effect after a restart.
 */
public final class FrameGeneration {
    private static final Map<String, FrameGenerationProvider> providers = new LinkedHashMap<>();
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
     */
    public static FrameGenerationDescription mode() {
        FrameGenerationDescription description =
                FrameGenerationRegistry.getDescriptionById(SuperResolutionConfig.getFrameGenerationProvider());
        return description != null
                ? description
                : FrameGenerationRegistry.getDescriptionById(FrameGenerationDescriptions.AUTO_ID);
    }

    /**
     * Id of the backend actually in use, which differs from {@link #mode()} when the
     * automatic entry is selected or the chosen backend did not come up. Empty when
     * nothing is usable.
     */
    public static synchronized String activeId() {
        FrameGenerationDescription description = mode();
        if (description != null && !description.isAutomatic()) {
            FrameGenerationProvider selected = providers.get(description.getId());
            if (selected != null && selected.isAvailable()) {
                return description.getId();
            }
            // The chosen backend is not usable this session; fall back rather than
            // disabling frame generation outright, as the previous enum-based
            // selection did.
        }
        // Automatic: first registered backend that came up this session.
        for (Map.Entry<String, FrameGenerationProvider> entry : providers.entrySet()) {
            if (entry.getValue().isAvailable()) {
                return entry.getKey();
            }
        }
        return "";
    }

    private static synchronized @Nullable FrameGenerationProvider activeProvider() {
        String id = activeId();
        return id.isEmpty() ? null : providers.get(id);
    }

    /**
     * Coarse identity of the active backend. Kept for consumers that only need to know
     * whether Streamline owns presentation this session (Reflex does).
     */
    public static FrameGenerationBackend activeBackend() {
        String id = activeId();
        if (FrameGenerationDescriptions.STREAMLINE_ID.equals(id)) {
            return FrameGenerationBackend.STREAMLINE;
        }
        return id.isEmpty() ? FrameGenerationBackend.NONE : FrameGenerationBackend.NGX;
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
