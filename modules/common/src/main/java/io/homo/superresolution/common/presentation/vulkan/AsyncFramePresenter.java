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

import io.homo.superresolution.api.registry.framegeneration.ProviderInputSnapshot;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.common.presentation.capture.CaptureFrameRing;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

final class AsyncFramePresenter implements AutoCloseable {
    static final int GENERATION_QUEUE_CAPACITY = CaptureFrameRing.MAX_IN_FLIGHT_FRAMES - 1;
    static final int MAX_GENERATED_FRAMES = 5;
    static final int MAX_PRESENTS_PER_BATCH = MAX_GENERATED_FRAMES + 1;
    static final int PRESENTATION_QUEUE_CAPACITY = MAX_PRESENTS_PER_BATCH;
    private static final long THREAD_JOIN_TIMEOUT_NANOS = 2_000_000_000L;

    final Object stateLock = new Object();
    final FrameQueue<FrameGenerationWork> generationQueue =
            new FrameQueue<>(GENERATION_QUEUE_CAPACITY);
    final FrameQueue<PresentImageBatch> presentationQueue =
            new FrameQueue<>(PRESENTATION_QUEUE_CAPACITY, PresentImageBatch::imageCount);
    final PresentPacer pacer;
    private final String providerId;
    private final AtomicLong nextRealIndex = new AtomicLong();
    private final FramePacingTiming framePacingTiming;
    private final FrameGenerationWorker generationWorker;
    private final PresentWorker presentWorker;
    private long nextBatchId;
    private volatile Throwable failure;
    private Runnable terminalTeardown;

    AsyncFramePresenter(VulkanSwapchain swapchain, VulkanDevice device, String providerId) {
        this(swapchain, device, providerId, swapchain.framePacingTiming(),
                swapchain.presentPacer(), System::nanoTime);
    }

    AsyncFramePresenter(
            VulkanSwapchain swapchain, VulkanDevice device, String providerId, NanoClock clock
    ) {
        this(swapchain, device, providerId, new FramePacingTiming(clock::nanoTime), clock);
    }

    private AsyncFramePresenter(
            VulkanSwapchain swapchain, VulkanDevice device, String providerId,
            FramePacingTiming timing, NanoClock clock
    ) {
        this(swapchain, device, providerId, timing, new PresentPacer(clock, timing), clock);
    }

    private AsyncFramePresenter(
            VulkanSwapchain swapchain, VulkanDevice device, String providerId,
            FramePacingTiming timing, PresentPacer pacer, NanoClock clock
    ) {
        if (!device.asyncDispatchCapabilities().available()
                || !providerId.equals(device.asyncDispatchCapabilities().providerId())) {
            throw new IllegalStateException("The presenter requires the selected provider's async queues");
        }
        this.providerId = providerId;
        this.framePacingTiming = timing;
        this.pacer = pacer;
        this.generationWorker = new FrameGenerationWorker(this, swapchain, device, providerId);
        this.presentWorker = new PresentWorker(this, swapchain, device);
        presentWorker.start();
        generationWorker.start();
    }

    String providerId() {
        return providerId;
    }

    boolean enqueue(FrameResources resources, boolean presentAllowed) {
        throwIfFailed();
        if (!FrameGeneration.isApplicationManagedPresenterCompatible(providerId)) {
            throw new IllegalStateException("Drain the presenter before switching frame-generation providers");
        }
        long realIndex = nextRealIndex.getAndIncrement();
        ProviderInputSnapshot snapshot = null;
        if (presentAllowed) {
            FramePacingTrace.Span snapshotTrace = FramePacingTrace.INSTANCE.begin(
                    "frame_generation_input_snapshot",
                    resources.logicalFrameIndex(),
                    realIndex,
                    -1L,
                    -1L,
                    -1L,
                    "REAL",
                    providerId
            );
            try {
                snapshot = FrameGeneration.captureProviderInputSnapshotForFrame(providerId, resources);
                if (snapshot != null && (!providerId.equals(snapshot.providerId())
                        || snapshot.logicalFrameIndex() != resources.logicalFrameIndex())) {
                    SuperResolution.LOGGER.warn("Provider '{}' returned mismatched input for real frame {}",
                            providerId, realIndex);
                    snapshot = null;
                }
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.warn("Failed to capture provider '{}' input; using real-only fallback",
                        providerId, throwable);
            } finally {
                snapshotTrace.close();
            }
        }
        FrameGenerationWork work = new FrameGenerationWork(
                realIndex, resources.logicalFrameIndex(), LowLatency.currentLatencyFrameId(),
                presentAllowed ? VulkanLowLatency.claimCurrentFramePresentId() : 0L,
                resources, snapshot, pacer.takeRealFrameProducerTimeNanos(),
                Math.min(Math.max(0, FrameGeneration.plannedGeneratedFrameCount()), MAX_GENERATED_FRAMES),
                snapshot != null && snapshot.historyResetRequested(), presentAllowed
        );
        resources.markQueued();
        FramePacingTrace.Span enqueueTrace = FramePacingTrace.INSTANCE.begin(
                "frame_generation_enqueue_wait",
                work.logicalFrameIndex(),
                work.realIndex(),
                -1L,
                -1L,
                work.realPresentId(),
                "REAL",
                providerId
        );
        try {
            framePacingTiming.recordExcludedWait(generationQueue.put(work));
            return true;
        } catch (InterruptedException exception) {
            resources.markUnrecoverable();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while enqueueing a real frame", exception);
        } catch (IllegalStateException exception) {
            resources.markUnrecoverable();
            throwIfFailed();
            throw exception;
        } finally {
            enqueueTrace.close();
        }
    }

    long nextBatchId() {
        return ++nextBatchId;
    }

    void awaitPresentationDrain() {
        synchronized (stateLock) {
            generationWorker.pause();
            presentWorker.pause();
            awaitState(() -> !generationWorker.isInFlight()
                    && presentationQueue.isEmpty() && !presentWorker.isInFlight());
        }
        throwIfFailed();
    }

    void awaitPresentIdle() {
        presentWorker.awaitIdle();
    }

    void resumePresenting() {
        synchronized (stateLock) {
            generationWorker.resume();
            presentWorker.resume();
        }
    }

    void shutdownProviderOnFrameGenerationThread(String expectedProviderId, Runnable teardown) {
        synchronized (stateLock) {
            if (!providerId.equals(expectedProviderId) || generationWorker.isTerminated()
                    || terminalTeardown != null) {
                throw new IllegalStateException("Provider teardown does not match the live presenter lifecycle");
            }
            terminalTeardown = teardown;
            generationQueue.close();
            resumePresenting();
        }
        join(generationWorker.thread(), () -> {
        });
        throwIfFailed();
    }

    @Override
    public void close() {
        generationQueue.close();
        resumePresenting();
        join(generationWorker.thread(), () -> {
        });
        join(presentWorker.thread(), () -> {
        });
        throwIfFailed();
    }

    void awaitPresentTermination(Runnable releasePending) {
        join(presentWorker.thread(), releasePending);
    }

    boolean isTerminated() {
        return generationWorker.isTerminated() && presentWorker.isTerminated();
    }

    void runTerminalTeardown() {
        Runnable teardown;
        synchronized (stateLock) {
            teardown = terminalTeardown;
        }
        if (teardown != null) {
            teardown.run();
        }
    }

    boolean hasFailed() {
        return failure != null;
    }

    void throwIfFailed() {
        if (failure != null) {
            throw new IllegalStateException("Application-managed frame presenter failed", failure);
        }
    }

    void fail(Throwable throwable) {
        synchronized (stateLock) {
            if (failure == null) {
                failure = throwable;
                SuperResolution.LOGGER.warn("Application-managed frame presenter stopped", throwable);
            }
            generationQueue.close();
            presentationQueue.close();
            resumePresenting();
            stateLock.notifyAll();
        }
    }

    void retireBatch(FrameGenerationWorker.RetiredBatch batch) {
        generationWorker.retireBatch(batch);
    }

    void awaitState(BooleanSupplier complete) {
        boolean interrupted = false;
        while (!complete.getAsBoolean()) {
            try {
                stateLock.wait();
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void join(Thread thread, Runnable maintenance) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + THREAD_JOIN_TIMEOUT_NANOS;
        while (thread.isAlive() && System.nanoTime() < deadline) {
            try {
                thread.join(20L);
            } catch (InterruptedException exception) {
                interrupted = true;
            }
            maintenance.run();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IllegalStateException("Presenter worker did not terminate before teardown");
        }
    }

    @FunctionalInterface
    interface NanoClock {
        long nanoTime();
    }
}
