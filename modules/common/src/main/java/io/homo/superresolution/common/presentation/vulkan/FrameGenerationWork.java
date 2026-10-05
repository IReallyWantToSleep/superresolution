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

import io.homo.superresolution.api.registry.framegeneration.ProviderInputSnapshot;
import io.homo.superresolution.common.presentation.capture.FrameResources;

import javax.annotation.Nullable;

public final class FrameGenerationWork {
    private final long realIndex;
    private final int logicalFrameIndex;
    private final long latencyFrameId;
    private final long realPresentId;
    private final FrameResources frameResources;
    private final @Nullable ProviderInputSnapshot providerInputSnapshot;
    private final long producerTimeNanos;
    private final int plannedGeneratedCount;
    private final boolean historyResetRequested;
    private final boolean presentAllowed;

    public FrameGenerationWork(
            long realIndex,
            int logicalFrameIndex,
            long latencyFrameId,
            long realPresentId,
            FrameResources frameResources,
            @Nullable
            ProviderInputSnapshot providerInputSnapshot,
            long producerTimeNanos,
            int plannedGeneratedCount,
            boolean historyResetRequested,
            boolean presentAllowed
    ) {
        this.realIndex = realIndex;
        this.logicalFrameIndex = logicalFrameIndex;
        this.latencyFrameId = latencyFrameId;
        this.realPresentId = realPresentId;
        this.frameResources = frameResources;
        this.providerInputSnapshot = providerInputSnapshot;
        this.producerTimeNanos = producerTimeNanos;
        this.plannedGeneratedCount = plannedGeneratedCount;
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

    public boolean historyResetRequested() {
        return historyResetRequested;
    }

    public boolean presentAllowed() {
        return presentAllowed;
    }
}
