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
    /** The configured group representative (never a concrete backend). */
    private static @Nullable LowLatencyDescription mode;
    /** The negotiator-resolved backend description (may differ from {@link #mode}). */
    private static @Nullable LowLatencyDescription activeBackend;
    private static @Nullable LowLatencyProvider lowLatency;
    private static volatile long currentLatencyFrameId;

    static {
        LowLatencyDescriptions.register();
    }

    private LowLatency() {
    }

    /**
     * The configured low-latency algorithm group. Returned as a {@link LowLatencyDescription}
     * for backward compatibility with existing consumers that check its id
     * (e.g. {@code !"superresolution:none".equals(mode.getId())}).
     */
    public static LowLatencyDescription mode() {
        if (mode == null) {
            mode = resolveConfiguredGroup();
        }
        return mode;
    }

    /** Group id of the configured low-latency algorithm. */
    public static String configuredGroupId() {
        LowLatencyDescription current = mode();
        return current != null ? current.getId() : LowLatencyDescriptions.NONE_ID;
    }

    public static @Nullable LowLatencyProvider lowLatency() {
        return lowLatency;
    }

    public static String modeId() {
        return configuredGroupId();
    }

    /** Records a new configured group id and re-resolves the active backend accordingly. */
    public static synchronized void setMode(String newModeId) {
        String selected = newModeId == null ? LowLatencyDescriptions.NONE_ID : newModeId;
        LowLatencyDescription description = LowLatencyRegistry.getDescriptionById(selected);
        if (description == null) {
            description = LowLatencyRegistry.getDescriptionById(LowLatencyDescriptions.NONE_ID);
        }
        mode = description;
        renegotiate();
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
        currentLatencyFrameId = frameIndex;
        Streamline.nextFrame(frameIndex);
        VulkanLowLatency.nextFrame(frameIndex);
        // Resolve the configured group first so an unknown persisted id falls back to
        // "none" without recreating the fallback provider on every frame. Re-resolving
        // also lets a backend registered later become active without rewriting config.
        LowLatencyDescription configured = resolveConfiguredGroup();
        if (mode == null || !configured.equals(mode)) {
            setMode(configured.getId());
        } else {
            renegotiate();
        }
    }

    public static long currentLatencyFrameId() {
        return currentLatencyFrameId;
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
        activeBackend = null;
        mode = null;
    }

    /**
     * Re-runs the {@code (fg, ll)} negotiation and swaps the active provider if the
     * resolved backend changed. Called on config changes and per-frame in
     * {@link #beginFrame(int)} because the FG side's binding constraints can flip which
     * LL backend is picked at any time.
     */
    private static void renegotiate() {
        String targetBackendId = FrameGeneration.activeLowLatencyBackendId();
        LowLatencyDescription target = targetBackendId.isEmpty()
                ? null
                : LowLatencyRegistry.getDescriptionById(targetBackendId);
        if (target != activeBackend) {
            releaseProvider();
            activeBackend = target;
            if (target != null) {
                lowLatency = target.createProvider();
            }
        }
        if (lowLatency != null) {
            lowLatency.refresh();
        }
    }

    private static LowLatencyDescription resolveConfiguredGroup() {
        String configuredId = SuperResolutionConfig.getLowLatencyMode();
        LowLatencyDescription description = LowLatencyRegistry.getDescriptionById(configuredId);
        return description != null
                ? description
                : LowLatencyRegistry.getDescriptionById(LowLatencyDescriptions.NONE_ID);
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
