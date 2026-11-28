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
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.common.perf.FrameRateLog;

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
 * <p>The cap is also all that keeps latency down. Reflex's own sleep does not hold frames back
 * in this pipeline: whenever the cap stopped binding, the sleep fell to 0.1 ms and the time from
 * simulation start to the end of the frame's GPU work rose from about one frame to 1.7 frames
 * (25-29 ms against 45-58 ms; FG X2, 26.3 Vulkan backend). So a window only counts as keeping up
 * when the frames also slept (see {@link #PACED_SLEEP_FRACTION}): frames that land on the cap
 * without sleeping were held back by the GPU instead, through frames queued ahead of them, and
 * that window's period is what the GPU sustains.</p>
 *
 * <p>Works on windows of {@link #WINDOW_FRAMES} frame starts. A cap that holds is probed
 * downwards to notice the GPU speeding up: in 1% steps while still above the remembered
 * sustained period, in growing steps once below it. A probe the frames cannot hold, falling
 * behind or spreading wider than {@link #SPREAD_TOLERANCE}, returns to the cap that held before
 * it, and pushes the next probe further out. Only frames falling behind a cap that held mean
 * the GPU slowed down: then the measured period becomes the sustained one and the cap that plus
 * {@link #HEADROOM}, after {@link #BEHIND_WINDOWS} such windows in a row so that one hitch does
 * not count. Frames that keep up but spread wide otherwise leave the cap alone: raising
 * a cap the frames already meet cannot even them out. Raising it there, and on failed probes,
 * held the cap far above what the GPU sustains (it climbed from 25 ms to 38 ms against frames
 * that ran at 23 ms when the cap allowed it). After the first measurement since a reset, the
 * cap descends in growing steps every window until a probe fails ({@link #DESCENT_STEP}), and
 * the window after a reset or a pause is discarded. A later re-measure only holds: it measured
 * frames the GPU held back, which is what the GPU sustains, and descending from it ran probes
 * below that limit every window. A descent that ends in a failed probe is started again, up to
 * {@link #DESCENT_RESTARTS} times, and a probe whose frames slept yet fell behind even the cap
 * before it is a hitch rather than a failure. A probe whose frames fall behind ran as fast as
 * the GPU allows, so the cap lands on that period plus the headroom: when they did not sleep,
 * whatever the cap before it, otherwise only when that is below the cap before the probe. While
 * the user's own limit (or Minecraft's AFK limit) paces the frames, the cap stays as it is, for
 * when the limit lifts.</p>
 */
final class AdaptiveFrameCap {
    private static final int WINDOW_FRAMES = 40;
    // Descending windows are shorter: at the 40 ms frames of a world that just loaded, a full
    // window takes 1.6 s, and the descent needs one per step. So are windows that measure without
    // a cap, and the discarded window after a reset or a pause, which at the 70-150 ms frames of
    // a world still loading took 4-6 s together.
    private static final int SHORT_WINDOW_FRAMES = 20;
    // Statistics drop this share of the shortest and longest periods, so one hitch neither
    // raises the cap nor reads as a failed probe.
    private static final int TRIM_DIVISOR = 10;
    private static final double HEADROOM = 1.04;
    // A binding limiter lands within a fraction of a percent of its interval...
    private static final double HOLD_TOLERANCE = 1.015;
    // ...and without the sawtooth's long frames.
    private static final double SPREAD_TOLERANCE = 1.05;
    // A window's frames count as paced by the cap when their median Reflex sleep is at least this
    // share of the cap, and at least MIN_PACED_SLEEP_NANOS. A binding cap leaves the frames the
    // cap minus their CPU time to sleep (15-20 ms of 25 ms GPU-bound), and still the headroom when
    // CPU-bound; a cap the GPU holds the frames behind leaves them ~0.1 ms, the call itself.
    private static final double PACED_SLEEP_FRACTION = 0.01;
    private static final long MIN_PACED_SLEEP_NANOS = 200_000L;
    private static final double MIN_PROBE_STEP = 0.01;
    private static final double MAX_PROBE_STEP = 0.08;
    private static final int BASE_PROBE_DELAY_WINDOWS = 3;
    private static final int MAX_PROBE_DELAY_WINDOWS = 12;
    // A cap that held is only re-measured after this many windows in a row fall behind it: one
    // slow window (a chunk load, a hitch) would otherwise lift it by the headroom and more.
    private static final int BEHIND_WINDOWS = 2;
    // After a re-measure the cap descends fast: a probe every window, starting at this step and
    // doubling up to MAX_PROBE_STEP, until one fails. The first measurement on entering a world
    // includes its loading hitches (40 ms against frames that settle at 23 ms), and 1% probes
    // behind a probe delay took 15-25 s to undo it, far longer under a heavier modpack.
    private static final double DESCENT_STEP = 0.04;
    // Descent steps double up to this. At 8%, each 20-frame window lowered the cap by about 4 ms
    // a second whatever the cap, so a descent from the 65 ms measured as a world loaded took
    // 10-12 s to reach the 24 ms the GPU had been sustaining for most of that time. A step that
    // overshoots costs one window behind, and lands the cap on what the GPU sustained in it.
    private static final double MAX_DESCENT_STEP = 0.25;
    // A descending probe whose frames keep up but spread wide is retried this many times before
    // it counts as failed.
    private static final int DESCENT_UNEVEN_RETRIES = 2;
    // A descent that ends in a failed probe is started again this many times, after the base
    // probe delay, before probing falls back to small steps behind a growing delay. Right after
    // a world loads, chunk loading failed descents, and the doubled delays plus 1% steps then
    // held the cap at 32.8-34.7 ms for 15-26 s against frames that settled at 23 ms.
    private static final int DESCENT_RESTARTS = 2;
    // Short windows a restarted descent waits first. Three full windows at the 66 ms frames of a
    // world still loading held a failed descent for 8 s.
    private static final int DESCENT_RESTART_DELAY_WINDOWS = 2;
    private static final long MAX_PERIOD_NANOS = 250_000_000L;

    private final long[] periods = new long[WINDOW_FRAMES];
    // The Reflex sleep that ended each of those periods.
    private final long[] sleeps = new long[WINDOW_FRAMES];
    private int windowFrames;
    private long lastFrameStartNanos;
    private long capNanos;
    private long sustainedNanos;
    private int holdingWindows;
    private int probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
    private double probeStep = MIN_PROBE_STEP;
    private boolean probing;
    private long capBeforeProbeNanos;
    private int behindWindows;
    private boolean descending;
    private int unevenDescents;
    private int descentRestarts;
    private boolean resumeDescent;
    // The first window after a reset or a pause holds the hitches around it.
    private boolean discardWindow = true;
    private long lastUserIntervalNanos;

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
        capBeforeProbeNanos = 0L;
        behindWindows = 0;
        descending = false;
        unevenDescents = 0;
        descentRestarts = 0;
        resumeDescent = false;
        discardWindow = true;
        lastUserIntervalNanos = 0L;
    }

    /**
     * Records the start of one real frame.
     *
     * @param userIntervalNanos the user's own frame limit as a real-frame interval, 0 if none
     * @param sleptNanos        how long the Reflex sleep right before this frame start took
     */
    void onFrameStart(long nowNanos, long userIntervalNanos, long sleptNanos) {
        long previous = lastFrameStartNanos;
        lastFrameStartNanos = nowNanos;
        if (userIntervalNanos != lastUserIntervalNanos) {
            // A window across a change of the user's limit measures neither side. Across the AFK
            // limit lifting it read the limit's 66 ms frames as what the GPU sustains.
            lastUserIntervalNanos = userIntervalNanos;
            windowFrames = 0;
            return;
        }
        if (previous == 0L) {
            return;
        }
        long period = nowNanos - previous;
        if (period <= 0L || period > MAX_PERIOD_NANOS) {
            // A pause says nothing about steady throughput, nor do the frames right after it.
            windowFrames = 0;
            discardWindow = true;
            return;
        }
        sleeps[windowFrames] = Math.max(0L, sleptNanos);
        periods[windowFrames++] = period;
        int length = descending || resumeDescent || capNanos == 0L || discardWindow
                ? SHORT_WINDOW_FRAMES : WINDOW_FRAMES;
        if (windowFrames < length) {
            return;
        }
        windowFrames = 0;
        if (discardWindow) {
            discardWindow = false;
            return;
        }
        long[] sorted = Arrays.copyOf(periods, length);
        Arrays.sort(sorted);
        int trim = length / TRIM_DIVISOR;
        long sum = 0L;
        for (int index = trim; index < length - trim; index++) {
            sum += sorted[index];
        }
        // A trimmed mean rather than the median: the sawtooth skews short, and what the GPU
        // sustains is time over frames.
        long typicalNanos = sum / (length - 2L * trim);
        long[] sortedSleeps = Arrays.copyOf(sleeps, length);
        Arrays.sort(sortedSleeps);
        evaluate(typicalNanos, sorted[length - trim - 1], sortedSleeps[length / 2], userIntervalNanos);
    }

    private void evaluate(long typicalNanos, long p90Nanos, long medianSleepNanos, long userIntervalNanos) {
        long previousCap = capNanos;
        // Frames on the cap that did not sleep were held back by frames queued ahead of them,
        // which is latency the period cannot show: in 26.3 Vulkan FG sessions the cap sat within
        // the hold tolerance above such frames for minutes, at 1.7 frames of latency.
        boolean paced = capNanos != 0L && medianSleepNanos >= Math.max(MIN_PACED_SLEEP_NANOS,
                Math.round(capNanos * PACED_SLEEP_FRACTION));
        boolean keepsUp = paced && typicalNanos <= capNanos * HOLD_TOLERANCE;
        boolean even = p90Nanos <= capNanos * SPREAD_TOLERANCE;
        if (keepsUp) {
            behindWindows = 0;
        }
        String decision;
        if (userIntervalNanos > 0L && typicalNanos <= userIntervalNanos * HOLD_TOLERANCE) {
            // The user's own limit (or Minecraft's AFK limit) already paces every frame, which
            // says nothing about the GPU. Keep the cap that held for when the limit lifts: cleared,
            // the cap had to be found again from the limit's 66 ms, which took 7-11 s every time
            // the AFK limit lifted.
            if (probing) {
                capNanos = capBeforeProbeNanos;
                probing = false;
            }
            holdingWindows = 0;
            behindWindows = 0;
            unevenDescents = 0;
            decision = "user_limit";
        } else if (probing && paced && !keepsUp && typicalNanos > capBeforeProbeNanos * HOLD_TOLERANCE) {
            // Behind even the cap that held before the probe while the frames still slept: a
            // hitch, which says nothing about the probe. Go back without counting it as failed; a
            // descent carries on. Frames that stopped sleeping instead filled the queue the probe
            // opened, and reverting to a cap the GPU no longer meets kept them queued.
            capNanos = capBeforeProbeNanos;
            probing = false;
            unevenDescents = 0;
            holdingWindows = 0;
            decision = "probe_hitch";
        } else if (descending && probing && keepsUp && !even && ++unevenDescents <= DESCENT_UNEVEN_RETRIES) {
            // Mid-descent the frames keep up but spread wide: a loading hitch as likely as the
            // limit. Try the same cap again rather than ending the descent.
            decision = "descend_uneven";
        } else if (descending && probing && (!keepsUp || !even) && descentRestarts > 0) {
            // Soon after the first measurement the GPU is often still settling (chunk loading after
            // a world loads): hold the last cap that kept up and descend again shortly.
            descentRestarts--;
            capNanos = capAfterFailedProbe(keepsUp, paced, typicalNanos);
            probeDelayWindows = DESCENT_RESTART_DELAY_WINDOWS;
            probeStep = MIN_PROBE_STEP;
            probing = false;
            descending = false;
            resumeDescent = true;
            unevenDescents = 0;
            holdingWindows = 0;
            decision = "descend_restart";
        } else if (probing && (!keepsUp || !even)) {
            // The probe asked for more than the GPU gives: back to the cap that held before it,
            // or to what the GPU sustained in its window (see capAfterFailedProbe).
            capNanos = capAfterFailedProbe(keepsUp, paced, typicalNanos);
            probeDelayWindows = Math.min(probeDelayWindows * 2, MAX_PROBE_DELAY_WINDOWS);
            probeStep = MIN_PROBE_STEP;
            probing = false;
            descending = false;
            resumeDescent = false;
            unevenDescents = 0;
            holdingWindows = 0;
            decision = keepsUp ? "probe_uneven" : paced ? "probe_behind" : "probe_unpaced";
        } else if (!keepsUp && capNanos != 0L && ++behindWindows < BEHIND_WINDOWS) {
            // Noise until the next window says otherwise: it neither counts towards a probe nor
            // restarts the count, or periodic slow windows would keep the cap from ever probing.
            decision = paced ? "behind_once" : "unpaced_once";
        } else if (!keepsUp) {
            // No cap yet, or the GPU no longer keeps up with a cap that held: this window is what
            // it sustains now. The new cap sits well clear of the limit, so probing resumes soon.
            // Only the first measurement since a reset descends, as it can include a world's
            // loading hitches. A later one measured frames the GPU held back: descending from
            // there probed below the GPU's limit every window and kept frames queued, which
            // reloading the shader pack (a reset) was the only way out of.
            behindWindows = 0;
            sustainedNanos = typicalNanos;
            capNanos = Math.round(typicalNanos * HEADROOM);
            probeStep = MIN_PROBE_STEP;
            probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
            descending = previousCap == 0L;
            descentRestarts = descending ? DESCENT_RESTARTS : 0;
            resumeDescent = false;
            holdingWindows = 0;
            decision = previousCap == 0L ? "measure" : paced ? "behind" : "unpaced";
        } else if (!even) {
            // The frames meet the cap but spread wide. That is not the cap's to fix: keep it,
            // without restarting the count towards the next probe.
            decision = "uneven";
        } else if (descending || probing || ++holdingWindows >= probeDelayWindows) {
            if (resumeDescent) {
                resumeDescent = false;
                descending = true;
            }
            if (descending) {
                probeStep = probing ? Math.min(probeStep * 2.0, MAX_DESCENT_STEP) : DESCENT_STEP;
            } else if (probing && capNanos < sustainedNanos) {
                // Faster than the GPU last managed and still holding: it sped up, search quickly.
                probeStep = Math.min(probeStep * 2.0, MAX_PROBE_STEP);
                probeDelayWindows = BASE_PROBE_DELAY_WINDOWS;
            }
            capBeforeProbeNanos = capNanos;
            capNanos = Math.round(capNanos * (1.0 - probeStep));
            unevenDescents = 0;
            probing = true;
            holdingWindows = 0;
            decision = descending ? "descend" : "probe";
        } else {
            decision = "hold";
        }
        if (capNanos != previousCap) {
            SuperResolution.LOGGER.debug("Frame generation frame cap: {} us (window {} us, p90 {} us)",
                    capMicros(), typicalNanos / 1000L, p90Nanos / 1000L);
        }
        FrameRateLog.onFrameCapDecision(decision, capMicros());
        if (FramePacingTrace.INSTANCE.isRecording()) {
            FramePacingTrace.INSTANCE.instant("adaptive_frame_cap", FramePacingTrace.Context.empty(), "complete",
                    decision + " cap_us=" + capMicros() + " window_us=" + typicalNanos / 1000L
                            + " p90_us=" + p90Nanos / 1000L + " sustained_us=" + sustainedNanos / 1000L
                            + " sleep_us=" + medianSleepNanos / 1000L);
        }
    }

    /**
     * The cap after a failed probe. Frames that kept up but spread wide return to the cap that held
     * before it. Frames that fell behind ran as fast as the GPU allows, so their period plus the
     * headroom is what it sustains, which after a long descent step can sit well below the cap
     * before the probe. If they also stopped sleeping, the GPU held them back for the whole window,
     * and that period plus the headroom applies even above the cap before the probe: the frames
     * that probe queued only drain under a cap the GPU can beat.
     */
    private long capAfterFailedProbe(boolean keepsUp, boolean paced, long typicalNanos) {
        if (keepsUp) {
            return capBeforeProbeNanos;
        }
        sustainedNanos = typicalNanos;
        long sustainedCap = Math.round(typicalNanos * HEADROOM);
        return paced ? Math.min(capBeforeProbeNanos, sustainedCap) : sustainedCap;
    }
}
