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

public final class CaptureFrameRing {
    public static final int MAX_IN_FLIGHT_FRAMES = 3;
    private final FrameResources[] slots = new FrameResources[MAX_IN_FLIGHT_FRAMES];
    private int cursor;
    private long generation;
    private volatile FrameResources activeFrame;

    synchronized void initialize(VulkanDevice device) {
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

    synchronized boolean isInitialized() {
        return slots[0] != null;
    }

    synchronized FrameResources beginFrame(int logicalFrameIndex) {
        if (activeFrame != null) {
            if (activeFrame.logicalFrameIndex() != logicalFrameIndex) {
                throw new IllegalStateException(
                        "Capture frame index changed from " + activeFrame.logicalFrameIndex()
                                + " to " + logicalFrameIndex
                );
            }
            return activeFrame;
        }
        FrameResources frame = selectReusableSlot();
        frame.begin(++generation, logicalFrameIndex);
        activeFrame = frame;
        return frame;
    }

    FrameResources activeFrameOrNull() {
        return activeFrame;
    }

    synchronized FrameResources finishFrame() {
        FrameResources frame = activeFrame;
        activeFrame = null;
        if (frame == null) {
            return null;
        }
        if (!frame.hasAnyResource()) {
            frame.discardEmptyRecording();
            return null;
        }
        frame.seal();
        return frame;
    }

    synchronized void destroy() {
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

    private FrameResources selectReusableSlot() {
        for (int offset = 0; offset < slots.length; offset++) {
            int index = (cursor + offset) % slots.length;
            FrameResources frame = requireSlot(index);
            if (frame.state() == FrameResourceState.REUSABLE) {
                cursor = (index + 1) % slots.length;
                return frame;
            }
        }
        for (int offset = 0; offset < slots.length; offset++) {
            int index = (cursor + offset) % slots.length;
            FrameResources frame = requireSlot(index);
            if (frame.state() == FrameResourceState.SUBMITTED) {
                cursor = (index + 1) % slots.length;
                return frame;
            }
        }
        throw new IllegalStateException(
                "No reusable capture slot is available; states="
                        + Arrays.toString(Arrays.stream(slots)
                                .map(FrameResources::state)
                                .toArray())
        );
    }

    private FrameResources requireSlot(int index) {
        FrameResources frame = slots[index];
        if (frame == null) {
            throw new IllegalStateException("Frame capture resources are not initialized");
        }
        return frame;
    }
}
