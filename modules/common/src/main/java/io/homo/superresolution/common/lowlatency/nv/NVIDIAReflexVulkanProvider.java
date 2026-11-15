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

package io.homo.superresolution.common.lowlatency.nv;

import io.homo.superresolution.api.registry.lowlatency.LowLatencyMarker;
import io.homo.superresolution.api.registry.lowlatency.LowLatencyProvider;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;
import net.minecraft.client.Minecraft;

import static org.lwjgl.vulkan.NVLowLatency2.*;

/**
 * VK_NV_low_latency2 backend for NVIDIA Reflex. Registered inside the
 * {@code superresolution:nv_reflex} group by {@code LowLatencyDescriptions}.
 * The Streamline-based Reflex backend lives in the Wisteria mod.
 */
public final class NVIDIAReflexVulkanProvider implements LowLatencyProvider {
    private static final int PACING_WARMUP_FRAMES = 3;

    private final AdaptiveFrameCap frameCap = new AdaptiveFrameCap();
    private OptionsKey lastAppliedOptions;
    private int pacingWarmupRemaining;

    public NVIDIAReflexVulkanProvider() {
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
        frameCap.reset();
    }

    @Override
    public void refresh() {
        int reflexMode = SuperResolutionConfig.getNVIDIAReflexMode().ordinal();
        OptionsKey desired = new OptionsKey(
                reflexMode != NVIDIAReflexMode.OFF.ordinal(),
                reflexMode == NVIDIAReflexMode.BOOST.ordinal(),
                Math.max(userRealFrameIntervalUs(), stabilizesFrameRate() ? frameCap.capMicros() : 0)
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
        frameCap.reset();
    }

    @Override
    public void sleep() {
        if (pacingWarmupRemaining > 0) {
            pacingWarmupRemaining--;
            return;
        }
        VulkanLowLatency.sleep();
        if (stabilizesFrameRate()) {
            frameCap.onFrameStart(System.nanoTime(), userRealFrameIntervalUs() * 1000L);
        } else {
            frameCap.reset();
        }
        traceLatencyReports();
    }

    private static boolean stabilizesFrameRate() {
        return SuperResolutionConfig.isFrameGenerationStabilizeFrameRate()
                && SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF
                && FrameGeneration.isFrameGenerationEnabled()
                && Minecraft.getInstance().level != null;
    }

    // Copies the driver's own per-frame view (vkGetLatencyTimingsNV) into a running
    // frame-pacing trace. Timestamps stay in the driver's microseconds in the details.
    private static void traceLatencyReports() {
        if (!FramePacingTrace.INSTANCE.isRecording()) {
            return;
        }
        for (VulkanLowLatency.LatencyFrameReport report : VulkanLowLatency.takeLatencyReports()) {
            FramePacingTrace.INSTANCE.instant(
                    "reflex_latency_report",
                    new FramePacingTrace.Context(-1, -1L, -1L, -1L, report.presentId(), "REAL", ""),
                    "complete",
                    "input=" + report.inputSampleUs()
                            + ",sim_start=" + report.simStartUs()
                            + ",sim_end=" + report.simEndUs()
                            + ",submit_start=" + report.renderSubmitStartUs()
                            + ",submit_end=" + report.renderSubmitEndUs()
                            + ",present_start=" + report.presentStartUs()
                            + ",present_end=" + report.presentEndUs()
                            + ",driver_start=" + report.driverStartUs()
                            + ",driver_end=" + report.driverEndUs()
                            + ",os_queue_start=" + report.osRenderQueueStartUs()
                            + ",os_queue_end=" + report.osRenderQueueEndUs()
                            + ",gpu_start=" + report.gpuRenderStartUs()
                            + ",gpu_end=" + report.gpuRenderEndUs()
            );
        }
    }

    /**
     * vkLatencySleepNV throttles once per rendered frame, while the configured
     * framerate limit counts displayed frames; with frame generation the real
     * frame interval is scaled by the output multiplier so the displayed rate
     * matches the limit.
     */
    private static int userRealFrameIntervalUs() {
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
