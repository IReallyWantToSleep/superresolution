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

import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.ProviderOutputLease;

import javax.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Immutable display item published by the FG thread.
 */
public final class PresentFrame {
    public enum Kind {
        GENERATED,
        REAL
    }

    private final long displayIndex;
    private final long realIndex;
    private final long latencyFrameId;
    private final Kind kind;
    private final long swapchainGeneration;
    private final long swapchainHandle;
    private final int swapchainImageIndex;
    private final long presentReadyBinary;
    private final long submissionTicket;
    private final long presentId;
    private final boolean outOfBand;
    private final long batchId;
    private final long batchIntervalNanos;
    private final int batchGeneratedCount;
    private final boolean pacingEnabled;
    private final @Nullable ProviderOutputLease sourceLease;
    private final FrameGenerationDispatchCompletion sourceCompletion;
    private final FrameGenerationDispatchCompletion acquireCompletion;
    private final VulkanBinarySemaphoreLease acquireLease;
    private final AtomicBoolean acquireLeaseReleased = new AtomicBoolean();

    PresentFrame(
            long displayIndex,
            long realIndex,
            long latencyFrameId,
            Kind kind,
            long swapchainGeneration,
            long swapchainHandle,
            int swapchainImageIndex,
            long presentReadyBinary,
            long submissionTicket,
            long presentId,
            boolean outOfBand,
            long batchId,
            long batchIntervalNanos,
            int batchGeneratedCount,
            boolean pacingEnabled,
            @Nullable ProviderOutputLease sourceLease,
            FrameGenerationDispatchCompletion sourceCompletion,
            @Nullable FrameGenerationDispatchCompletion acquireCompletion,
            @Nullable VulkanBinarySemaphoreLease acquireLease
    ) {
        if (displayIndex < 0L || realIndex < 0L) {
            throw new IllegalArgumentException("Frame indices cannot be negative");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind cannot be null");
        }
        if (swapchainGeneration <= 0L || swapchainHandle == 0L) {
            throw new IllegalArgumentException("PresentFrame must reference a live swapchain");
        }
        if (swapchainImageIndex < 0 || presentReadyBinary == 0L) {
            throw new IllegalArgumentException("PresentFrame must reference a binary present wait");
        }
        if (submissionTicket <= 0L) {
            throw new IllegalArgumentException("submissionTicket must be positive");
        }
        if (batchId <= 0L || batchIntervalNanos <= 0L || batchGeneratedCount < 0) {
            throw new IllegalArgumentException("PresentFrame batch metadata is invalid");
        }
        if (kind == Kind.GENERATED && batchGeneratedCount == 0) {
            throw new IllegalArgumentException("Generated frame requires a generated batch");
        }
        if (pacingEnabled && batchGeneratedCount == 0) {
            throw new IllegalArgumentException("Real-only frame cannot enable pacing");
        }
        if (sourceCompletion == null) {
            throw new IllegalArgumentException("sourceCompletion cannot be null");
        }
        if (kind == Kind.GENERATED && sourceLease == null) {
            throw new IllegalArgumentException("Generated frames require a provider output lease");
        }
        this.displayIndex = displayIndex;
        this.realIndex = realIndex;
        this.latencyFrameId = latencyFrameId;
        this.kind = kind;
        this.swapchainGeneration = swapchainGeneration;
        this.swapchainHandle = swapchainHandle;
        this.swapchainImageIndex = swapchainImageIndex;
        this.presentReadyBinary = presentReadyBinary;
        this.submissionTicket = submissionTicket;
        this.presentId = presentId;
        this.outOfBand = outOfBand;
        this.batchId = batchId;
        this.batchIntervalNanos = batchIntervalNanos;
        this.batchGeneratedCount = batchGeneratedCount;
        this.pacingEnabled = pacingEnabled;
        this.sourceLease = sourceLease;
        this.sourceCompletion = sourceCompletion;
        this.acquireCompletion = acquireCompletion == null ? sourceCompletion : acquireCompletion;
        this.acquireLease = acquireLease;
    }

    public long displayIndex() {
        return displayIndex;
    }

    public long realIndex() {
        return realIndex;
    }

    public long latencyFrameId() {
        return latencyFrameId;
    }

    public Kind kind() {
        return kind;
    }

    public long swapchainGeneration() {
        return swapchainGeneration;
    }

    public long swapchainHandle() {
        return swapchainHandle;
    }

    public int swapchainImageIndex() {
        return swapchainImageIndex;
    }

    public long presentReadyBinary() {
        return presentReadyBinary;
    }

    public long submissionTicket() {
        return submissionTicket;
    }

    public long presentId() {
        return presentId;
    }

    public boolean outOfBand() {
        return outOfBand;
    }

    public long batchId() {
        return batchId;
    }

    public long batchIntervalNanos() {
        return batchIntervalNanos;
    }

    public int batchGeneratedCount() {
        return batchGeneratedCount;
    }

    public boolean pacingEnabled() {
        return pacingEnabled;
    }

    public @Nullable ProviderOutputLease sourceLease() {
        return sourceLease;
    }

    public FrameGenerationDispatchCompletion sourceCompletion() {
        return sourceCompletion;
    }

    /**
     * Completion of the single submission that consumed this frame's acquire semaphore.
     * Recycling that semaphore only requires its own submission to retire, so this is
     * narrower than {@link #sourceCompletion()} — which spans every submission in the
     * batch and would otherwise make one frame's release wait on its siblings.
     */
    public FrameGenerationDispatchCompletion acquireCompletion() {
        return acquireCompletion;
    }

    void releaseAcquireLease() {
        if (acquireLease != null && acquireLeaseReleased.compareAndSet(false, true)) {
            acquireLease.close();
        }
    }

    interface VulkanBinarySemaphoreLease {
        void close();
    }
}
