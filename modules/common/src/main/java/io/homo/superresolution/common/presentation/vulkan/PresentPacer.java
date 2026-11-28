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

package io.homo.superresolution.common.presentation.vulkan;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;

/** Shared phase timing and present-deadline state for application-managed presentation. */
final class PresentPacer {
    private static final long MAX_PRESENT_INTERVAL_NANOS = 100_000_000L;
    private static final long FINAL_SPIN_WINDOW_NANOS = 200_000L;
    private static final long BACKLOG_CATCH_UP_DIVISOR = 8L;
    // A batch with one real frame queued behind it runs on 3/4 of the interval, with two or more
    // on half of it.
    private static final long BACKLOG_ONE_FRAME_INTERVAL_NUMERATOR = 3L;
    // Batches of up to this many images get their own present block averages.
    private static final int MAX_TRACKED_BATCH_SIZE = 8;
    private static final long PRESENT_BLOCK_AVERAGE_DIVISOR = 16L;
    private static final long MIN_PRESENT_BLOCK_SAMPLE_CAP_NANOS = 1_000_000L;

    private final AsyncFramePresenter.NanoClock clock;
    private final LongConsumer deadlineWaiter;
    private final ThreadLocal<PhaseStarts> phaseStarts =
            ThreadLocal.withInitial(PhaseStarts::new);
    private final AtomicLongArray lastPhaseDurations = new AtomicLongArray(Phase.values().length);
    private final AtomicLong realFrameProducerTimeNanos = new AtomicLong();
    private final AtomicBoolean hasRealFrameProducerTime = new AtomicBoolean();
    private long nextDeadlineNanos;
    private boolean previousPacingEnabled;
    private int previousGeneratedCount = -1;
    private long presentIntervalNanos;
    // Running average of how long each position's vkQueuePresentKHR blocked, per batch size;
    // -1 until measured. Present thread only.
    private final long[][] presentBlockNanos = newPresentBlockAverages();

    PresentPacer(AsyncFramePresenter.NanoClock clock) {
        this.clock = clock;
        this.deadlineWaiter = this::sleepUntil;
    }

    PresentPacer(AsyncFramePresenter.NanoClock clock, LongConsumer deadlineWaiter) {
        this.clock = clock;
        this.deadlineWaiter = deadlineWaiter;
    }

    void beginRealFrameRendering() {
        beginPhase(Phase.REAL_FRAME_RENDERING);
    }

    // The real-frame period is sampled on the wall clock. Subtracting Super Resolution's own
    // queue and slot waits measured only CPU time whenever the GPU was the bottleneck, since
    // that backpressure lands on exactly those waits: at 2560x1380 the estimate came out at
    // ~5 ms for 24 ms frames and the batch went out 2.5 ms apart. A presenter that falls
    // behind would now throttle production to its own pace, so beginBatch catches up instead.
    void endRealFrameRendering() {
        long completedAtNanos = clock.nanoTime();
        endPhase(Phase.REAL_FRAME_RENDERING, completedAtNanos);
        realFrameProducerTimeNanos.set(completedAtNanos);
        hasRealFrameProducerTime.set(true);
    }

    long takeRealFrameProducerTimeNanos() {
        if (hasRealFrameProducerTime.compareAndSet(true, false)) {
            return realFrameProducerTimeNanos.get();
        }
        return clock.nanoTime();
    }

    void beginDispatchFrameGenerationBatch() {
        beginPhase(Phase.DISPATCH_FRAME_GENERATION_BATCH);
    }

    void endDispatchFrameGenerationBatch() {
        endPhase(Phase.DISPATCH_FRAME_GENERATION_BATCH);
    }

    void beginPresentFrameBatch(
            boolean waited, boolean pacingEnabled, int generatedCount, long intervalNanos
    ) {
        beginPresentFrameBatch(waited, pacingEnabled, generatedCount, intervalNanos, 0);
    }

    /**
     * Starts presenting a batch. {@code backlogFrames} counts the real frames already queued
     * behind it (generated or waiting for frame generation).
     *
     * <p>A queued frame means the presenter runs a whole frame or more behind production, and
     * every queued frame is display latency Reflex cannot see: its in-band present is the handoff
     * to frame generation. The interval comes from the production period, so once a backlog
     * formed nothing drained it but the 1/16-per-frame catch-up below, and while the queues were
     * full they throttled production to the presenter's own pace, which the period estimate then
     * measured. Coming out of a slow stretch (43 ms frames, then 28 ms) the real frame was shown
     * 130-210 ms after rendering for seconds instead of ~42 ms (26.3 Vulkan, DLSS-G X2). So a
     * batch with frames queued behind it is presented on a shorter interval, which drains the
     * queue within a few frames while still spacing the batch evenly.</p>
     */
    void beginPresentFrameBatch(
            boolean waited, boolean pacingEnabled, int generatedCount, long intervalNanos, int backlogFrames
    ) {
        beginPhase(Phase.PRESENT_FRAME_BATCH);
        presentIntervalNanos = backlogFrames <= 0 ? intervalNanos
                : backlogFrames == 1 ? intervalNanos * BACKLOG_ONE_FRAME_INTERVAL_NUMERATOR / 4L
                : intervalNanos / 2L;
        beginBatch(waited, pacingEnabled, generatedCount);
    }

    void endPresentFrameBatch() {
        endPhase(Phase.PRESENT_FRAME_BATCH);
    }

    void beginPresentFrame() {
        beginPhase(Phase.PRESENT_FRAME);
    }

    void endPresentFrame(boolean advanceDeadline) {
        endPhase(Phase.PRESENT_FRAME);
        if (advanceDeadline) {
            advance(presentIntervalNanos);
        }
    }

    void endPresentFrame() {
        endPresentFrame(true);
    }

    /**
     * Moves the pending deadline up to now when the image only became presentable after
     * it, so the images behind it keep their full interval instead of bunching up behind
     * a late GPU completion. A batch whose timeline was just reset is anchored here.
     */
    void imageReady() {
        if (nextDeadlineNanos == 0L) {
            return;
        }
        long now = clock.nanoTime();
        if (now - nextDeadlineNanos > 0L) {
            nextDeadlineNanos = now;
        }
    }

    /**
     * Waits until the image at {@code position} of a batch of {@code batchSize} images should be
     * presented.
     *
     * <p>The frame reaches the compositor when vkQueuePresentKHR returns, and a later image's
     * present can block much longer than the batch's first. On NVIDIA under Wayland the first
     * goes out right after frame generation, while the GPU is idle, and returned in 0.2 ms. The
     * real frame goes out in the middle of Minecraft's next frame, where the driver's present
     * work waits for a GPU timeslice: 3-4.5 ms (2560x1380, RTX 4060 Laptop, X2). Presents called
     * on an even timeline thus arrived 14.7 and 9.8 ms apart instead of 12.3, and at 240 Hz the
     * shown frames alternated between 2 and 4 refreshes. So each later image starts early by how
     * much longer its present blocks than the first image's. The block is measured at the lead
     * in use, so this settles where every present returns one first-image block after its
     * deadline, however the driver's block responds to presenting earlier. With Reflex this
     * also kept the driver out of a slow pacing state the late real present pushed it into
     * (30 to 39-41 real fps; 240 Hz judder 52-60% to 28-42%).</p>
     */
    void sleepAtPresentImage(int batchSize, int position) {
        if (nextDeadlineNanos != 0L) {
            deadlineWaiter.accept(nextDeadlineNanos - presentLeadNanos(batchSize, position));
        }
    }

    /** Records how long the present of the image at {@code position} of a paced batch blocked. */
    void recordPresentBlock(int batchSize, int position, long blockedNanos) {
        if (batchSize < 1 || batchSize > MAX_TRACKED_BATCH_SIZE || position < 0 || position >= batchSize) {
            return;
        }
        // A hitch must not drag the lead for the next many frames.
        long sample = Math.min(Math.max(0L, blockedNanos),
                Math.max(presentIntervalNanos, MIN_PRESENT_BLOCK_SAMPLE_CAP_NANOS));
        long[] averages = presentBlockNanos[batchSize];
        long previous = averages[position];
        averages[position] = previous < 0L
                ? sample
                : previous + (sample - previous) / PRESENT_BLOCK_AVERAGE_DIVISOR;
    }

    long presentLeadNanos(int batchSize, int position) {
        if (position <= 0 || batchSize > MAX_TRACKED_BATCH_SIZE || position >= batchSize) {
            return 0L;
        }
        long[] averages = presentBlockNanos[batchSize];
        if (averages[0] < 0L || averages[position] < 0L) {
            return 0L;
        }
        return Math.max(0L, Math.min(averages[position] - averages[0], presentIntervalNanos / 2L));
    }

    long lastPhaseDurationNanos(Phase phase) {
        return lastPhaseDurations.get(phase.ordinal());
    }

    void reset() {
        nextDeadlineNanos = 0L;
        previousPacingEnabled = false;
        previousGeneratedCount = -1;
    }

    private void beginBatch(boolean waited, boolean pacingEnabled, int generatedCount) {
        long now = clock.nanoTime();
        if (!pacingEnabled) {
            nextDeadlineNanos = 0L;
            previousPacingEnabled = false;
            previousGeneratedCount = generatedCount;
            return;
        }
        boolean resetTimeline = waited
                || !previousPacingEnabled
                || previousGeneratedCount != generatedCount
                || nextDeadlineNanos == 0L;
        if (resetTimeline) {
            nextDeadlineNanos = now;
        } else {
            // The batch was queued before the previous one finished: the presenter is behind.
            // Start a little early so the backlog drains rather than settling at a period the
            // backlog itself is throttling production to.
            nextDeadlineNanos = Math.max(now,
                    nextDeadlineNanos - presentIntervalNanos / BACKLOG_CATCH_UP_DIVISOR);
        }
        previousPacingEnabled = true;
        previousGeneratedCount = generatedCount;
    }

    private static long[][] newPresentBlockAverages() {
        long[][] averages = new long[MAX_TRACKED_BATCH_SIZE + 1][];
        for (int batchSize = 0; batchSize <= MAX_TRACKED_BATCH_SIZE; batchSize++) {
            averages[batchSize] = new long[batchSize];
            Arrays.fill(averages[batchSize], -1L);
        }
        return averages;
    }

    private void sleepUntil(long targetNanos) {
        while (true) {
            long remaining = targetNanos - clock.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            if (remaining > FINAL_SPIN_WINDOW_NANOS) {
                LockSupport.parkNanos(remaining - FINAL_SPIN_WINDOW_NANOS);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private void advance(long intervalNanos) {
        if (nextDeadlineNanos == 0L) {
            return;
        }
        nextDeadlineNanos += intervalNanos;
        long lateBy = clock.nanoTime() - nextDeadlineNanos;
        if (lateBy > Math.max(intervalNanos * 4L, MAX_PRESENT_INTERVAL_NANOS)) {
            nextDeadlineNanos = clock.nanoTime();
        }
    }

    private void beginPhase(Phase phase) {
        PhaseStarts starts = phaseStarts.get();
        int index = phase.ordinal();
        starts.startedAtNanos[index] = clock.nanoTime();
        starts.active[index] = true;
    }

    private void endPhase(Phase phase) {
        endPhase(phase, clock.nanoTime());
    }

    private void endPhase(Phase phase, long endedAtNanos) {
        PhaseStarts starts = phaseStarts.get();
        int index = phase.ordinal();
        if (!starts.active[index]) {
            return;
        }
        long elapsedNanos = endedAtNanos - starts.startedAtNanos[index];
        lastPhaseDurations.set(index, Math.max(0L, elapsedNanos));
        starts.active[index] = false;
    }

    enum Phase {
        REAL_FRAME_RENDERING,
        DISPATCH_FRAME_GENERATION_BATCH,
        PRESENT_FRAME_BATCH,
        PRESENT_FRAME
    }

    private static final class PhaseStarts {
        private final long[] startedAtNanos = new long[Phase.values().length];
        private final boolean[] active = new boolean[Phase.values().length];
    }
}
