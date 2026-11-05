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

import io.homo.superresolution.common.SuperResolution;

import java.util.Arrays;

/**
 * Holds the real-frame rate a few percent under what the GPU sustains while frame generation
 * runs with Reflex.
 *
 * <p>GPU-bound, Reflex starts each frame on its own prediction of when the GPU frees up, and
 * with frame generation that prediction runs a sawtooth: a few ~21 ms frames against a GPU that
 * needs ~24 ms, the queue builds, then 25-33 ms frames while it drains (measured at 2560x1380 on
 * an RTX 4060 Laptop: real-frame period sd 4.3 ms). With Reflex's limiter binding instead, a
 * whole window of frames lands within 0.1 ms of the interval, and frame generation spaces its
 * output just as evenly.</p>
 *
 * <p>Works on windows of {@link #WINDOW_FRAMES} frame starts. A cap the frames do not hold is
 * replaced by the measured period plus {@link #HEADROOM}, and that period is remembered as what
 * the GPU sustains. A cap that holds is probed downwards to notice the GPU speeding up: in 1%
 * steps while still above the remembered period, in growing steps once below it, and each
 * failed probe pushes the next one further out.</p>
 */
final class AdaptiveFrameCap {
    private static final int WINDOW_FRAMES = 40;
    // Statistics drop this many of the shortest and longest periods, so one hitch neither
    // raises the cap nor reads as a failed probe.
    private static final int TRIM_FRAMES = WINDOW_FRAMES / 10;
    private static final double HEADROOM = 1.04;
    // A binding limiter lands within a fraction of a percent of its interval...
    private static final double HOLD_TOLERANCE = 1.015;
    // ...and without the sawtooth's long frames.
    private static final double SPREAD_TOLERANCE = 1.05;
    private static final double MIN_PROBE_STEP = 0.01;
    private static final double MAX_PROBE_STEP = 0.08;
    private static final int BASE_PROBE_DELAY_WINDOWS = 3;
    private static final int MAX_PROBE_DELAY_WINDOWS = 12;
    private static final long MAX_PERIOD_NANOS = 250_000_000L;

    private final long[] periods = new long[WINDOW_FRAMES];
    private int windowFrames;
    private long lastFrameStartNanos;
    private long capNanos;
    private long sustainedNanos;
    private int holdingWindows;
    private int probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
    private double probeStep = MIN_PROBE_STEP;
    private boolean probing;

    /** The real-frame interval to enforce, in microseconds; 0 when no cap applies. */
    int capMicros() {
        return (int) Math.min(Integer.MAX_VALUE, (capNanos + 999L) / 1000L);
    }

    void reset() {
        windowFrames = 0;
        lastFrameStartNanos = 0L;
        capNanos = 0L;
        sustainedNanos = 0L;
        holdingWindows = 0;
        probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
        probeStep = MIN_PROBE_STEP;
        probing = false;
    }

    /**
     * Records the start of one real frame.
     *
     * @param userIntervalNanos the user's own frame limit as a real-frame interval, 0 if none
     */
    void onFrameStart(long nowNanos, long userIntervalNanos) {
        long previous = lastFrameStartNanos;
        lastFrameStartNanos = nowNanos;
        if (previous == 0L) {
            return;
        }
        long period = nowNanos - previous;
        if (period <= 0L || period > MAX_PERIOD_NANOS) {
            // A pause says nothing about steady throughput.
            windowFrames = 0;
            return;
        }
        periods[windowFrames++] = period;
        if (windowFrames < WINDOW_FRAMES) {
            return;
        }
        windowFrames = 0;
        long[] sorted = periods.clone();
        Arrays.sort(sorted);
        long sum = 0L;
        for (int index = TRIM_FRAMES; index < WINDOW_FRAMES - TRIM_FRAMES; index++) {
            sum += sorted[index];
        }
        // A trimmed mean rather than the median: the sawtooth skews short, and what the GPU
        // sustains is time over frames.
        long typicalNanos = sum / (WINDOW_FRAMES - 2L * TRIM_FRAMES);
        evaluate(typicalNanos, sorted[WINDOW_FRAMES - TRIM_FRAMES - 1], userIntervalNanos);
    }

    private void evaluate(long typicalNanos, long p90Nanos, long userIntervalNanos) {
        long previousCap = capNanos;
        boolean holding = capNanos != 0L
                && typicalNanos <= capNanos * HOLD_TOLERANCE
                && p90Nanos <= capNanos * SPREAD_TOLERANCE;
        if (userIntervalNanos > 0L && typicalNanos <= userIntervalNanos * HOLD_TOLERANCE) {
            // The user's own limit (or Minecraft's AFK limit) already paces every frame.
            capNanos = 0L;
            holdingWindows = 0;
            probing = false;
        } else if (!holding) {
            // No cap yet, or the GPU no longer keeps up with it: this window is what it sustains.
            if (probing) {
                probeDelayWindows = Math.min(probeDelayWindows * 2, MAX_PROBE_DELAY_WINDOWS);
            }
            sustainedNanos = typicalNanos;
            capNanos = Math.round(typicalNanos * HEADROOM);
            probeStep = MIN_PROBE_STEP;
            probing = false;
            holdingWindows = 0;
        } else if (probing || ++holdingWindows >= probeDelayWindows) {
            if (probing && capNanos < sustainedNanos) {
                // Faster than the GPU last managed and still holding: it sped up, search quickly.
                probeStep = Math.min(probeStep * 2.0, MAX_PROBE_STEP);
                probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
            }
            capNanos = Math.round(capNanos * (1.0 - probeStep));
            probing = true;
            holdingWindows = 0;
        }
        if (capNanos != previousCap) {
            SuperResolution.LOGGER.debug("Frame generation frame cap: {} us (window {} us, p90 {} us)",
                    capMicros(), typicalNanos / 1000L, p90Nanos / 1000L);
        }
    }
}
