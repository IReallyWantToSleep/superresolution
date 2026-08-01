/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.vulkan;

import java.util.Objects;

/**
 * Immutable result of evaluating the first application-managed async-dispatch model.
 */
public record VulkanAsyncDispatchCapabilities(
        String providerId,
        boolean requested,
        int queueFamilyIndex,
        int availableQueueCount,
        boolean timelineSemaphoreSupported,
        boolean timelineSemaphoreEnabled,
        boolean frameGenerationQueueCreated,
        String unavailableReason
) {
    public VulkanAsyncDispatchCapabilities {
        providerId = Objects.requireNonNullElse(providerId, "");
        unavailableReason = Objects.requireNonNullElse(unavailableReason, "");
    }

    public static VulkanAsyncDispatchCapabilities evaluate(
            String providerId,
            boolean requested,
            int queueFamilyIndex,
            int availableQueueCount,
            boolean timelineSemaphoreSupported,
            boolean timelineSemaphoreEnabled,
            boolean frameGenerationQueueCreated
    ) {
        String reason = "";
        if (!requested) {
            reason = "No application-managed async provider was selected at startup";
        } else if (queueFamilyIndex < 0) {
            reason = "No graphics/present queue family was selected";
        } else if (availableQueueCount < 2) {
            reason = "Graphics/present queue family " + queueFamilyIndex
                    + " exposes " + availableQueueCount + " queue(s); at least 2 are required";
        } else if (!timelineSemaphoreSupported) {
            reason = "Timeline semaphore feature is not supported";
        } else if (!timelineSemaphoreEnabled) {
            reason = "Timeline semaphore feature was not enabled on the logical device";
        } else if (!frameGenerationQueueCreated) {
            reason = "The dedicated frame-generation queue was not created";
        }
        return new VulkanAsyncDispatchCapabilities(
                providerId,
                requested,
                queueFamilyIndex,
                availableQueueCount,
                timelineSemaphoreSupported,
                timelineSemaphoreEnabled,
                frameGenerationQueueCreated,
                reason
        );
    }

    public boolean available() {
        return requested && unavailableReason.isEmpty();
    }

    public VulkanAsyncDispatchCapabilities withRuntimeFailure(String reason) {
        return new VulkanAsyncDispatchCapabilities(
                providerId,
                requested,
                queueFamilyIndex,
                availableQueueCount,
                timelineSemaphoreSupported,
                timelineSemaphoreEnabled,
                frameGenerationQueueCreated,
                Objects.requireNonNull(reason, "reason cannot be null")
        );
    }
}
