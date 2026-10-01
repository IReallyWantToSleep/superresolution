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

import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;

/** Pacing state used directly by the application-managed present worker. */
final class PresentPacer {
    private static final long MAX_PRESENT_INTERVAL_NANOS = 100_000_000L;
    private static final long FINAL_SPIN_WINDOW_NANOS = 200_000L;

    private final AsyncFramePresenter.NanoClock clock;
    private final LongConsumer deadlineWaiter;
    private long nextDeadlineNanos;
    private boolean previousPacingEnabled;
    private int previousGeneratedCount = -1;

    PresentPacer(AsyncFramePresenter.NanoClock clock) {
        this.clock = clock;
        this.deadlineWaiter = this::sleepUntil;
    }

    PresentPacer(AsyncFramePresenter.NanoClock clock, LongConsumer deadlineWaiter) {
        this.clock = clock;
        this.deadlineWaiter = deadlineWaiter;
    }

    void reset() {
        nextDeadlineNanos = 0L;
        previousPacingEnabled = false;
        previousGeneratedCount = -1;
    }

    void beginBatch(boolean waited, boolean pacingEnabled, int generatedCount) {
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
        }
        previousPacingEnabled = true;
        previousGeneratedCount = generatedCount;
    }

    void awaitNextImage() {
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

    void advance(long intervalNanos) {
        if (nextDeadlineNanos == 0L) {
            return;
        }
        nextDeadlineNanos += intervalNanos;
        long lateBy = clock.nanoTime() - nextDeadlineNanos;
        if (lateBy > Math.max(intervalNanos * 4L, MAX_PRESENT_INTERVAL_NANOS)) {
            nextDeadlineNanos = clock.nanoTime();
        }
    }
}
