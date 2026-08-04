/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.ProviderInputSnapshot;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

/**
 * Immutable ownership transfer from the render thread to the FG scheduler.
 *
 * <p>The optional provider snapshot is captured before queue publication. The
 * FG thread must consume this snapshot instead of rereading mutable frame
 * constants or provider configuration.</p>
 */
public final class RealFrameJob {
    private final long realIndex;
    private final int logicalFrameIndex;
    private final long latencyFrameId;
    private final long realPresentId;
    private final FrameResources frameResources;
    private final @Nullable ProviderInputSnapshot providerInputSnapshot;
    private final long producerTimeNanos;
    private final int plannedGeneratedCount;
    private final int colorWidth;
    private final int colorHeight;
    private final @Nullable TextureFormat colorFormat;
    private final boolean historyResetRequested;
    private final boolean presentAllowed;

    public RealFrameJob(
            long realIndex,
            int logicalFrameIndex,
            long latencyFrameId,
            long realPresentId,
            FrameResources frameResources,
            @Nullable ProviderInputSnapshot providerInputSnapshot,
            long producerTimeNanos,
            int plannedGeneratedCount,
            boolean historyResetRequested,
            boolean presentAllowed
    ) {
        if (realIndex < 0L) {
            throw new IllegalArgumentException("realIndex cannot be negative");
        }
        if (frameResources == null) {
            throw new IllegalArgumentException("frameResources cannot be null");
        }
        if (providerInputSnapshot != null
                && providerInputSnapshot.logicalFrameIndex() != logicalFrameIndex) {
            throw new IllegalArgumentException(
                    "Provider input snapshot does not belong to the queued logical frame"
            );
        }
        if (plannedGeneratedCount < 0) {
            throw new IllegalArgumentException("plannedGeneratedCount cannot be negative");
        }
        this.realIndex = realIndex;
        this.logicalFrameIndex = logicalFrameIndex;
        this.latencyFrameId = latencyFrameId;
        this.realPresentId = realPresentId;
        this.frameResources = frameResources;
        this.providerInputSnapshot = providerInputSnapshot;
        this.producerTimeNanos = producerTimeNanos;
        this.plannedGeneratedCount = plannedGeneratedCount;
        VulkanTexture color = frameResources.finalColorVulkanTexture();
        this.colorWidth = color == null ? 0 : color.getWidth();
        this.colorHeight = color == null ? 0 : color.getHeight();
        this.colorFormat = color == null ? null : color.getTextureFormat();
        this.historyResetRequested = historyResetRequested;
        this.presentAllowed = presentAllowed;
    }

    public long realIndex() {
        return realIndex;
    }

    public int logicalFrameIndex() {
        return logicalFrameIndex;
    }

    public long latencyFrameId() {
        return latencyFrameId;
    }

    public long realPresentId() {
        return realPresentId;
    }

    public FrameResources frameResources() {
        return frameResources;
    }

    public @Nullable ProviderInputSnapshot providerInputSnapshot() {
        return providerInputSnapshot;
    }

    public long producerTimeNanos() {
        return producerTimeNanos;
    }

    public int plannedGeneratedCount() {
        return plannedGeneratedCount;
    }

    public int colorWidth() {
        return colorWidth;
    }

    public int colorHeight() {
        return colorHeight;
    }

    public @Nullable TextureFormat colorFormat() {
        return colorFormat;
    }

    public boolean historyResetRequested() {
        return historyResetRequested;
    }

    public boolean presentAllowed() {
        return presentAllowed;
    }
}
