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

package io.homo.superresolution.common.perf;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Locale;

/**
 * Logs the frame rate once a second while {@code debug/log_frame_rate} is on, so that a game
 * log shows how a session ran without a trace: rendered and shown frame rates, the slowest
 * frame, how long presents blocked, the low-latency sleep and frame cap, the driver's latency
 * reports, and the time since the world was entered. Whenever the upscaler, frame generation or
 * low-latency setup changes, a line describing it comes first.
 *
 * <p>Frames, sleeps and latency reports are recorded on the render thread; presents arrive
 * from the present threads.</p>
 */
public final class FrameRateLog {
    private static final String PREFIX = "[SR frame rate] ";
    private static final long INTERVAL_NANOS = 1_000_000_000L;
    private static final Object PRESENT_LOCK = new Object();

    private static volatile boolean enabled;

    // Render thread.
    private static long intervalStartNanos;
    private static long lastFrameNanos;
    private static int frames;
    private static long maxFrameNanos;
    private static int sleeps;
    private static long sleepNanos;
    private static int frameLimitMicros;
    private static int latencyReports;
    private static long latencyMicros;
    private static int submitReports;
    private static long submitMicros;
    private static Object world;
    private static long worldStartNanos;
    private static String lastSetup;

    // GPU time of SR's own Vulkan work, from VulkanTimestampProfiler.collect on the render thread.
    // The samples land a few frames after the work, so a total over the interval divided by its
    // frames is the per-frame cost.
    private static final String[] GPU_REGIONS = {
            PerformanceTracker.VK_UPSCALE,
            PerformanceTracker.VK_UPSCALE_PROVIDER,
            PerformanceTracker.VK_FRAME_CAPTURE,
            PerformanceTracker.VK_FRAME_GEN,
            PerformanceTracker.VK_PRESENT_BLIT
    };
    private static final String[] GPU_LABELS = {
            "upscale", "provider", "capture", "frame generation", "present blit"
    };
    private static final int GPU_UPSCALE = 0;
    private static final int GPU_PROVIDER = 1;
    private static final long[] gpuNanos = new long[GPU_REGIONS.length];

    // Present threads, under PRESENT_LOCK.
    private static int realPresents;
    private static long realPresentNanos;
    private static int generatedPresents;
    private static long generatedPresentNanos;
    private static long maxPresentNanos;

    private FrameRateLog() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Called on the render thread at the end of every frame. */
    public static void onFrameEnd() {
        if (!SuperResolution.gameIsLoaded) {
            return;
        }
        long now = System.nanoTime();
        Object level = Minecraft.getInstance().level;
        if (level != world) {
            world = level;
            worldStartNanos = now;
        }
        if (!enabled) {
            // The option is re-read once a second.
            if (now - intervalStartNanos >= INTERVAL_NANOS) {
                intervalStartNanos = now;
                if (SuperResolutionConfig.isLogFrameRate()) {
                    start(now);
                }
            }
            return;
        }
        maxFrameNanos = Math.max(maxFrameNanos, now - lastFrameNanos);
        lastFrameNanos = now;
        frames++;
        long elapsed = now - intervalStartNanos;
        if (elapsed < INTERVAL_NANOS) {
            return;
        }
        if (!SuperResolutionConfig.isLogFrameRate()) {
            enabled = false;
            intervalStartNanos = now;
            return;
        }
        logInterval(now, elapsed);
        resetInterval(now);
    }

    /** Records one vkQueuePresentKHR and how long the call blocked. */
    public static void onPresent(boolean generated, long blockedNanos) {
        if (!enabled) {
            return;
        }
        synchronized (PRESENT_LOCK) {
            if (generated) {
                generatedPresents++;
                generatedPresentNanos += blockedNanos;
            } else {
                realPresents++;
                realPresentNanos += blockedNanos;
            }
            maxPresentNanos = Math.max(maxPresentNanos, blockedNanos);
        }
    }

    /** Records how long the low-latency provider slept before a frame. Render thread. */
    public static void onLowLatencySleep(long nanos) {
        if (!enabled) {
            return;
        }
        sleeps++;
        sleepNanos += nanos;
    }

    /** Records the real-frame interval the low-latency provider enforces, 0 if none. Render thread. */
    public static void onFrameLimit(int micros) {
        frameLimitMicros = micros;
    }

    /**
     * Records one of the driver's per-frame latency reports, in its microseconds: the latency
     * from simulation start, and the time from the start of render submission, to the end of
     * the frame's GPU work.
     */
    public static void onLatencyReport(long simStartUs, long renderSubmitStartUs, long gpuEndUs) {
        if (!enabled || gpuEndUs == 0L) {
            return;
        }
        if (simStartUs != 0L && gpuEndUs >= simStartUs) {
            latencyReports++;
            latencyMicros += gpuEndUs - simStartUs;
        }
        if (renderSubmitStartUs != 0L && gpuEndUs >= renderSubmitStartUs) {
            submitReports++;
            submitMicros += gpuEndUs - renderSubmitStartUs;
        }
    }

    /** Records the GPU time of one timed region of SR's Vulkan work. Render thread. */
    public static void onGpuSample(String region, long nanos) {
        if (!enabled) {
            return;
        }
        for (int i = 0; i < GPU_REGIONS.length; i++) {
            if (GPU_REGIONS[i].equals(region)) {
                gpuNanos[i] += nanos;
                return;
            }
        }
    }

    private static void start(long now) {
        lastSetup = null;
        synchronized (PRESENT_LOCK) {
            resetPresents();
        }
        resetInterval(now);
        enabled = true;
    }

    private static void resetInterval(long now) {
        intervalStartNanos = now;
        lastFrameNanos = now;
        frames = 0;
        maxFrameNanos = 0L;
        sleeps = 0;
        sleepNanos = 0L;
        frameLimitMicros = 0;
        latencyReports = 0;
        latencyMicros = 0L;
        submitReports = 0;
        submitMicros = 0L;
        Arrays.fill(gpuNanos, 0L);
    }

    private static void resetPresents() {
        realPresents = 0;
        realPresentNanos = 0L;
        generatedPresents = 0;
        generatedPresentNanos = 0L;
        maxPresentNanos = 0L;
    }

    private static void logInterval(long now, long elapsedNanos) {
        String setup = describeSetup();
        if (!setup.equals(lastSetup)) {
            lastSetup = setup;
            SuperResolution.LOGGER.info(PREFIX + "{}", setup);
        }
        int real;
        int generated;
        long realNanos;
        long generatedNanos;
        long maxPresent;
        synchronized (PRESENT_LOCK) {
            real = realPresents;
            generated = generatedPresents;
            realNanos = realPresentNanos;
            generatedNanos = generatedPresentNanos;
            maxPresent = maxPresentNanos;
            resetPresents();
        }
        double seconds = elapsedNanos / 1e9;
        StringBuilder line = new StringBuilder(192);
        line.append(format("%.1f fps", frames / seconds));
        if (real + generated > 0) {
            line.append(format(" | shown %.1f fps (%.1f generated)", (real + generated) / seconds, generated / seconds));
        }
        line.append(format(" | frame %.1f ms avg, %.1f max", elapsedNanos / 1e6 / Math.max(frames, 1), maxFrameNanos / 1e6));
        if (real + generated > 0) {
            line.append(" | present");
            if (real > 0) {
                line.append(format(" real %.2f", realNanos / 1e6 / real));
            }
            if (generated > 0) {
                line.append(format(" gen %.2f", generatedNanos / 1e6 / generated));
            }
            line.append(format(" max %.2f ms", maxPresent / 1e6));
        }
        if (sleeps > 0) {
            line.append(format(" | low-latency sleep %.1f ms", sleepNanos / 1e6 / sleeps));
            if (frameLimitMicros > 0) {
                line.append(format(", cap %.1f ms", frameLimitMicros / 1000.0));
            }
        }
        if (latencyReports > 0) {
            line.append(format(" | latency %.1f ms", latencyMicros / 1000.0 / latencyReports));
        }
        if (submitReports > 0) {
            line.append(format(latencyReports > 0 ? ", submit->GPU end %.1f ms" : " | submit->GPU end %.1f ms",
                    submitMicros / 1000.0 / submitReports));
        }
        appendGpuTimes(line);
        line.append(world == null ? " | no world" : format(" | world %d s", (now - worldStartNanos) / 1_000_000_000L));
        SuperResolution.LOGGER.info(PREFIX + "{}", line);
    }

    /** Appends e.g. {@code | SR GPU per frame: upscale 2.10 ms (provider 1.62), capture 0.31 ms}. */
    private static void appendGpuTimes(StringBuilder line) {
        double frameCount = Math.max(frames, 1);
        boolean any = false;
        for (int i = 0; i < GPU_REGIONS.length; i++) {
            // The provider runs inside the upscale region, so it is shown as a part of it.
            boolean nestedProvider = i == GPU_PROVIDER && gpuNanos[GPU_UPSCALE] != 0L;
            if (gpuNanos[i] == 0L || nestedProvider) {
                continue;
            }
            line.append(any ? ", " : " | SR GPU per frame: ");
            line.append(format("%s %.2f ms", GPU_LABELS[i], gpuNanos[i] / 1e6 / frameCount));
            if (i == GPU_UPSCALE && gpuNanos[GPU_PROVIDER] != 0L) {
                line.append(format(" (%s %.2f)", GPU_LABELS[GPU_PROVIDER], gpuNanos[GPU_PROVIDER] / 1e6 / frameCount));
            }
            any = true;
        }
    }

    private static String describeSetup() {
        try {
            StringBuilder setup = new StringBuilder("setup: ");
            if (SuperResolutionConfig.isEnableUpscale()) {
                setup.append(SuperResolutionConfig.UPSCALE_ALGO.get()).append(' ')
                        .append(RenderHandlerManager.getRenderWidth()).append('x')
                        .append(RenderHandlerManager.getRenderHeight()).append(" -> ");
            } else {
                setup.append("no upscaling, ");
            }
            setup.append(RenderHandlerManager.getScreenWidth()).append('x').append(RenderHandlerManager.getScreenHeight());
            setup.append(" | frame generation ");
            if (FrameGeneration.isFrameGenerationEnabled()) {
                setup.append(FrameGeneration.activeId()).append(' ').append(FrameGeneration.displayedMode().name());
            } else {
                setup.append("off");
            }
            setup.append(" | low latency ").append(LowLatency.modeId())
                    .append(", Reflex ").append(SuperResolutionConfig.getNVIDIAReflexMode().name())
                    .append(", stabilize ").append(SuperResolutionConfig.isFrameGenerationStabilizeFrameRate() ? "on" : "off");
            return setup.toString();
        } catch (RuntimeException exception) {
            return "setup: unavailable (" + exception.getClass().getSimpleName() + ")";
        }
    }

    private static String format(String pattern, Object... arguments) {
        return String.format(Locale.ROOT, pattern, arguments);
    }
}
