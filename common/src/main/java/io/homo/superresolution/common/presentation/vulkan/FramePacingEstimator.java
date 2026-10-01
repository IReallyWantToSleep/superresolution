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

import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchResult;
import io.homo.superresolution.common.SuperResolution;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * FG-thread-owned estimator for real-frame production cadence.
 */
final class FramePacingEstimator {
    private static final double TRIM_FRACTION = 0.2;
    private static final long DEFAULT_REAL_PERIOD_NANOS = 16_666_667L;
    private static final long MIN_REAL_PERIOD_NANOS = 1_000_000L;
    private static final long MAX_REAL_PERIOD_NANOS = 500_000_000L;
    private static final int HISTORY_WINDOW_SIZE = 20;
    private static final int REQUIRED_REAL_ONLY_BATCHES = 2;
    private final String providerId;
    private final Deque<Long> recentPeriods = new ArrayDeque<>();
    private int plannedGeneratedCount = -1;
    private long swapchainGeneration = Long.MIN_VALUE;
    private long currentProducerTimeNanos;
    private long previousProducerTimeNanos;
    private boolean hasPreviousProducerTime;
    private boolean hasEstimatedPeriod;
    private boolean invalidatedThisFrame;
    private double estimatedPeriodNanos;
    private BatchMode confirmedBatchMode = BatchMode.UNKNOWN;
    private int consecutiveRealOnlyBatches;

    FramePacingEstimator(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId cannot be blank");
        }
        this.providerId = providerId;
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

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    long observeRealFrame(
            FrameGenerationWork job,
            long currentSwapchainGeneration
    ) {
        if (job == null) {
            throw new IllegalArgumentException("job cannot be null");
        }
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
            resetHistory(
                    String.join("; ", invalidationReasons),
                    currentProducerTimeNanos,
                    true
            );
            invalidatedThisFrame = true;
        } else {
            observeProducerTime(currentProducerTimeNanos);
        }
        return estimatedRealPeriodNanos();
    }

    boolean onBatchResult(
            int generatedCount,
            @Nullable
            FrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        if (generatedCount < 0) {
            throw new IllegalArgumentException("generatedCount cannot be negative");
        }

        if (historyDisposition != null
                && historyDisposition
                != FrameGenerationDispatchResult.HistoryDisposition.UNCHANGED) {
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
                && hasEstimatedPeriod
                && confirmedBatchMode != BatchMode.REAL_ONLY;
    }

    private void observeProducerTime(long producerTimeNanos) {
        if (!hasPreviousProducerTime) {
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

        recentPeriods.addLast(sample);
        while (recentPeriods.size() > HISTORY_WINDOW_SIZE) {
            recentPeriods.removeFirst();
        }
        estimatedPeriodNanos = trimmedMean(recentPeriods);
        hasEstimatedPeriod = true;
    }

    private void invalidateCurrentFrame(String reason) {
        if (invalidatedThisFrame) {
            return;
        }
        resetHistory(reason, currentProducerTimeNanos, true);
        invalidatedThisFrame = true;
    }

    private void resetHistory(
            String reason,
            long producerTimeNanos,
            boolean hasProducerTime
    ) {
        recentPeriods.clear();
        estimatedPeriodNanos = 0.0;
        hasEstimatedPeriod = false;
        hasPreviousProducerTime = false;
        previousProducerTimeNanos = 0L;
        if (hasProducerTime) {
            previousProducerTimeNanos = producerTimeNanos;
            hasPreviousProducerTime = true;
        }
        SuperResolution.LOGGER.info(
                "Frame pacing sample history reset for provider '{}': {}",
                providerId,
                reason
        );
    }

    private long estimatedRealPeriodNanos() {
        return hasEstimatedPeriod
                ? Math.round(estimatedPeriodNanos)
                : DEFAULT_REAL_PERIOD_NANOS;
    }

    private void resetBatchModeObservation() {
        confirmedBatchMode = BatchMode.UNKNOWN;
        consecutiveRealOnlyBatches = 0;
    }

    private enum BatchMode {
        UNKNOWN,
        GENERATED,
        REAL_ONLY
    }
}
