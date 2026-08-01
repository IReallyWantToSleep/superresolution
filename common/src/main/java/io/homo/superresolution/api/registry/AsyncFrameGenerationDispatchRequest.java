/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.Objects;

/**
 * Complete immutable input for one application-managed provider dispatch.
 * <p>
 * The scheduler creates this request on its FG thread. {@code commandBuffer}
 * belongs to the dedicated FG queue selected by the scheduler; the provider
 * records into it but does not acquire swapchain images, publish present queue
 * items, pace frames, or call {@code vkQueuePresentKHR}.
 */
public record AsyncFrameGenerationDispatchRequest(
        FrameResources frameResources,
        ProviderInputSnapshot providerInputSnapshot,
        VulkanDevice device,
        long commandBuffer,
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
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer cannot be null");
        }
        if (outputWidth <= 0 || outputHeight <= 0) {
            throw new IllegalArgumentException("Output dimensions must be positive");
        }
        if (backBufferCount <= 0) {
            throw new IllegalArgumentException("backBufferCount must be positive");
        }
    }

    public int requestedGeneratedFrameCount() {
        return providerInputSnapshot.mode().generatedFrameCount();
    }
}
