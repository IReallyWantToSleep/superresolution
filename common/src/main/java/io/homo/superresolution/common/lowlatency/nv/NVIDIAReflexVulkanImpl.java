/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.lowlatency.nv;

import io.homo.superresolution.api.registry.LowLatencyMarker;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;
import net.minecraft.client.Minecraft;

import static org.lwjgl.vulkan.NVLowLatency2.*;


public final class NVIDIAReflexVulkanImpl implements ReflexImplementation {
    private static final int PACING_WARMUP_FRAMES = 3;

    private OptionsKey lastAppliedOptions;
    private int pacingWarmupRemaining;

    public NVIDIAReflexVulkanImpl() {
        VulkanLowLatency.setActive(true);
    }

    public static boolean isSupported() {
        return VulkanLowLatency.isSupported();
    }

    @Override
    public void setMarker(LowLatencyMarker marker) {
        switch (marker) {
            case SIMULATION_START -> VulkanLowLatency.frameMarker(VK_LATENCY_MARKER_SIMULATION_START_NV);
            case SIMULATION_END -> VulkanLowLatency.frameMarker(VK_LATENCY_MARKER_SIMULATION_END_NV);
            case RENDER_SUBMIT_START -> VulkanLowLatency.frameMarker(VK_LATENCY_MARKER_RENDERSUBMIT_START_NV);
            case RENDER_SUBMIT_END -> VulkanLowLatency.frameMarker(VK_LATENCY_MARKER_RENDERSUBMIT_END_NV);
            case PRESENT_START -> VulkanLowLatency.presentPhaseMarker(true);
            case PRESENT_END -> VulkanLowLatency.presentPhaseMarker(false);
            case TRIGGER_FLASH -> VulkanLowLatency.frameMarker(VK_LATENCY_MARKER_TRIGGER_FLASH_NV);
            case LATENCY_PING -> {
                // PCL latency pings are a Streamline/FrameView concept with no
                // VK_NV_low_latency2 equivalent.
            }
        }
    }

    @Override
    public void release() {
        VulkanLowLatency.setSleepMode(false, false, 0);
        VulkanLowLatency.setActive(false);
        lastAppliedOptions = null;
    }

    @Override
    public void refresh(int reflexMode) {
        OptionsKey desired = new OptionsKey(
                reflexMode != NVIDIAReflexMode.OFF.ordinal(),
                reflexMode == NVIDIAReflexMode.BOOST.ordinal(),
                realFrameIntervalUs()
        );
        if (desired.equals(lastAppliedOptions)) {
            return;
        }
        if (VulkanLowLatency.setSleepMode(desired.lowLatencyMode(), desired.boost(), desired.frameIntervalUs())) {
            lastAppliedOptions = desired;
        }
    }

    @Override
    public void invalidatePacing() {
        pacingWarmupRemaining = PACING_WARMUP_FRAMES;
        lastAppliedOptions = null;
    }

    @Override
    public void sleep() {
        if (pacingWarmupRemaining > 0) {
            pacingWarmupRemaining--;
            return;
        }
        VulkanLowLatency.sleep();
    }

    /**
     * vkLatencySleepNV throttles once per rendered frame, while the configured
     * framerate limit counts displayed frames; with frame generation the real
     * frame interval is scaled by the output multiplier so the displayed rate
     * matches the limit.
     */
    private static int realFrameIntervalUs() {
        int framerateLimit = MinecraftUtils.getFramerateLimit();
        if (framerateLimit <= 0 || framerateLimit >= 260) {
            return 0;
        }
        int outputMultiplier = FrameGeneration.isFrameGenerationEnabled() && Minecraft.getInstance().level != null
                ? 1 + FrameGeneration.displayedMode().generatedFrameCount()
                : 1;
        return (int) (((long) outputMultiplier * 1_000_000L + framerateLimit - 1) / framerateLimit);
    }

    private record OptionsKey(
            boolean lowLatencyMode,
            boolean boost,
            int frameIntervalUs
    ) {
    }
}
