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
        beginPhase(Phase.PRESENT_FRAME_BATCH);
        presentIntervalNanos = intervalNanos;
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

    void sleepAtPresentGeneratedFrame() {
        awaitNextImage();
    }

    void sleepAtPresentRealFrame() {
        awaitNextImage();
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

    private void awaitNextImage() {
        if (nextDeadlineNanos != 0L) {
            deadlineWaiter.accept(nextDeadlineNanos);
        }
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
