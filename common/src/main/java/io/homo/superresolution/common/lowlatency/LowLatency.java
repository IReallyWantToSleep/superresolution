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

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflex;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.streamline.Streamline;

import javax.annotation.Nullable;

public final class LowLatency {
    private static LowLatencyMode mode;
    private static @Nullable ILowLatency lowLatency;

    private LowLatency() {
    }

    public static LowLatencyMode mode() {
        return mode != null ? mode : SuperResolutionConfig.getLowLatencyMode();
    }

    public static @Nullable ILowLatency lowLatency() {
        return lowLatency;
    }

    public static synchronized void setMode(LowLatencyMode newMode) {
        LowLatencyMode selected = newMode == null ? LowLatencyMode.None : newMode;
        if (mode == selected && lowLatency != null) {
            lowLatency.refresh();
            return;
        }
        releaseProvider();
        mode = selected;
        if (Streamline.isInitialized()) {
            lowLatency = createLowLatency();
            lowLatency.refresh();
        }
    }

    public static int frameLimitUs() {
        int framerateLimit = MinecraftUtils.getFramerateLimit();
        if (framerateLimit <= 0 || framerateLimit >= 260) {
            return 0;
        }
        return (1_000_000 + framerateLimit - 1) / framerateLimit;
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

    public static void beginSubmission() {
        setMarker(LowLatencyMarker.RENDER_SUBMIT_START);
    }

    public static void endSubmission() {
        setMarker(LowLatencyMarker.RENDER_SUBMIT_END);
    }

    public static synchronized void beginFrame(int frameIndex) {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        Streamline.nextFrame(frameIndex);
        LowLatencyMode configuredMode = SuperResolutionConfig.getLowLatencyMode();
        if (lowLatency == null || mode != configuredMode) {
            setMode(configuredMode);
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
        ILowLatency active = lowLatency;
        if (active != null) {
            active.sleep();
        }
    }

    public static void onDestructiveRebuild() {
        ILowLatency active = lowLatency;
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

    private static ILowLatency createLowLatency() {
        return new NVIDIAReflex();
    }

    private static void setMarker(LowLatencyMarker marker) {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        ILowLatency active = lowLatency;
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
