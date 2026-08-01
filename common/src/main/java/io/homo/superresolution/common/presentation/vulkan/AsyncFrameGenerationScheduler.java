/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.ProviderInputSnapshot;
import io.homo.superresolution.api.registry.ProviderOutputLease;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.presentation.capture.CaptureFrameRing;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Application-managed frame scheduler. The render thread publishes immutable
 * real-frame jobs, the FG thread owns provider dispatch and batch construction,
 * and the present thread owns application-managed {@code vkQueuePresentKHR}.
 */
final class AsyncFrameGenerationScheduler implements AutoCloseable {
    static final int REAL_QUEUE_CAPACITY = CaptureFrameRing.MAX_IN_FLIGHT_FRAMES - 1;
    static final int MAX_GENERATED_FRAMES = 5;
    static final int PRESENT_QUEUE_CAPACITY =
            REAL_QUEUE_CAPACITY * (MAX_GENERATED_FRAMES + 1);

    private static final double INTERVAL_EMA_ALPHA = 0.2;
    private static final long DEFAULT_REAL_PERIOD_NANOS = 16_666_667L;
    private static final long MIN_REAL_PERIOD_NANOS = 1_000_000L;
    private static final long MAX_REAL_PERIOD_NANOS = 500_000_000L;
    private static final long MAX_PRESENT_INTERVAL_NANOS = 100_000_000L;
    private static final long FINAL_SPIN_WINDOW_NANOS = 200_000L;
    private static final long THREAD_JOIN_TIMEOUT_NANOS = 2_000_000_000L;

    private final VulkanSwapchain swapchain;
    private final VulkanDevice device;
    private final String providerId;
    private final FrameQueue<RealFrameJob> realFramesQueue =
            new FrameQueue<>(REAL_QUEUE_CAPACITY);
    private final FrameQueue<PresentFrame> presentFramesQueue =
            new FrameQueue<>(PRESENT_QUEUE_CAPACITY);
    private final ConcurrentLinkedQueue<ProviderLeaseRelease> pendingProviderReleases =
            new ConcurrentLinkedQueue<>();
    private final AtomicLong nextRealIndex = new AtomicLong();
    private final AtomicLong nextDisplayIndex = new AtomicLong();
    private final AtomicLong nextBatchId = new AtomicLong();
    private final Object presentState = new Object();
    private final NanoClock clock;

    private volatile Throwable failure;
    private boolean presentInFlight;
    private boolean presentPaused;
    private Thread fgThread;
    private Thread presentThread;
    private long previousSubmittedAtNanos;
    private boolean hasPreviousSubmittedAt;
    private double realPeriodEmaNanos;

    AsyncFrameGenerationScheduler(
            VulkanSwapchain swapchain,
            VulkanDevice device,
            String providerId
    ) {
        this(swapchain, device, providerId, System::nanoTime);
    }

    AsyncFrameGenerationScheduler(
            VulkanSwapchain swapchain,
            VulkanDevice device,
            String providerId,
            NanoClock clock
    ) {
        if (swapchain == null || device == null || clock == null) {
            throw new IllegalArgumentException("scheduler dependencies cannot be null");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId cannot be blank");
        }
        if (!device.asyncDispatchCapabilities().available()) {
            throw new IllegalStateException(
                    "Application-managed scheduler requires the Vulkan async foundation: "
                            + device.asyncDispatchCapabilities().unavailableReason()
            );
        }
        if (!providerId.equals(device.asyncDispatchCapabilities().providerId())) {
            throw new IllegalStateException(
                    "Application-managed scheduler provider '" + providerId
                            + "' does not match the Vulkan async foundation provider '"
                            + device.asyncDispatchCapabilities().providerId() + "'"
            );
        }
        this.swapchain = swapchain;
        this.device = device;
        this.providerId = providerId;
        this.clock = clock;
        startThreads();
    }

    String providerId() {
        return providerId;
    }

    boolean enqueue(FrameResources frameResources, boolean presentAllowed) {
        if (frameResources == null) {
            throw new IllegalArgumentException("frameResources cannot be null");
        }
        throwIfFailed();
        requireCompatibleProviderLifecycle();

        long realIndex = nextRealIndex.getAndIncrement();
        ProviderInputSnapshot providerInputSnapshot = null;
        if (presentAllowed) {
            try {
                providerInputSnapshot =
                        FrameGeneration.captureProviderInputSnapshotForFrame(
                                providerId,
                                frameResources
                        );
                if (providerInputSnapshot != null
                        && (!providerId.equals(providerInputSnapshot.providerId())
                        || providerInputSnapshot.logicalFrameIndex()
                        != frameResources.logicalFrameIndex())) {
                    SuperResolution.LOGGER.warn(
                            "Frame generation provider '{}' returned an input snapshot for "
                                    + "provider '{}' and logical frame {}; "
                                    + "using Real-only fallback for real frame {}",
                            providerId,
                            providerInputSnapshot.providerId(),
                            providerInputSnapshot.logicalFrameIndex(),
                            realIndex
                    );
                    providerInputSnapshot = null;
                }
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.warn(
                        "Failed to capture immutable input for frame generation provider '{}'; "
                                + "using Real-only fallback for real frame {}",
                        providerId,
                        realIndex,
                        throwable
                );
            }
        }

        RealFrameJob job = new RealFrameJob(
                realIndex,
                frameResources.logicalFrameIndex(),
                0L,
                0L,
                frameResources,
                providerInputSnapshot,
                clock.nanoTime(),
                providerInputSnapshot != null
                        && providerInputSnapshot.historyResetRequested(),
                presentAllowed
        );
        frameResources.markQueued();
        try {
            realFramesQueue.put(job);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while enqueueing a real frame", e);
        }
    }

    void awaitPresentIdle() {
        if (Thread.currentThread() == presentThread) {
            return;
        }
        synchronized (presentState) {
            presentPaused = true;
            presentState.notifyAll();
            boolean interrupted = false;
            while (!presentFramesQueue.isEmpty() || presentInFlight) {
                try {
                    presentState.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void resumePresenting() {
        synchronized (presentState) {
            presentPaused = false;
            presentState.notifyAll();
        }
    }

    void throwIfFailed() {
        Throwable pending = failure;
        if (pending != null) {
            throw new IllegalStateException(
                    "Application-managed presentation scheduler failed",
                    pending
            );
        }
    }

    int realQueueDepth() {
        return realFramesQueue.size();
    }

    int presentQueueDepth() {
        return presentFramesQueue.size();
    }

    @Override
    public void close() {
        realFramesQueue.close();
        join(fgThread);
        presentFramesQueue.close();
        join(presentThread);
        throwIfFailed();
    }

    private void requireCompatibleProviderLifecycle() {
        if (!FrameGeneration.isApplicationManagedSchedulerCompatible(providerId)) {
            throw new IllegalStateException(
                    "Cannot switch frame generation provider while scheduler provider '"
                            + providerId + "' still owns queued work"
            );
        }
    }

    private void startThreads() {
        presentThread = new Thread(this::runPresentLoop, "SR-FrameGeneration-Present");
        presentThread.setDaemon(true);
        presentThread.start();
        fgThread = new Thread(this::runFgLoop, "SR-FrameGeneration-FG");
        fgThread.setDaemon(true);
        fgThread.start();
    }

    private void runFgLoop() {
        try {
            while (true) {
                releasePendingProviderLeases();
                FrameQueue.HeadResult<RealFrameJob> head = realFramesQueue.awaitHead(
                        () -> !pendingProviderReleases.isEmpty()
                );
                if (head.externalWake()) {
                    continue;
                }
                if (head.closedAndEmpty()) {
                    return;
                }

                RealFrameJob job = head.value();
                job.frameResources().markDispatching();
                releasePendingProviderLeases();
                long batchId = nextBatchId.incrementAndGet();
                long realPeriodNanos = nextRealPeriod(job);
                FrameBatch batch = swapchain.submitApplicationManagedFrame(
                        job,
                        nextDisplayIndex.get(),
                        batchId,
                        realPeriodNanos,
                        providerId
                );
                if (batch != null) {
                    try {
                        presentFramesQueue.putBatch(batch.presentFrames());
                    } catch (Throwable throwable) {
                        releaseUnpublishedBatch(batch);
                        throw throwable;
                    }
                    nextDisplayIndex.addAndGet(batch.presentFrames().size());
                }
                realFramesQueue.removeHead(job);
            }
        } catch (Throwable throwable) {
            fail(throwable);
        } finally {
            presentFramesQueue.close();
            waitForPresentThreadToDrain();
            try {
                releasePendingProviderLeases();
            } catch (Throwable throwable) {
                recordFailure(throwable);
            }
        }
    }

    private void runPresentLoop() {
        long nextTick = 0L;
        try {
            while (true) {
                FrameQueue.TakeResult<PresentFrame> result = presentFramesQueue.takeResult();
                if (result.closedAndEmpty()) {
                    return;
                }
                PresentFrame frame = result.value();
                if (failure != null) {
                    releaseScheduledFrame(frame);
                    continue;
                }
                if (result.waited()) {
                    nextTick = 0L;
                }
                if (!swapchain.isCurrentGeneration(frame)) {
                    releaseScheduledFrame(frame);
                    continue;
                }
                synchronized (presentState) {
                    if (presentPaused) {
                        releaseScheduledFrame(frame);
                        presentState.notifyAll();
                        continue;
                    }
                    presentInFlight = true;
                }

                long interval = frame.batchIntervalNanos();
                long now = clock.nanoTime();
                if (nextTick == 0L) {
                    nextTick = now;
                }
                sleepUntil(nextTick);

                try {
                    device.requirePresentSubmitTimeline().awaitIssued(frame.submissionTicket());
                    int resultCode = swapchain.presentScheduledFrame(frame);
                    if (resultCode == VK_ERROR_OUT_OF_DATE_KHR
                            || resultCode == VK_SUBOPTIMAL_KHR) {
                        swapchain.requestRecreate();
                    } else if (resultCode != VK_SUCCESS) {
                        throw new IllegalStateException(
                                "Failed to present application-managed frame, VkResult="
                                        + resultCode
                        );
                    }
                } finally {
                    releaseScheduledFrame(frame);
                    synchronized (presentState) {
                        presentInFlight = false;
                        presentState.notifyAll();
                    }
                }

                nextTick += interval;
                long lateBy = clock.nanoTime() - nextTick;
                if (lateBy > Math.max(interval * 4L, MAX_PRESENT_INTERVAL_NANOS)) {
                    nextTick = clock.nanoTime();
                }
            }
        } catch (Throwable throwable) {
            fail(throwable);
            discardRemainingPresentFrames();
        } finally {
            synchronized (presentState) {
                presentInFlight = false;
                presentState.notifyAll();
            }
        }
    }

    private long nextRealPeriod(RealFrameJob job) {
        if (job.historyResetRequested()) {
            previousSubmittedAtNanos = 0L;
            hasPreviousSubmittedAt = false;
            realPeriodEmaNanos = 0.0;
        }
        long submittedAt = job.submittedAtNanos();
        if (hasPreviousSubmittedAt) {
            long sample = clamp(
                    submittedAt - previousSubmittedAtNanos,
                    MIN_REAL_PERIOD_NANOS,
                    MAX_REAL_PERIOD_NANOS
            );
            realPeriodEmaNanos = realPeriodEmaNanos <= 0.0
                    ? sample
                    : realPeriodEmaNanos * (1.0 - INTERVAL_EMA_ALPHA)
                            + sample * INTERVAL_EMA_ALPHA;
        }
        previousSubmittedAtNanos = submittedAt;
        hasPreviousSubmittedAt = true;
        return realPeriodEmaNanos <= 0.0
                ? DEFAULT_REAL_PERIOD_NANOS
                : Math.round(realPeriodEmaNanos);
    }

    private void releaseScheduledFrame(PresentFrame frame) {
        swapchain.discardScheduledFrame(frame);
        if (frame.kind() == PresentFrame.Kind.REAL && frame.sourceLease() != null) {
            pendingProviderReleases.add(
                    new ProviderLeaseRelease(
                            frame.sourceLease(),
                            frame.sourceCompletion()
                    )
            );
            realFramesQueue.signalConsumer();
        }
    }

    private void releaseUnpublishedBatch(FrameBatch batch) {
        for (PresentFrame frame : batch.presentFrames()) {
            swapchain.discardScheduledFrame(frame);
        }
        ProviderOutputLease lease = batch.providerOutputLease();
        if (lease != null) {
            releaseProviderLease(
                    new ProviderLeaseRelease(lease, batch.dispatchCompletion())
            );
        }
    }

    private void releasePendingProviderLeases() {
        ProviderLeaseRelease release;
        while ((release = pendingProviderReleases.poll()) != null) {
            releaseProviderLease(release);
        }
    }

    private void releaseProviderLease(ProviderLeaseRelease release) {
        release.completion().awaitCompletion();
        if (!release.lease().isReleased()) {
            release.lease().release();
        }
    }

    private void discardRemainingPresentFrames() {
        while (true) {
            try {
                FrameQueue.TakeResult<PresentFrame> result = presentFramesQueue.takeResult();
                if (result.closedAndEmpty()) {
                    return;
                }
                releaseScheduledFrame(result.value());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void waitForPresentThreadToDrain() {
        Thread thread = presentThread;
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        boolean interrupted = false;
        long deadline = System.nanoTime() + THREAD_JOIN_TIMEOUT_NANOS;
        while (thread.isAlive() && System.nanoTime() < deadline) {
            try {
                thread.join(20L);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            releasePendingProviderLeases();
        }
        if (thread.isAlive()) {
            thread.interrupt();
            recordFailure(new IllegalStateException(
                    "Present thread did not drain before scheduler shutdown"
            ));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void fail(Throwable throwable) {
        recordFailure(throwable);
        realFramesQueue.close();
        presentFramesQueue.close();
        synchronized (presentState) {
            presentState.notifyAll();
        }
    }

    private void recordFailure(Throwable throwable) {
        if (failure == null) {
            failure = throwable;
            SuperResolution.LOGGER.warn(
                    "Application-managed presentation scheduler stopped",
                    throwable
            );
        }
    }

    private void join(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        try {
            thread.join(2_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            thread.interrupt();
        }
    }

    private void sleepUntil(long targetNanos) {
        while (true) {
            long remaining = targetNanos - clock.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            if (remaining > FINAL_SPIN_WINDOW_NANOS) {
                LockSupport.parkNanos(remaining - FINAL_SPIN_WINDOW_NANOS);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ProviderLeaseRelease(
            ProviderOutputLease lease,
            FrameGenerationDispatchCompletion completion
    ) {
        private ProviderLeaseRelease {
            if (lease == null || completion == null) {
                throw new IllegalArgumentException(
                        "Provider lease release dependencies cannot be null"
                );
            }
        }
    }

    @FunctionalInterface
    interface NanoClock {
        long nanoTime();
    }
}
