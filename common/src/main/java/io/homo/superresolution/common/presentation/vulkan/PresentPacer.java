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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.common.SuperResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Presents frame-generation batches (interpolated frames followed by the real frame)
 * from a dedicated thread so the render thread never sleeps for pacing. The DLSS-FG
 * SDK requires the application to space generated and real frames evenly; the pacer
 * derives the spacing from the measured interval between successive batches.
 */
final class PresentPacer {
    private static final long MIN_SPACING_NANOS = 500_000L;
    private static final long MAX_SPACING_NANOS = 100_000_000L;
    private static final double INTERVAL_EMA_ALPHA = 0.2;

    private final Object lock = new Object();
    private List<Integer> pendingImages;
    private long pendingSpacingNanos;
    private PresentFunction pendingPresentFunction;
    private boolean batchInFlight;
    private boolean shutdownRequested;
    private boolean recreateRequested;
    private Throwable failure;
    private Thread worker;
    private long lastBatchNanos;
    private double intervalEmaNanos;

    interface PresentFunction {
        int present(int imageIndex);
    }

    /**
     * Queues one batch of already-submitted swapchain images for paced presentation.
     * Call {@link #awaitIdle()} first; at most one batch may be outstanding.
     */
    void submitBatch(List<Integer> imageIndices, PresentFunction presentFunction) {
        if (imageIndices == null || imageIndices.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        synchronized (lock) {
            if (shutdownRequested) {
                return;
            }
            if (pendingImages != null || batchInFlight) {
                throw new IllegalStateException("A present batch is already outstanding");
            }
            if (lastBatchNanos != 0L) {
                long interval = now - lastBatchNanos;
                intervalEmaNanos = intervalEmaNanos <= 0.0
                        ? interval
                        : intervalEmaNanos * (1.0 - INTERVAL_EMA_ALPHA) + interval * INTERVAL_EMA_ALPHA;
            }
            lastBatchNanos = now;
            long spacing = intervalEmaNanos <= 0.0
                    ? MIN_SPACING_NANOS
                    : (long) (intervalEmaNanos / imageIndices.size());
            pendingSpacingNanos = Math.max(MIN_SPACING_NANOS, Math.min(MAX_SPACING_NANOS, spacing));
            pendingImages = new ArrayList<>(imageIndices);
            pendingPresentFunction = presentFunction;
            ensureWorker();
            lock.notifyAll();
        }
    }

    /**
     * Blocks until the outstanding batch (if any) has been fully presented.
     */
    void awaitIdle() {
        synchronized (lock) {
            boolean interrupted = false;
            while (pendingImages != null || batchInFlight) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    boolean consumeRecreateRequest() {
        synchronized (lock) {
            boolean requested = recreateRequested;
            recreateRequested = false;
            return requested;
        }
    }

    void throwIfFailed() {
        Throwable pendingFailure;
        synchronized (lock) {
            pendingFailure = failure;
            failure = null;
        }
        if (pendingFailure != null) {
            throw new IllegalStateException("Paced presentation failed", pendingFailure);
        }
    }

    void invalidatePacing() {
        synchronized (lock) {
            lastBatchNanos = 0L;
            intervalEmaNanos = 0.0;
        }
    }

    void shutdown() {
        Thread activeWorker;
        synchronized (lock) {
            shutdownRequested = true;
            pendingImages = null;
            pendingPresentFunction = null;
            activeWorker = worker;
            worker = null;
            lock.notifyAll();
        }
        if (activeWorker != null) {
            try {
                activeWorker.join(2_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (lock) {
            shutdownRequested = false;
            batchInFlight = false;
            failure = null;
            recreateRequested = false;
            lastBatchNanos = 0L;
            intervalEmaNanos = 0.0;
        }
    }

    private void ensureWorker() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        worker = new Thread(this::runWorker, "SR-FrameGeneration-Pacer");
        worker.setDaemon(true);
        worker.start();
    }

    private void runWorker() {
        while (true) {
            List<Integer> images;
            long spacing;
            PresentFunction presentFunction;
            synchronized (lock) {
                while (pendingImages == null && !shutdownRequested) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        // Shutdown uses the flag; spurious interrupts are ignored.
                    }
                }
                if (shutdownRequested) {
                    return;
                }
                images = pendingImages;
                spacing = pendingSpacingNanos;
                presentFunction = pendingPresentFunction;
                pendingImages = null;
                pendingPresentFunction = null;
                batchInFlight = true;
            }

            try {
                long startNanos = System.nanoTime();
                for (int i = 0; i < images.size(); i++) {
                    sleepUntil(startNanos + (long) i * spacing);
                    int result = presentFunction.present(images.get(i));
                    if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                        synchronized (lock) {
                            recreateRequested = true;
                        }
                        break;
                    }
                    if (result == VK_SUBOPTIMAL_KHR) {
                        synchronized (lock) {
                            recreateRequested = true;
                        }
                    } else if (result != VK_SUCCESS) {
                        throw new IllegalStateException(
                                "Failed to present a generated frame, VkResult=" + result
                        );
                    }
                }
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.warn("Frame generation present pacing failed", throwable);
                synchronized (lock) {
                    failure = throwable;
                }
            } finally {
                synchronized (lock) {
                    batchInFlight = false;
                    lock.notifyAll();
                }
            }
        }
    }

    private static void sleepUntil(long targetNanos) {
        while (true) {
            long remaining = targetNanos - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            LockSupport.parkNanos(Math.min(remaining, 2_000_000L));
        }
    }
}
