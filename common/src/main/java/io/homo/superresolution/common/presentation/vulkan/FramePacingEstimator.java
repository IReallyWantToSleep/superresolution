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

import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchResult;
import io.homo.superresolution.common.SuperResolution;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * FG-thread-owned estimator for real-frame production cadence.
 */
final class FramePacingEstimator {
    private static final double EMA_ALPHA = 0.2;
    private static final double SAMPLE_EMA_MIN_FACTOR = 0.5;
    private static final double SAMPLE_EMA_MAX_FACTOR = 2.0;
    private static final double INVALID_FPS_DEVIATION = 0.25;
    private static final double TRIM_FRACTION = 0.2;
    private static final long DEFAULT_REAL_PERIOD_NANOS = 16_666_667L;
    private static final long MIN_REAL_PERIOD_NANOS = 1_000_000L;
    private static final long MAX_REAL_PERIOD_NANOS = 500_000_000L;
    private static final long MIN_CALIBRATION_PRODUCER_SPAN_NANOS = 250_000_000L;
    private static final long MAX_CALIBRATION_WALL_TIME_NANOS = 2_000_000_000L;
    private static final int PREFERRED_CALIBRATION_SAMPLES = 16;
    private static final int MIN_TIMEOUT_CALIBRATION_SAMPLES = 8;
    private static final int OBSERVATION_WINDOW_SIZE = 16;
    private static final int REQUIRED_MISMATCH_WINDOWS = 2;
    private static final int REQUIRED_REAL_ONLY_BATCHES = 2;

    enum State {
        CALIBRATING,
        TRACKING
    }

    private enum BatchMode {
        UNKNOWN,
        GENERATED,
        REAL_ONLY
    }

    private final String providerId;
    private final FramePacingTiming timing;
    private final LongSupplier wallClock;
    private final List<Long> calibrationSamples = new ArrayList<>();
    private final Deque<Long> observationWindow = new ArrayDeque<>();

    private State state;
    private int plannedGeneratedCount = -1;
    private long swapchainGeneration = Long.MIN_VALUE;
    private long currentProducerTimeNanos;
    private long previousProducerTimeNanos;
    private long calibrationFirstProducerTimeNanos;
    private long calibrationStartWallNanos;
    private long calibrationStartExcludedWaitNanos;
    private boolean hasPreviousProducerTime;
    private boolean calibrationStarted;
    private boolean invalidatedThisFrame;
    private double realPeriodEmaNanos;
    private int consecutiveMismatchWindows;
    private BatchMode confirmedBatchMode = BatchMode.UNKNOWN;
    private int consecutiveRealOnlyBatches;

    FramePacingEstimator(
            String providerId,
            FramePacingTiming timing,
            LongSupplier wallClock
    ) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId cannot be blank");
        }
        if (timing == null || wallClock == null) {
            throw new IllegalArgumentException("pacing estimator dependencies cannot be null");
        }
        this.providerId = providerId;
        this.timing = timing;
        this.wallClock = wallClock;
        beginCalibration(
                "application-managed provider lifecycle created",
                0L,
                false
        );
    }

    long observeRealFrame(
            RealFrameJob job,
            long currentSwapchainGeneration
    ) {
        if (job == null) {
            throw new IllegalArgumentException("job cannot be null");
        }
        long wallNow = wallClock.getAsLong();
        currentProducerTimeNanos = job.producerTimeNanos();
        invalidatedThisFrame = false;

        List<String> invalidationReasons = new ArrayList<>(3);
        boolean plannedCountChanged = false;
        if (plannedGeneratedCount < 0) {
            plannedGeneratedCount = job.plannedGeneratedCount();
        } else if (plannedGeneratedCount != job.plannedGeneratedCount()) {
            invalidationReasons.add(
                    "planned generated count changed from "
                            + plannedGeneratedCount + " to " + job.plannedGeneratedCount()
            );
            plannedGeneratedCount = job.plannedGeneratedCount();
            plannedCountChanged = true;
        }

        if (swapchainGeneration == Long.MIN_VALUE) {
            swapchainGeneration = currentSwapchainGeneration;
        } else if (swapchainGeneration != currentSwapchainGeneration) {
            invalidationReasons.add(
                    "swapchain generation changed from "
                            + swapchainGeneration + " to " + currentSwapchainGeneration
            );
            swapchainGeneration = currentSwapchainGeneration;
        }

        if (job.historyResetRequested()) {
            invalidationReasons.add("frame-generation history reset requested");
        }

        if (!invalidationReasons.isEmpty()) {
            if (plannedCountChanged) {
                resetBatchModeObservation();
            }
            beginCalibration(
                    String.join("; ", invalidationReasons),
                    currentProducerTimeNanos,
                    true
            );
            invalidatedThisFrame = true;
        } else {
            observeProducerTime(currentProducerTimeNanos, wallNow);
        }
        return estimatedRealPeriodNanos();
    }

    boolean onBatchResult(
            int generatedCount,
            @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        if (generatedCount < 0) {
            throw new IllegalArgumentException("generatedCount cannot be negative");
        }

        if (historyDisposition != null
                && historyDisposition
                != AsyncFrameGenerationDispatchResult.HistoryDisposition.UNCHANGED) {
            invalidateCurrentFrame(
                    "provider history disposition changed to " + historyDisposition
            );
        }

        if (plannedGeneratedCount <= 0) {
            resetBatchModeObservation();
            return false;
        }

        boolean generated = generatedCount > 0;
        if (generated) {
            consecutiveRealOnlyBatches = 0;
            if (confirmedBatchMode == BatchMode.REAL_ONLY) {
                confirmedBatchMode = BatchMode.GENERATED;
                invalidateCurrentFrame(
                        "batch state changed from sustained Real-only fallback to generated"
                );
            } else if (confirmedBatchMode == BatchMode.UNKNOWN) {
                confirmedBatchMode = BatchMode.GENERATED;
            }
        } else {
            consecutiveRealOnlyBatches++;
            if (consecutiveRealOnlyBatches >= REQUIRED_REAL_ONLY_BATCHES
                    && confirmedBatchMode != BatchMode.REAL_ONLY) {
                BatchMode previousMode = confirmedBatchMode;
                confirmedBatchMode = BatchMode.REAL_ONLY;
                if (previousMode == BatchMode.GENERATED) {
                    invalidateCurrentFrame(
                            "batch state changed from generated to sustained Real-only fallback"
                    );
                }
            }
        }

        return generated
                && state == State.TRACKING
                && confirmedBatchMode != BatchMode.REAL_ONLY;
    }

    private void observeProducerTime(long producerTimeNanos, long wallNow) {
        if (!hasPreviousProducerTime) {
            startCalibrationIfNeeded(producerTimeNanos, wallNow);
            previousProducerTimeNanos = producerTimeNanos;
            hasPreviousProducerTime = true;
            return;
        }

        long rawSample = producerTimeNanos - previousProducerTimeNanos;
        previousProducerTimeNanos = producerTimeNanos;
        if (rawSample <= 0L) {
            return;
        }
        long sample = clamp(
                rawSample,
                MIN_REAL_PERIOD_NANOS,
                MAX_REAL_PERIOD_NANOS
        );

        if (state == State.CALIBRATING) {
            startCalibrationIfNeeded(producerTimeNanos, wallNow);
            calibrationSamples.add(sample);
            long producerSpan = producerTimeNanos - calibrationFirstProducerTimeNanos;
            long wallTime = wallNow - calibrationStartWallNanos;
            boolean preferredSampleSetReady =
                    calibrationSamples.size() >= PREFERRED_CALIBRATION_SAMPLES
                            && producerSpan >= MIN_CALIBRATION_PRODUCER_SPAN_NANOS;
            boolean timeoutSampleSetReady =
                    wallTime >= MAX_CALIBRATION_WALL_TIME_NANOS
                            && calibrationSamples.size() >= MIN_TIMEOUT_CALIBRATION_SAMPLES;
            if (preferredSampleSetReady || timeoutSampleSetReady) {
                completeCalibration(wallNow);
            }
            return;
        }

        observationWindow.addLast(sample);
        while (observationWindow.size() > OBSERVATION_WINDOW_SIZE) {
            observationWindow.removeFirst();
        }

        if (sample >= realPeriodEmaNanos * SAMPLE_EMA_MIN_FACTOR
                && sample <= realPeriodEmaNanos * SAMPLE_EMA_MAX_FACTOR) {
            realPeriodEmaNanos =
                    realPeriodEmaNanos * (1.0 - EMA_ALPHA) + sample * EMA_ALPHA;
        }

        if (observationWindow.size() == OBSERVATION_WINDOW_SIZE) {
            double observedPeriodNanos = trimmedMean(observationWindow);
            double estimatedFps = nanosToFps(realPeriodEmaNanos);
            double observedFps = nanosToFps(observedPeriodNanos);
            double deviation = Math.abs(estimatedFps - observedFps) / observedFps;
            consecutiveMismatchWindows = deviation > INVALID_FPS_DEVIATION
                    ? consecutiveMismatchWindows + 1
                    : 0;
            if (consecutiveMismatchWindows >= REQUIRED_MISMATCH_WINDOWS) {
                SuperResolution.LOGGER.info(
                        "Frame pacing EMA invalid for provider '{}': estimatedFps={}, "
                                + "observedFps={}, deviation={}%",
                        providerId,
                        format(estimatedFps),
                        format(observedFps),
                        format(deviation * 100.0)
                );
                beginCalibration(
                        "estimated and observed real-frame rates diverged",
                        producerTimeNanos,
                        true
                );
                invalidatedThisFrame = true;
            }
        }
    }

    private void startCalibrationIfNeeded(long producerTimeNanos, long wallNow) {
        if (state != State.CALIBRATING || calibrationStarted) {
            return;
        }
        calibrationStarted = true;
        calibrationFirstProducerTimeNanos = producerTimeNanos;
        calibrationStartWallNanos = wallNow;
        calibrationStartExcludedWaitNanos = timing.excludedWaitNanos();
    }

    private void completeCalibration(long wallNow) {
        realPeriodEmaNanos = trimmedMean(calibrationSamples);
        state = State.TRACKING;
        observationWindow.clear();
        consecutiveMismatchWindows = 0;

        long wallTimeNanos = Math.max(0L, wallNow - calibrationStartWallNanos);
        long excludedWaitNanos = Math.max(
                0L,
                timing.excludedWaitNanos() - calibrationStartExcludedWaitNanos
        );
        double realFps = nanosToFps(realPeriodEmaNanos);
        double targetPresentFps = realFps * (plannedGeneratedCount + 1.0);
        SuperResolution.LOGGER.info(
                "Frame pacing calibration completed for provider '{}': samples={}, "
                        + "wallTimeMs={}, excludedWaitMs={}, realFps={}, targetPresentFps={}",
                providerId,
                calibrationSamples.size(),
                format(wallTimeNanos / 1_000_000.0),
                format(excludedWaitNanos / 1_000_000.0),
                format(realFps),
                format(targetPresentFps)
        );
    }

    private void invalidateCurrentFrame(String reason) {
        if (invalidatedThisFrame) {
            return;
        }
        beginCalibration(reason, currentProducerTimeNanos, true);
        invalidatedThisFrame = true;
    }

    private void beginCalibration(
            String reason,
            long producerTimeNanos,
            boolean hasProducerTime
    ) {
        state = State.CALIBRATING;
        realPeriodEmaNanos = 0.0;
        calibrationSamples.clear();
        observationWindow.clear();
        consecutiveMismatchWindows = 0;
        calibrationStarted = false;
        hasPreviousProducerTime = false;
        previousProducerTimeNanos = 0L;
        calibrationFirstProducerTimeNanos = 0L;
        calibrationStartWallNanos = 0L;
        calibrationStartExcludedWaitNanos = 0L;
        if (hasProducerTime) {
            long wallNow = wallClock.getAsLong();
            startCalibrationIfNeeded(producerTimeNanos, wallNow);
            previousProducerTimeNanos = producerTimeNanos;
            hasPreviousProducerTime = true;
        }
        SuperResolution.LOGGER.info(
                "Frame pacing calibration started for provider '{}': {}",
                providerId,
                reason
        );
    }

    private long estimatedRealPeriodNanos() {
        return state == State.TRACKING && realPeriodEmaNanos > 0.0
                ? Math.round(realPeriodEmaNanos)
                : DEFAULT_REAL_PERIOD_NANOS;
    }

    private void resetBatchModeObservation() {
        confirmedBatchMode = BatchMode.UNKNOWN;
        consecutiveRealOnlyBatches = 0;
    }

    private static double trimmedMean(Iterable<Long> samples) {
        List<Long> sorted = new ArrayList<>();
        for (Long sample : samples) {
            sorted.add(sample);
        }
        if (sorted.isEmpty()) {
            return DEFAULT_REAL_PERIOD_NANOS;
        }
        sorted.sort(Long::compareTo);
        int trimCount = (int) Math.floor(sorted.size() * TRIM_FRACTION);
        int first = trimCount;
        int lastExclusive = sorted.size() - trimCount;
        if (first >= lastExclusive) {
            first = 0;
            lastExclusive = sorted.size();
        }
        double sum = 0.0;
        for (int index = first; index < lastExclusive; index++) {
            sum += sorted.get(index);
        }
        return sum / (lastExclusive - first);
    }

    private static double nanosToFps(double periodNanos) {
        return 1_000_000_000.0 / Math.max(1.0, periodNanos);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
