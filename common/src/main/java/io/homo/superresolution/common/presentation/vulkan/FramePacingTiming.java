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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Shared producer clock for application-managed presentation.
 *
 * <p>The raw monotonic clock remains the deadline source for presentation.
 * Producer samples subtract only waits that Super Resolution explicitly
 * introduced on the render thread.</p>
 */
public final class FramePacingTiming {
    private final LongSupplier clock;
    private final AtomicLong excludedWaitNanos = new AtomicLong();

    public FramePacingTiming() {
        this(System::nanoTime);
    }

    FramePacingTiming(LongSupplier clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    public long producerTimeNanos() {
        return clock.getAsLong() - excludedWaitNanos.get();
    }

    public void recordExcludedWait(long waitNanos) {
        if (waitNanos > 0L) {
            excludedWaitNanos.addAndGet(waitNanos);
        }
    }

    public long excludedWaitNanos() {
        return excludedWaitNanos.get();
    }
}
