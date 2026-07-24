/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.lowlatency;

import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyMarker;
import io.homo.superresolution.api.registry.LowLatencyProvider;
import io.homo.superresolution.api.registry.LowLatencyRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;
import io.homo.superresolution.core.streamline.Streamline;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public final class LowLatency {
    private static @Nullable LowLatencyDescription mode;
    private static @Nullable LowLatencyProvider lowLatency;

    static {
        LowLatencyDescriptions.register();
    }

    private LowLatency() {
    }

    public static LowLatencyDescription mode() {
        if (mode == null) {
            String configuredId = SuperResolutionConfig.getLowLatencyMode();
            LowLatencyDescription description = LowLatencyRegistry.getDescriptionById(configuredId);
            mode = description != null ? description : LowLatencyRegistry.getDescriptionById("superresolution:none");
        }
        return mode;
    }

    public static @Nullable LowLatencyProvider lowLatency() {
        return lowLatency;
    }

    public static String modeId() {
        LowLatencyDescription current = mode();
        return current != null ? current.getId() : "superresolution:none";
    }

    public static synchronized void setMode(String newModeId) {
        String selected = newModeId == null ? "superresolution:none" : newModeId;
        LowLatencyDescription description = LowLatencyRegistry.getDescriptionById(selected);
        if (description == null) {
            description = LowLatencyRegistry.getDescriptionById("superresolution:none");
        }
        if (mode == description && lowLatency != null) {
            lowLatency.refresh();
            return;
        }
        releaseProvider();
        mode = description;
        lowLatency = description.createProvider();
        lowLatency.refresh();
    }

    public static int frameLimitUs() {
        int framerateLimit = MinecraftUtils.getFramerateLimit();
        if (framerateLimit <= 0 || framerateLimit >= 260) {
            return 0;
        }
        int outputMultiplier = FrameGeneration.isFrameGenerationEnabled() && Minecraft.getInstance().level != null
                ? 1 + FrameGeneration.displayedMode().generatedFrameCount()
                : 1;
        int outputFramerateLimit = framerateLimit * outputMultiplier;
        return (1_000_000 + outputFramerateLimit - 1) / outputFramerateLimit;
    }

    public static void beginPresent() {
        setMarker(LowLatencyMarker.PRESENT_START);
    }

    public static void endPresent() {
        setMarker(LowLatencyMarker.PRESENT_END);
    }

    public static void beginSimulation() {
        setMarker(LowLatencyMarker.SIMULATION_START);
    }

    public static void endSimulation() {
        setMarker(LowLatencyMarker.SIMULATION_END);
    }

    public static void beginRenderSubmission() {
        setMarker(LowLatencyMarker.RENDER_SUBMIT_START);
    }

    public static void endRenderSubmission() {
        setMarker(LowLatencyMarker.RENDER_SUBMIT_END);
    }

    public static synchronized void beginFrame(int frameIndex) {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        Streamline.nextFrame(frameIndex);
        VulkanLowLatency.nextFrame();
        String configuredId = SuperResolutionConfig.getLowLatencyMode();
        if (lowLatency == null || mode == null || !configuredId.equals(mode.getId())) {
            setMode(configuredId);
        } else {
            lowLatency.refresh();
        }
    }

    public static void onLatencyPing(boolean pressed) {
        if (pressed) {
            setMarker(LowLatencyMarker.LATENCY_PING);
        }
    }

    public static void onTriggerFlash() {
        setMarker(LowLatencyMarker.TRIGGER_FLASH);
    }

    public static void sleep() {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        LowLatencyProvider active = lowLatency;
        if (active != null) {
            active.sleep();
        }
    }

    public static void onDestructiveRebuild() {
        LowLatencyProvider active = lowLatency;
        if (active != null) {
            active.invalidatePacing();
        }
    }

    public static boolean isAvailable() {
        return SuperResolutionConfig.isEnableVulkanPresentation()
                && VulkanPresentationFeature.isAvailable();
    }

    public static boolean isPclAvailable() {
        var frame = Streamline.currentFrame();
        return lowLatency != null
                && Streamline.isInitialized()
                && frame != null
                && frame.nativeHandle != 0L;
    }

    public static synchronized void shutdown() {
        releaseProvider();
        mode = null;
    }

    private static void setMarker(LowLatencyMarker marker) {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        LowLatencyProvider active = lowLatency;
        if (active != null) {
            active.setMarker(marker);
        }
    }

    private static void releaseProvider() {
        if (lowLatency != null) {
            lowLatency.release();
            lowLatency = null;
        }
    }
}
