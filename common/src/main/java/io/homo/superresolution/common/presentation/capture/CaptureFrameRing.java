/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.capture;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.Arrays;

final class CaptureFrameRing {
    static final int MAX_IN_FLIGHT_FRAMES = 3;
    private final FrameResources[] slots = new FrameResources[MAX_IN_FLIGHT_FRAMES];
    private int cursor;
    private long generation;
    private FrameResources activeFrame;

    void initialize(VulkanDevice device) {
        if (slots[0] != null) {
            return;
        }
        try {
            for (int i = 0; i < slots.length; i++) {
                slots[i] = new FrameResources(i, device);
            }
        } catch (Throwable throwable) {
            destroy();
            throw throwable;
        }
    }

    boolean isInitialized() {
        return slots[0] != null;
    }

    FrameResources beginFrame(int logicalFrameIndex) {
        if (activeFrame != null) {
            if (activeFrame.logicalFrameIndex() != logicalFrameIndex) {
                throw new IllegalStateException(
                        "Capture frame index changed from " + activeFrame.logicalFrameIndex()
                                + " to " + logicalFrameIndex
                );
            }
            return activeFrame;
        }
        FrameResources frame = slots[cursor];
        if (frame == null) {
            throw new IllegalStateException("Frame capture resources are not initialized");
        }
        cursor = (cursor + 1) % slots.length;
        frame.begin(++generation, logicalFrameIndex);
        activeFrame = frame;
        return frame;
    }

    FrameResources activeFrameOrNull() {
        return activeFrame;
    }

    FrameResources finishFrame() {
        FrameResources frame = activeFrame;
        activeFrame = null;
        if (frame == null || !frame.hasAnyResource()) {
            return null;
        }
        frame.seal();
        return frame;
    }

    void destroy() {
        activeFrame = null;
        for (FrameResources frame : slots) {
            if (frame != null) {
                frame.destroy();
            }
        }
        Arrays.fill(slots, null);
        cursor = 0;
        generation = 0;
    }
}
