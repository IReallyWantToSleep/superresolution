/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.capture;

import java.util.concurrent.atomic.AtomicReference;

final class FrameResourceLifecycle {
    private final AtomicReference<FrameResourceState> state =
            new AtomicReference<>(FrameResourceState.REUSABLE);

    FrameResourceState state() {
        return state.get();
    }

    void beginRecording() {
        transition(FrameResourceState.REUSABLE, FrameResourceState.RECORDING);
    }

    void seal() {
        transition(FrameResourceState.RECORDING, FrameResourceState.SEALED);
    }

    void discardEmptyRecording() {
        transition(FrameResourceState.RECORDING, FrameResourceState.REUSABLE);
    }

    void markQueued() {
        transition(FrameResourceState.SEALED, FrameResourceState.QUEUED);
    }

    void markDispatching() {
        transition(FrameResourceState.QUEUED, FrameResourceState.DISPATCHING);
    }

    void requireSubmittable() {
        FrameResourceState current = state.get();
        if (current != FrameResourceState.SEALED
                && current != FrameResourceState.DISPATCHING) {
            throw invalidTransition(current, FrameResourceState.SUBMITTED);
        }
    }

    void markSubmitted() {
        while (true) {
            FrameResourceState current = state.get();
            if (current != FrameResourceState.SEALED
                    && current != FrameResourceState.DISPATCHING) {
                throw invalidTransition(current, FrameResourceState.SUBMITTED);
            }
            if (state.compareAndSet(current, FrameResourceState.SUBMITTED)) {
                return;
            }
        }
    }

    void markReusable() {
        transition(FrameResourceState.SUBMITTED, FrameResourceState.REUSABLE);
    }

    private void transition(FrameResourceState expected, FrameResourceState target) {
        if (!state.compareAndSet(expected, target)) {
            throw invalidTransition(state.get(), target);
        }
    }

    private static IllegalStateException invalidTransition(
            FrameResourceState current,
            FrameResourceState target
    ) {
        return new IllegalStateException(
                "Invalid frame-resource transition " + current + " -> " + target
        );
    }
}
