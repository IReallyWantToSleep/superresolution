/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.presentation.capture;

import io.homo.superresolution.common.presentation.vulkan.FramePacingTiming;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class CaptureFrameRing {
    public static final int MAX_IN_FLIGHT_FRAMES = 3;
    private static final long SLOT_WAIT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private final FrameResources[] slots = new FrameResources[MAX_IN_FLIGHT_FRAMES];
    // Separate from the ring monitor: workers signal slot submission through it while the
    // render thread may hold the ring monitor during a GPU wait in FrameResources.begin.
    private final Object slotStateMonitor = new Object();
    private FramePacingTiming framePacingTiming;
    private int cursor;
    private long generation;
    private volatile FrameResources activeFrame;

    synchronized void initialize(
            VulkanDevice device,
            FramePacingTiming framePacingTiming
    ) {
        if (slots[0] != null) {
            return;
        }
        this.framePacingTiming = framePacingTiming;
        try {
            for (int i = 0; i < slots.length; i++) {
                slots[i] = new FrameResources(i, device, framePacingTiming, this::onSlotStateChanged);
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
        framePacingTiming = null;
    }

    private void onSlotStateChanged() {
        synchronized (slotStateMonitor) {
            slotStateMonitor.notifyAll();
        }
    }

    private FrameResources selectReusableSlot() {
        FrameResources frame = findAvailableSlot();
        return frame != null ? frame : awaitAvailableSlot();
    }

    // The render thread is throttled only by the generation queue, which is shorter than the
    // ring, so every slot can still be QUEUED or DISPATCHING on the presentation workers.
    // Wait for a worker to submit one instead of failing the frame.
    private FrameResources awaitAvailableSlot() {
        long startedAtNanos = System.nanoTime();
        long deadlineNanos = startedAtNanos + SLOT_WAIT_TIMEOUT_NANOS;
        try {
            synchronized (slotStateMonitor) {
                while (true) {
                    FrameResources frame = findAvailableSlot();
                    if (frame != null) {
                        return frame;
                    }
                    if (allSlotsUnrecoverable()) {
                        throw new IllegalStateException(
                                "Every capture slot failed ownership recovery; states=" + describeStates()
                        );
                    }
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (remainingNanos <= 0L) {
                        throw new IllegalStateException(
                                "Timed out waiting for a reusable capture slot; states=" + describeStates()
                        );
                    }
                    TimeUnit.NANOSECONDS.timedWait(slotStateMonitor, remainingNanos);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a reusable capture slot", exception);
        } finally {
            if (framePacingTiming != null) {
                framePacingTiming.recordExcludedWait(Math.max(0L, System.nanoTime() - startedAtNanos));
            }
        }
    }

    private FrameResources findAvailableSlot() {
        FrameResources frame = findSlot(FrameResourceState.REUSABLE);
        return frame != null ? frame : findSlot(FrameResourceState.SUBMITTED);
    }

    private FrameResources findSlot(FrameResourceState state) {
        for (int offset = 0; offset < slots.length; offset++) {
            int index = (cursor + offset) % slots.length;
            FrameResources frame = requireSlot(index);
            if (frame.state() == state) {
                cursor = (index + 1) % slots.length;
                return frame;
            }
        }
        return null;
    }

    private boolean allSlotsUnrecoverable() {
        for (int index = 0; index < slots.length; index++) {
            if (!requireSlot(index).isUnrecoverable()) {
                return false;
            }
        }
        return true;
    }

    private String describeStates() {
        return Arrays.toString(Arrays.stream(slots).map(FrameResources::state).toArray());
    }

    private FrameResources requireSlot(int index) {
        FrameResources frame = slots[index];
        if (frame == null) {
            throw new IllegalStateException("Frame capture resources are not initialized");
        }
        return frame;
    }
}
