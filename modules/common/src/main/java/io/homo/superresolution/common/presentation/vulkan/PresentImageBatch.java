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

import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.framegeneration.FrameGenerationProviderOutput;
import io.homo.superresolution.common.presentation.capture.FrameResources;

import javax.annotation.Nullable;
import java.util.List;

/** Atomically published display sources, generated images first and the real image last. */
final class PresentImageBatch {
    private final FrameGenerationWork work;
    private final long batchId;
    private final List<PresentImage> images;
    private final VulkanSwapchain.PresentationConfiguration configuration;
    private final long intervalNanos;
    private final boolean pacingEnabled;
    private final @Nullable FrameGenerationProviderOutput providerOutput;
    private final FrameGenerationDispatchCompletion providerCompletion;
    private final GpuReadyFences gpuReadyFences;
    private final @Nullable FrameGenerationDispatchResult.HistoryDisposition historyDisposition;
    private final boolean captureReleasedByGeneration;
    private final long[] captureReleaseSemaphores;

    PresentImageBatch(
            FrameGenerationWork work,
            long batchId,
            List<PresentImage> images,
            VulkanSwapchain.PresentationConfiguration configuration,
            long intervalNanos,
            boolean pacingEnabled,
            @Nullable
            FrameGenerationProviderOutput providerOutput,
            GpuReadyFences gpuReadyFences,
            @Nullable
            FrameGenerationDispatchResult.HistoryDisposition historyDisposition
    ) {
        this.images = List.copyOf(images);
        if (this.images.isEmpty()
                || this.images.get(this.images.size() - 1).kind() != PresentImage.Kind.REAL) {
            throw new IllegalArgumentException("A presentation batch must end with its real image");
        }
        for (int index = 0; index < this.images.size() - 1; index++) {
            if (this.images.get(index).kind() != PresentImage.Kind.GENERATED) {
                throw new IllegalArgumentException("Generated images must precede the real image");
            }
        }
        this.work = work;
        this.batchId = batchId;
        this.configuration = configuration;
        this.intervalNanos = intervalNanos;
        this.pacingEnabled = pacingEnabled;
        this.providerOutput = providerOutput;
        this.providerCompletion = providerOutput == null
                ? FrameGenerationDispatchCompletion.completed()
                : providerOutput.completion();
        this.gpuReadyFences = gpuReadyFences;
        this.historyDisposition = historyDisposition;
        this.captureReleasedByGeneration = providerOutput != null && providerOutput.realOutput() != null;
        // Snapshot before the capture ring can reuse a provider-real frame's inputs.
        this.captureReleaseSemaphores = work.frameResources().releaseSemaphores();
    }

    long realIndex() {
        return work.realIndex();
    }

    long batchId() {
        return batchId;
    }

    int generatedCount() {
        return images.size() - 1;
    }

    int imageCount() {
        return images.size();
    }

    List<PresentImage> images() {
        return images;
    }

    FrameResources frameResources() {
        return work.frameResources();
    }

    boolean presentAllowed() {
        return work.presentAllowed();
    }

    VulkanSwapchain.PresentationConfiguration configuration() {
        return configuration;
    }

    long intervalNanos() {
        return intervalNanos;
    }

    boolean pacingEnabled() {
        return pacingEnabled;
    }

    @Nullable
    FrameGenerationProviderOutput output() {
        return providerOutput;
    }

    FrameGenerationDispatchCompletion providerCompletion() {
        return providerCompletion;
    }

    GpuReadyFences gpuReadyFences() {
        return gpuReadyFences;
    }

    @Nullable
    FrameGenerationDispatchResult.HistoryDisposition historyDisposition() {
        return historyDisposition;
    }

    boolean captureReleasedByGeneration() {
        return captureReleasedByGeneration;
    }

    long[] captureReleaseSemaphores() {
        return captureReleaseSemaphores;
    }
}
