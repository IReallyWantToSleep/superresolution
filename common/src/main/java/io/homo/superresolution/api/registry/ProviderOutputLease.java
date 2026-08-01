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

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Exclusive lease over provider-owned display outputs for one dispatch.
 * <p>
 * The scheduler keeps the lease until every display item that references it
 * has drained. The provider may reuse or destroy the underlying slot only
 * after {@link #release()} and its own {@link #completion()} requirements are
 * satisfied. Acquire and release for application-managed providers occur on
 * the FG scheduler thread.
 */
public interface ProviderOutputLease extends AutoCloseable {
    List<VulkanTexture> generatedOutputs();

    /**
     * Optional provider-produced real output. A null value means the scheduler
     * presents the captured real frame.
     */
    @Nullable VulkanTexture realOutput();

    FrameGenerationDispatchCompletion completion();

    OutputKey outputKey();

    boolean isReleased();

    void release();

    @Override
    default void close() {
        release();
    }

    record OutputKey(int width, int height, int format) {
        public OutputKey {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Output dimensions must be positive");
            }
        }
    }
}
