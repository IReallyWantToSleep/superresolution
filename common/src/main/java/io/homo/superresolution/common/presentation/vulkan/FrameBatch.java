/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License.
 */

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.ProviderOutputLease;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Locally complete display batch. Publication is all-or-nothing through
 * {@link FrameQueue#putBatch(java.util.Collection)}.
 */
public final class FrameBatch {
    private final long realIndex;
    private final int generatedCount;
    private final long batchId;
    private final long latencyFrameId;
    private final long realPresentId;
    private final long batchIntervalNanos;
    private final boolean pacingEnabled;
    private final List<PresentFrame> presentFrames;
    private final @Nullable ProviderOutputLease providerOutputLease;
    private final FrameGenerationDispatchCompletion dispatchCompletion;
    private final @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition;

    public FrameBatch(
            long realIndex,
            int generatedCount,
            long batchId,
            long latencyFrameId,
            long realPresentId,
            long batchIntervalNanos,
            boolean pacingEnabled,
            List<PresentFrame> presentFrames,
            @Nullable ProviderOutputLease providerOutputLease,
            FrameGenerationDispatchCompletion dispatchCompletion,
            @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        if (realIndex < 0L || generatedCount < 0 || batchId <= 0L || batchIntervalNanos <= 0L) {
            throw new IllegalArgumentException("Invalid frame batch metadata");
        }
        if (presentFrames == null || presentFrames.isEmpty()) {
            throw new IllegalArgumentException("A frame batch must contain at least one item");
        }
        if (presentFrames.size() != generatedCount + 1) {
            throw new IllegalArgumentException("Frame batch item count does not match generated count");
        }
        if (pacingEnabled && generatedCount == 0) {
            throw new IllegalArgumentException("Real-only batch cannot enable pacing");
        }
        if (presentFrames.get(presentFrames.size() - 1).kind() != PresentFrame.Kind.REAL) {
            throw new IllegalArgumentException("Real frame must be the final batch item");
        }
        if (presentFrames.get(presentFrames.size() - 1).presentId() != realPresentId) {
            throw new IllegalArgumentException("Real present id does not match its batch");
        }
        if (dispatchCompletion == null) {
            throw new IllegalArgumentException("dispatchCompletion cannot be null");
        }
        if ((providerOutputLease == null) != (historyDisposition == null)) {
            throw new IllegalArgumentException(
                    "Provider lease and history disposition must either both be present or both be absent"
            );
        }
        for (int index = 0; index < presentFrames.size(); index++) {
            PresentFrame frame = presentFrames.get(index);
            PresentFrame.Kind expectedKind = index < generatedCount
                    ? PresentFrame.Kind.GENERATED
                    : PresentFrame.Kind.REAL;
            if (frame.kind() != expectedKind) {
                throw new IllegalArgumentException("Generated frames must precede the real frame");
            }
            if (frame.realIndex() != realIndex
                    || frame.latencyFrameId() != latencyFrameId
                    || frame.batchId() != batchId
                    || frame.batchIntervalNanos() != batchIntervalNanos
                    || frame.batchGeneratedCount() != generatedCount
                    || frame.pacingEnabled() != pacingEnabled) {
                throw new IllegalArgumentException("Present frame metadata does not match its batch");
            }
            if (frame.sourceLease() != providerOutputLease
                    || frame.sourceCompletion() != dispatchCompletion) {
                throw new IllegalArgumentException(
                        "Present frame provider ownership does not match its batch"
                );
            }
        }
        this.realIndex = realIndex;
        this.generatedCount = generatedCount;
        this.batchId = batchId;
        this.latencyFrameId = latencyFrameId;
        this.realPresentId = realPresentId;
        this.batchIntervalNanos = batchIntervalNanos;
        this.pacingEnabled = pacingEnabled;
        this.presentFrames = List.copyOf(presentFrames);
        this.providerOutputLease = providerOutputLease;
        this.dispatchCompletion = dispatchCompletion;
        this.historyDisposition = historyDisposition;
    }

    public static FrameBatch realOnly(
            long realIndex,
            long batchId,
            long latencyFrameId,
            long realPresentId,
            long batchIntervalNanos,
            PresentFrame realFrame,
            FrameGenerationDispatchCompletion completion
    ) {
        return new FrameBatch(
                realIndex,
                0,
                batchId,
                latencyFrameId,
                realPresentId,
                batchIntervalNanos,
                false,
                List.of(realFrame),
                null,
                completion,
                null
        );
    }

    public static FrameBatch applicationManaged(
            long realIndex,
            int generatedCount,
            long batchId,
            long latencyFrameId,
            long realPresentId,
            long batchIntervalNanos,
            boolean pacingEnabled,
            List<PresentFrame> presentFrames,
            ProviderOutputLease providerOutputLease,
            FrameGenerationDispatchCompletion dispatchCompletion,
            AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        return new FrameBatch(
                realIndex,
                generatedCount,
                batchId,
                latencyFrameId,
                realPresentId,
                batchIntervalNanos,
                pacingEnabled,
                presentFrames,
                providerOutputLease,
                dispatchCompletion,
                historyDisposition
        );
    }

    public long realIndex() {
        return realIndex;
    }

    public int generatedCount() {
        return generatedCount;
    }

    public long batchId() {
        return batchId;
    }

    public long latencyFrameId() {
        return latencyFrameId;
    }

    public long realPresentId() {
        return realPresentId;
    }

    public long batchIntervalNanos() {
        return batchIntervalNanos;
    }

    public boolean pacingEnabled() {
        return pacingEnabled;
    }

    public List<PresentFrame> presentFrames() {
        return presentFrames;
    }

    public @Nullable ProviderOutputLease providerOutputLease() {
        return providerOutputLease;
    }

    public FrameGenerationDispatchCompletion dispatchCompletion() {
        return dispatchCompletion;
    }

    public @Nullable AsyncFrameGenerationDispatchResult.HistoryDisposition historyDisposition() {
        return historyDisposition;
    }
}
