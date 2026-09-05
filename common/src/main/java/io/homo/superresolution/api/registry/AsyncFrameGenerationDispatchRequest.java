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

package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.Objects;

/**
 * Complete immutable input for one application-managed provider dispatch.
 * <p>
 * The scheduler creates this request on its FG thread. The command buffers belong to the
 * dedicated FG queue selected by the scheduler; the provider records into them but does
 * not acquire swapchain images, publish present queue items, pace frames, or call
 * {@code vkQueuePresentKHR}.
 * <p>
 * One command buffer is supplied per requested generated frame, and each is submitted
 * separately in index order. The work that produces generated frame {@code k} therefore
 * belongs in {@link #generatedFrameCommandBuffer(int)} for {@code k}, so that frame's
 * present can be signalled as soon as its own work retires instead of waiting for the
 * rest of the batch. Recording everything into {@link #commandBuffer()} stays correct —
 * the remaining buffers are simply submitted empty — but gives up that overlap.
 * <p>
 * Because the buffers are submitted in index order to a single queue, a barrier recorded
 * in an earlier buffer still covers work recorded in a later one; shared setup belongs in
 * {@link #commandBuffer()}.
 */
public record AsyncFrameGenerationDispatchRequest(
        FrameResources frameResources,
        ProviderInputSnapshot providerInputSnapshot,
        VulkanDevice device,
        long[] generatedFrameCommandBuffers,
        int outputWidth,
        int outputHeight,
        int outputFormat,
        int backBufferCount
) {
    public AsyncFrameGenerationDispatchRequest {
        frameResources = Objects.requireNonNull(frameResources, "frameResources cannot be null");
        providerInputSnapshot = Objects.requireNonNull(
                providerInputSnapshot,
                "providerInputSnapshot cannot be null"
        );
        device = Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(
                generatedFrameCommandBuffers,
                "generatedFrameCommandBuffers cannot be null"
        );
        if (generatedFrameCommandBuffers.length == 0) {
            throw new IllegalArgumentException("At least one command buffer is required");
        }
        generatedFrameCommandBuffers = generatedFrameCommandBuffers.clone();
        for (long commandBuffer : generatedFrameCommandBuffers) {
            if (commandBuffer == 0L) {
                throw new IllegalArgumentException("commandBuffer cannot be null");
            }
        }
        if (outputWidth <= 0 || outputHeight <= 0) {
            throw new IllegalArgumentException("Output dimensions must be positive");
        }
        if (backBufferCount <= 0) {
            throw new IllegalArgumentException("backBufferCount must be positive");
        }
    }

    /**
     * The command buffer for the work producing generated frame {@code generatedIndex},
     * which is also the buffer its swapchain blit is appended to.
     */
    public long generatedFrameCommandBuffer(int generatedIndex) {
        if (generatedIndex < 0 || generatedIndex >= generatedFrameCommandBuffers.length) {
            throw new IndexOutOfBoundsException(
                    "No command buffer for generated frame " + generatedIndex
            );
        }
        return generatedFrameCommandBuffers[generatedIndex];
    }

    /**
     * Command buffers supplied for this dispatch. Never less than one, even when the mode
     * asks for no generated frames, so shared work always has somewhere to go.
     */
    public int commandBufferCount() {
        return generatedFrameCommandBuffers.length;
    }

    /** The first command buffer, which is where shared setup work belongs. */
    public long commandBuffer() {
        return generatedFrameCommandBuffers[0];
    }

    @Override
    public long[] generatedFrameCommandBuffers() {
        return generatedFrameCommandBuffers.clone();
    }

    public int requestedGeneratedFrameCount() {
        return providerInputSnapshot.mode().generatedFrameCount();
    }
}
