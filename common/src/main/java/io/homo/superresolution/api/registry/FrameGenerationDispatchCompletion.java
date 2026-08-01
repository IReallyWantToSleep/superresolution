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

/**
 * Provider-owned completion point for one application-managed dispatch.
 * <p>
 * This completion protects provider output reuse. It is not the scheduler's
 * submission-issued ticket and must not be interpreted as present readiness.
 */
public interface FrameGenerationDispatchCompletion {
    FrameGenerationDispatchCompletion COMPLETED = new FrameGenerationDispatchCompletion() {
        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public void awaitCompletion() {
        }
    };

    boolean isComplete();

    void awaitCompletion();

    static FrameGenerationDispatchCompletion completed() {
        return COMPLETED;
    }
}
