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

package io.homo.superresolution.api.registry.framegeneration;

import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.Objects;

public record FrameGenerationDispatchInput(
        FrameResources frameResources,
        ProviderInputSnapshot providerInputSnapshot,
        VulkanDevice device,
        long[] dispatchCommandBuffers,
        int outputWidth,
        int outputHeight,
        int outputFormat
) {
    public FrameGenerationDispatchInput {
        frameResources = Objects.requireNonNull(frameResources, "frameResources cannot be null");
        providerInputSnapshot = Objects.requireNonNull(
                providerInputSnapshot,
                "providerInputSnapshot cannot be null"
        );
        device = Objects.requireNonNull(device, "device cannot be null");
        Objects.requireNonNull(
                dispatchCommandBuffers,
                "dispatchCommandBuffers cannot be null"
        );
        if (dispatchCommandBuffers.length == 0) {
            throw new IllegalArgumentException("At least one command buffer is required");
        }
        dispatchCommandBuffers = dispatchCommandBuffers.clone();
        for (long commandBuffer : dispatchCommandBuffers) {
            if (commandBuffer == 0L) {
                throw new IllegalArgumentException("commandBuffer cannot be null");
            }
        }
        if (outputWidth <= 0 || outputHeight <= 0) {
            throw new IllegalArgumentException("Output dimensions must be positive");
        }
    }

    public long dispatchCommandBuffer(int generatedIndex) {
        if (generatedIndex < 0 || generatedIndex >= dispatchCommandBuffers.length) {
            throw new IndexOutOfBoundsException(
                    "No command buffer for generated frame " + generatedIndex
            );
        }
        return dispatchCommandBuffers[generatedIndex];
    }

    public int commandBufferCount() {
        return dispatchCommandBuffers.length;
    }

    public long commandBuffer() {
        return dispatchCommandBuffers[0];
    }

    @Override
    public long[] dispatchCommandBuffers() {
        return dispatchCommandBuffers.clone();
    }

    public int requestedGeneratedFrameCount() {
        return providerInputSnapshot.mode().generatedFrameCount();
    }
}
