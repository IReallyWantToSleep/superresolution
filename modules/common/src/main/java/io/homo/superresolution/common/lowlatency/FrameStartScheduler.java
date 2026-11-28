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

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.debug.GpuHangWatchdog;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.common.perf.FrameRateLog;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * Starts each frame so that Minecraft's submission of it reaches the GPU just as frame generation
 * finishes the previous frame, while frame generation runs on Minecraft's Vulkan backend.
 *
 * <p>Minecraft records a whole frame into one submission. Once the previous frame's work ends on
 * the GPU, the GPU still runs DLSS-G on it, so the next submission should land when DLSS-G ends:
 * any earlier and it queues (latency), any later and the GPU idles (frame rate). So a frame starts
 * at the previous frame's GPU end, plus DLSS-G's GPU tail, minus the CPU time from frame start to
 * the submission. All three are measured: Minecraft's GPU end from its submit timeline (a thread
 * timestamps each completion), DLSS-G's end from the present thread's wait for the generated
 * image, and the CPU time on the render thread. (Waiting on DLSS-G's fence from another thread
 * instead held that command buffer's monitor and delayed presentation by 13 ms.) When the CPU time is longer than the tail, the
 * previous frame's end is still ahead and is predicted from the one before and the GPU period.</p>
 *
 * <p>Reflex alone left this on the table both ways (26.3 Vulkan, DLSS-G X2, 2560x1380). For
 * 7-16% of play it stopped sleeping and let a whole frame queue (simulation to GPU end 39 ms
 * against 23-25 ms). When it did pace, it woke frames 0.8 ms after the previous frame's GPU end
 * whatever the CPU time: with 7.7 ms of CPU against 5.6 ms of DLSS-G, the GPU idled ~2 ms a frame,
 * and with 4.4 ms of CPU against 6.5 ms the frame queued behind DLSS-G for 2 ms.</p>
 *
 * <p>Render thread, except where noted. {@code -Dsr.vk.frameStart=false} turns it off,
 * {@code =strict} only holds each frame until the previous one's GPU end.</p>
 */
public final class FrameStartScheduler {
    private static final String MODE = System.getProperty("sr.vk.frameStart", "scheduled");
    private static final boolean ENABLED = !"false".equalsIgnoreCase(MODE);
    private static final boolean STRICT = "strict".equalsIgnoreCase(MODE);
    private static final int HISTORY = 32;
    // Estimates need this many samples before the schedule replaces the plain hold.
    private static final int WARM_SAMPLES = 8;
    // The CPU time is taken high, so a slow frame's submission lands before DLSS-G ends rather
    // than after: a short overlap with DLSS-G costs less than the GPU idling.
    private static final double CPU_PERCENTILE = 0.75;
    private static final long MARGIN_NANOS = 300_000L;
    // Bounds of the start relative to the previous frame's GPU end: never more than this after
    // it, nor earlier than this share of the GPU period before it.
    private static final long MAX_DELAY_AFTER_END_NANOS = 8_000_000L;
    private static final double MAX_LEAD_SHARE = 0.5;
    // A frame this late is a hitch or a lost device, not a queue; Minecraft handles both.
    private static final long MAX_HOLD_NANOS = 100_000_000L;
    private static final long FINAL_SPIN_NANOS = 200_000L;
    private static final int RECENT_FRAMES = 16;

    private static final LinkedBlockingQueue<Submitted> OBSERVED = new LinkedBlockingQueue<>();
    private static final Submitted[] RECENT = new Submitted[RECENT_FRAMES];
    private static final long[] TAIL_SAMPLES = new long[HISTORY];
    private static int tailCount;
    private static int tailCursor;
    private static Thread observer;

    private static final long[] cpuSamples = new long[HISTORY];
    private static int cpuCount;
    private static int cpuCursor;
    private static B3DVulkanBridge.CommandCompletion recording;
    private static int recordingFrame;
    private static Submitted previousFrame;
    private static Submitted frameBefore;
    private static boolean scheduledThisFrame;
    private static long simulationStartNanos;
    private static long lastHoldNanos;
    private static long lastLeadNanos;
    private static boolean applied;

    private FrameStartScheduler() {
    }

    /**
     * At the start of a frame, before the low-latency sleep: holds the frame until its scheduled
     * start while frame generation is on.
     */
    public static void awaitFrameStart() {
        lastHoldNanos = 0L;
        scheduledThisFrame = false;
        boolean applies = ENABLED && applies();
        if (applies != applied) {
            applied = applies;
            resetEstimates();
        }
        Submitted previous = previousFrame;
        if (!applies || previous == null) {
            return;
        }
        scheduledThisFrame = true;
        long start = System.nanoTime();
        try (FramePacingTrace.Span ignored =
                     FramePacingTrace.INSTANCE.begin("frame_start_hold", FramePacingTrace.Context.empty())) {
            hold(previous, frameBefore, start);
        }
        lastHoldNanos = System.nanoTime() - start;
        FrameRateLog.onFrameStartHold(lastHoldNanos, lastLeadNanos);
    }

    /** Right after the low-latency sleep, when the frame's simulation starts. */
    public static void onSimulationStart() {
        simulationStartNanos = System.nanoTime();
    }

    /** Right before Minecraft submits the frame's commands: remembers what that submit signals. */
    public static void onFrameSubmitting(int frameIndex) {
        if (!ENABLED || !applied || !B3DVulkanBridge.isB3DVulkanBackend()) {
            recording = null;
            return;
        }
        recording = B3DVulkanBridge.captureCompletion();
        recordingFrame = frameIndex;
        if (scheduledThisFrame && simulationStartNanos != 0L) {
            addCpuSample(System.nanoTime() - simulationStartNanos);
        }
    }

    /** Right after Minecraft submitted the frame's commands. */
    public static void onFrameSubmitted() {
        B3DVulkanBridge.CommandCompletion completion = recording;
        recording = null;
        if (completion == null) {
            frameBefore = null;
            previousFrame = null;
            return;
        }
        completion.markSubmitted();
        Submitted submitted = new Submitted(recordingFrame, completion);
        synchronized (RECENT) {
            RECENT[Math.floorMod(recordingFrame, RECENT_FRAMES)] = submitted;
        }
        frameBefore = previousFrame;
        previousFrame = submitted;
        ensureObserver();
        OBSERVED.offer(submitted);
    }

    /**
     * Present thread: the last generated image of frame {@code frameIndex} has just become ready,
     * after a wait, so this is when frame generation finished that frame on the GPU.
     */
    public static void onGeneratedFrameReady(int frameIndex, long readyNanos) {
        if (!ENABLED) {
            return;
        }
        Submitted frame;
        synchronized (RECENT) {
            frame = RECENT[Math.floorMod(frameIndex, RECENT_FRAMES)];
        }
        if (frame == null || frame.frame != frameIndex) {
            return;
        }
        long done = frame.doneNanos;
        long tail = readyNanos - done;
        // A sample this long means a present that stalled, not frame generation's GPU time.
        if (done != 0L && tail > 0L && tail < 25_000_000L) {
            synchronized (TAIL_SAMPLES) {
                TAIL_SAMPLES[tailCursor] = tail;
                tailCursor = (tailCursor + 1) % HISTORY;
                tailCount = Math.min(tailCount + 1, HISTORY);
            }
        }
    }

    /** How long the last {@link #awaitFrameStart} held the frame. */
    public static long lastHoldNanos() {
        return lastHoldNanos;
    }

    private static void hold(Submitted previous, Submitted before, long start) {
        long deadline = start + MAX_HOLD_NANOS;
        long lead = STRICT ? 0L : scheduledLead(previous, before);
        lastLeadNanos = lead;
        while (true) {
            long now = System.nanoTime();
            if (now >= deadline) {
                return;
            }
            long previousDone = previous.doneNanos;
            if (previousDone != 0L) {
                if (now >= previousDone - lead) {
                    return;
                }
                sleepUntil(Math.min(previousDone - lead, deadline));
                return;
            }
            if (lead <= 0L || before == null || before.doneNanos == 0L) {
                // The start is at or after the previous frame's GPU end, or that end cannot be
                // predicted yet: wait for it. A wait that fails before its timeout means a lost
                // device, which Minecraft reports.
                if (!waitDone(previous, deadline - now) && System.nanoTime() < deadline) {
                    return;
                }
                continue;
            }
            long target = before.doneNanos + gpuPeriodNanos() - lead;
            if (now >= target) {
                return;
            }
            // Wake early if the previous frame ends before its predicted end.
            waitDone(previous, Math.min(target, deadline) - now);
        }
    }

    // How far before the previous frame's GPU end this frame should start; negative: after it.
    private static long scheduledLead(Submitted previous, Submitted before) {
        long cpu = percentile(cpuSamples, cpuCount, CPU_PERCENTILE);
        long tail;
        synchronized (TAIL_SAMPLES) {
            tail = tailCount < WARM_SAMPLES ? -1L : percentile(TAIL_SAMPLES, tailCount, 0.5);
        }
        if (cpuCount < WARM_SAMPLES || tail < 0L) {
            return 0L;
        }
        long period = gpuPeriodNanos();
        long maxLead = period > 0L ? (long) (period * MAX_LEAD_SHARE) : 0L;
        return Math.max(-MAX_DELAY_AFTER_END_NANOS, Math.min(maxLead, cpu - tail + MARGIN_NANOS));
    }

    // Median interval between recent frames' GPU ends; 0 until two consecutive ones are known.
    private static long gpuPeriodNanos() {
        long[] intervals = new long[RECENT_FRAMES];
        int count = 0;
        synchronized (RECENT) {
            for (Submitted frame : RECENT) {
                if (frame == null || frame.doneNanos == 0L) {
                    continue;
                }
                Submitted next = RECENT[Math.floorMod(frame.frame + 1, RECENT_FRAMES)];
                if (next != null && next.frame == frame.frame + 1 && next.doneNanos > frame.doneNanos) {
                    intervals[count++] = next.doneNanos - frame.doneNanos;
                }
            }
        }
        return count == 0 ? 0L : percentile(intervals, count, 0.5);
    }

    private static boolean waitDone(Submitted frame, long timeoutNanos) {
        if (timeoutNanos <= 0L) {
            return false;
        }
        if (frame.completion.awaitCompletion(timeoutNanos)) {
            frame.markDone(System.nanoTime());
            return true;
        }
        return false;
    }

    private static void sleepUntil(long target) {
        while (true) {
            long remaining = target - System.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            if (remaining > FINAL_SPIN_NANOS) {
                LockSupport.parkNanos(remaining - FINAL_SPIN_NANOS);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private static void addCpuSample(long value) {
        cpuSamples[cpuCursor] = value;
        cpuCursor = (cpuCursor + 1) % HISTORY;
        cpuCount = Math.min(cpuCount + 1, HISTORY);
    }

    private static long percentile(long[] samples, int count, double share) {
        if (count == 0) {
            return 0L;
        }
        long[] sorted = Arrays.copyOf(samples, count);
        Arrays.sort(sorted);
        return sorted[Math.min(count - 1, (int) (count * share))];
    }

    private static void resetEstimates() {
        cpuCount = 0;
        cpuCursor = 0;
        synchronized (TAIL_SAMPLES) {
            tailCount = 0;
            tailCursor = 0;
        }
        previousFrame = null;
        frameBefore = null;
        lastLeadNanos = 0L;
    }

    private static boolean applies() {
        return B3DVulkanBridge.isB3DVulkanBackend()
                && FrameGeneration.isFrameGenerationEnabled()
                && FrameGeneration.plannedGeneratedFrameCount() > 0
                && Minecraft.getInstance().level != null;
    }

    private static synchronized void ensureObserver() {
        if (observer != null) {
            return;
        }
        observer = new Thread(FrameStartScheduler::observe, "SR-Frame-GPU-End");
        observer.setDaemon(true);
        observer.start();
    }

    // Timestamps the end of each submitted frame's GPU work as it happens, so the schedule knows
    // it even for frames that ended while the render thread was busy.
    private static void observe() {
        while (true) {
            Submitted frame;
            try {
                frame = OBSERVED.take();
            } catch (InterruptedException exception) {
                return;
            }
            try {
                for (int attempt = 0; attempt < 20 && frame.doneNanos == 0L; attempt++) {
                    if (frame.completion.awaitCompletion(50_000_000L)) {
                        frame.markDone(System.nanoTime());
                        GpuHangWatchdog.record(GpuHangWatchdog.Queue.MINECRAFT, "done", frame.frame,
                                frame.completion.value());
                    }
                }
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.debug("Frame GPU end observer stopped waiting on a frame", throwable);
            }
        }
    }

    private static final class Submitted {
        private final int frame;
        private final B3DVulkanBridge.CommandCompletion completion;
        private volatile long doneNanos;

        private Submitted(int frame, B3DVulkanBridge.CommandCompletion completion) {
            this.frame = frame;
            this.completion = completion;
        }

        // The first thread to see the end records it; the observer normally does, as it waits.
        private synchronized void markDone(long nanos) {
            if (doneNanos == 0L) {
                doneNanos = nanos;
            }
        }
    }
}
