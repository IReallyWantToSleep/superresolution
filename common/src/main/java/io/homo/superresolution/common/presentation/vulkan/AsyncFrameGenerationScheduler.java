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

import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.api.registry.ProviderInputSnapshot;
import io.homo.superresolution.api.registry.ProviderOutputLease;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.presentation.capture.CaptureFrameRing;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;

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
    static final int MAX_PRESENTS_PER_BATCH = MAX_GENERATED_FRAMES + 1;
    // This bounds queued present work. VulkanSwapchain separately tracks images that
    // remain acquired after the present thread removes them from this queue.
    static final int PRESENT_QUEUE_CAPACITY = MAX_PRESENTS_PER_BATCH;

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
    private final ConcurrentLinkedQueue<AcquireLeaseRelease> pendingAcquireReleases =
            new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ProviderLeaseRelease> pendingProviderReleases =
            new ConcurrentLinkedQueue<>();
    private final AtomicLong nextRealIndex = new AtomicLong();
    private final AtomicLong nextDisplayIndex = new AtomicLong();
    private final AtomicLong nextBatchId = new AtomicLong();
    private final Object presentState = new Object();
    private final Object closeState = new Object();
    private final FramePacingTiming framePacingTiming;
    private final FramePacingEstimator framePacingEstimator;
    private final NanoClock clock;

    private volatile Throwable failure;
    private Runnable terminalTeardown;
    private boolean fgTerminated;
    private boolean fgPauseRequested;
    private boolean fgInFlight;
    private boolean presentInFlight;
    private boolean presentPaused;
    private Thread fgThread;
    private Thread presentThread;

    AsyncFrameGenerationScheduler(
            VulkanSwapchain swapchain,
            VulkanDevice device,
            String providerId
    ) {
        this(
                swapchain,
                device,
                providerId,
                swapchain == null ? null : swapchain.framePacingTiming(),
                System::nanoTime
        );
    }

    AsyncFrameGenerationScheduler(
            VulkanSwapchain swapchain,
            VulkanDevice device,
            String providerId,
            NanoClock clock
    ) {
        this(
                swapchain,
                device,
                providerId,
                createTiming(clock),
                clock
        );
    }

    private AsyncFrameGenerationScheduler(
            VulkanSwapchain swapchain,
            VulkanDevice device,
            String providerId,
            FramePacingTiming framePacingTiming,
            NanoClock clock
    ) {
        if (swapchain == null
                || device == null
                || framePacingTiming == null
                || clock == null) {
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
        this.framePacingTiming = framePacingTiming;
        this.clock = clock;
        this.framePacingEstimator =
                new FramePacingEstimator(providerId, framePacingTiming, clock::nanoTime);
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

        long latencyFrameId = LowLatency
                .currentLatencyFrameId();
        long realPresentId = presentAllowed
                ? VulkanLowLatency
                        .claimCurrentFramePresentId()
                : 0L;
        int plannedGeneratedCount = Math.min(
                Math.max(0, FrameGeneration.plannedGeneratedFrameCount()),
                MAX_GENERATED_FRAMES
        );
        RealFrameJob job = new RealFrameJob(
                realIndex,
                frameResources.logicalFrameIndex(),
                latencyFrameId,
                realPresentId,
                frameResources,
                providerInputSnapshot,
                framePacingTiming.producerTimeNanos(),
                plannedGeneratedCount,
                providerInputSnapshot != null
                        && providerInputSnapshot.historyResetRequested(),
                presentAllowed
        );
        frameResources.markQueued();
        try {
            // The immutable timestamp is taken before publication. Recording the
            // completed put wait here removes it from the next producer interval.
            long excludedWaitNanos = realFramesQueue.put(job);
            framePacingTiming.recordExcludedWait(excludedWaitNanos);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while enqueueing a real frame", e);
        } catch (IllegalStateException e) {
            throwIfFailed();
            throw e;
        }
    }

    boolean isWorkerThread() {
        Thread currentThread = Thread.currentThread();
        return currentThread == fgThread || currentThread == presentThread;
    }

    void awaitPresentationDrain() {
        if (isWorkerThread()) {
            throw new IllegalStateException("Presentation drain cannot run on a scheduler worker thread");
        }
        synchronized (presentState) {
            fgPauseRequested = true;
            presentPaused = true;
            presentState.notifyAll();
            boolean interrupted = false;
            while (fgInFlight || !presentFramesQueue.isEmpty() || presentInFlight) {
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
            fgPauseRequested = false;
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

    void shutdownProviderOnFgThread(String expectedProviderId, Runnable teardown) {
        if (!providerId.equals(expectedProviderId)) {
            throw new IllegalStateException(
                    "Scheduler provider '" + providerId + "' does not match teardown provider '"
                            + expectedProviderId + "'"
            );
        }
        if (Thread.currentThread() == fgThread) {
            throw new IllegalStateException("FG thread cannot wait for its own terminal teardown");
        }
        if (teardown == null) {
            throw new IllegalArgumentException("terminal teardown cannot be null");
        }
        synchronized (closeState) {
            if (fgTerminated) {
                throw new IllegalStateException("FG thread has already terminated");
            }
            if (terminalTeardown != null) {
                throw new IllegalStateException("A terminal provider teardown is already registered");
            }
            terminalTeardown = teardown;
            realFramesQueue.close();
            resumeWorkersForShutdown();
        }
        awaitFgTermination();
        throwIfFailed();
    }

    @Override
    public void close() {
        if (Thread.currentThread() == fgThread) {
            throw new IllegalStateException("FG thread cannot close its own scheduler");
        }
        synchronized (closeState) {
            realFramesQueue.close();
            resumeWorkersForShutdown();
        }
        awaitFgTermination();
        presentFramesQueue.close();
        join(presentThread);
        throwIfFailed();
    }

    private void awaitFgTermination() {
        Thread thread = fgThread;
        if (thread == null) {
            return;
        }
        join(thread);
        synchronized (closeState) {
            if (!fgTerminated) {
                throw new IllegalStateException("FG thread did not terminate before scheduler shutdown");
            }
        }
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

    private void resumeWorkersForShutdown() {
        synchronized (presentState) {
            fgPauseRequested = false;
            presentPaused = false;
            presentState.notifyAll();
        }
    }

    private void runFgLoop() {
        try {
            while (true) {
                releasePendingAcquireLeases();
                releasePendingProviderLeases();
                FrameQueue.HeadResult<RealFrameJob> head = realFramesQueue.awaitHead(
                        () -> !pendingAcquireReleases.isEmpty()
                                || !pendingProviderReleases.isEmpty()
                );
                if (head.externalWake()) {
                    continue;
                }
                if (head.closedAndEmpty()) {
                    return;
                }

                RealFrameJob job = head.value();
                synchronized (presentState) {
                    while (fgPauseRequested) {
                        presentState.notifyAll();
                        presentState.wait();
                    }
                    fgInFlight = true;
                }
                try {
                    job.frameResources().markDispatching();
                    releasePendingAcquireLeases();
                    releasePendingProviderLeases();
                    presentFramesQueue.awaitCapacityFor(maxPresentFramesFor(job));
                    long batchId = nextBatchId.incrementAndGet();
                    long realPeriodNanos = framePacingEstimator.observeRealFrame(
                            job,
                            swapchain.swapchainGeneration()
                    );
                    FrameBatch batch = swapchain.submitApplicationManagedFrame(
                            job,
                            nextDisplayIndex.get(),
                            batchId,
                            realPeriodNanos,
                            providerId,
                            framePacingEstimator
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
                } finally {
                    synchronized (presentState) {
                        fgInFlight = false;
                        presentState.notifyAll();
                    }
                }
            }
        } catch (Throwable throwable) {
            fail(throwable);
        } finally {
            presentFramesQueue.close();
            boolean presentDrained = waitForPresentThreadToDrain();
            try {
                if (!presentDrained) {
                    throw new IllegalStateException(
                            "Cannot run terminal provider teardown before present thread drains"
                    );
                }
                releasePendingAcquireLeases();
                releasePendingProviderLeases();
                runTerminalTeardown();
            } catch (Throwable throwable) {
                recordFailure(throwable);
            } finally {
                synchronized (closeState) {
                    fgTerminated = true;
                    closeState.notifyAll();
                }
            }
        }
    }

    private static int maxPresentFramesFor(RealFrameJob job) {
        ProviderInputSnapshot snapshot = job.providerInputSnapshot();
        if (snapshot == null) {
            return 1;
        }
        int generatedCount = Math.min(
                Math.max(0, snapshot.mode().generatedFrameCount()),
                MAX_GENERATED_FRAMES
        );
        return generatedCount + 1;
    }

    private void runPresentLoop() {
        long nextTick = 0L;
        boolean previousPacingEnabled = false;
        int previousGeneratedCount = -1;
        try {
            while (true) {
                FrameQueue.TakeResult<PresentFrame> result = presentFramesQueue.takeResult();
                if (result.closedAndEmpty()) {
                    return;
                }
                PresentFrame frame = result.value();
                if (failure != null) {
                    nextTick = 0L;
                    previousPacingEnabled = false;
                    previousGeneratedCount = -1;
                    releaseScheduledFrame(frame);
                    continue;
                }
                if (!swapchain.isCurrentGeneration(frame)) {
                    nextTick = 0L;
                    previousPacingEnabled = false;
                    previousGeneratedCount = -1;
                    releaseScheduledFrame(frame);
                    continue;
                }
                synchronized (presentState) {
                    if (presentPaused) {
                        nextTick = 0L;
                        previousPacingEnabled = false;
                        previousGeneratedCount = -1;
                        releaseScheduledFrame(frame);
                        presentState.notifyAll();
                        continue;
                    }
                    presentInFlight = true;
                }

                long interval = frame.batchIntervalNanos();
                long now = clock.nanoTime();
                boolean pacingEnabled = frame.pacingEnabled();
                if (!pacingEnabled) {
                    nextTick = 0L;
                } else {
                    boolean resetTimeline = result.waited()
                            || !previousPacingEnabled
                            || previousGeneratedCount != frame.batchGeneratedCount();
                    if (resetTimeline || nextTick == 0L) {
                        nextTick = now;
                    }
                    sleepUntil(nextTick);
                }

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

                if (pacingEnabled) {
                    nextTick += interval;
                    long lateBy = clock.nanoTime() - nextTick;
                    if (lateBy > Math.max(interval * 4L, MAX_PRESENT_INTERVAL_NANOS)) {
                        nextTick = clock.nanoTime();
                    }
                } else {
                    nextTick = 0L;
                }
                previousPacingEnabled = pacingEnabled;
                previousGeneratedCount = frame.batchGeneratedCount();
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

    private void releaseScheduledFrame(PresentFrame frame) {
        swapchain.releaseScheduledTarget(frame);
        pendingAcquireReleases.add(
                new AcquireLeaseRelease(frame, frame.acquireCompletion())
        );
        realFramesQueue.signalConsumer();
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
        batch.dispatchCompletion().awaitCompletion();
        for (PresentFrame frame : batch.presentFrames()) {
            swapchain.releaseScheduledTarget(frame);
            swapchain.discardScheduledFrame(frame);
        }
        ProviderOutputLease lease = batch.providerOutputLease();
        if (lease != null) {
            releaseProviderLease(
                    new ProviderLeaseRelease(lease, batch.dispatchCompletion())
            );
        }
    }

    private void releasePendingAcquireLeases() {
        AcquireLeaseRelease release;
        while ((release = pendingAcquireReleases.poll()) != null) {
            release.completion().awaitCompletion();
            swapchain.discardScheduledFrame(release.frame());
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

    private void runTerminalTeardown() {
        Runnable teardown;
        synchronized (closeState) {
            teardown = terminalTeardown;
        }
        if (teardown != null) {
            teardown.run();
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

    private boolean waitForPresentThreadToDrain() {
        Thread thread = presentThread;
        if (thread == null || thread == Thread.currentThread()) {
            return true;
        }
        boolean interrupted = false;
        long deadline = System.nanoTime() + THREAD_JOIN_TIMEOUT_NANOS;
        while (thread.isAlive() && System.nanoTime() < deadline) {
            try {
                thread.join(20L);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            releasePendingAcquireLeases();
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
        return !thread.isAlive();
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
        boolean interrupted = false;
        long deadline = System.nanoTime() + THREAD_JOIN_TIMEOUT_NANOS;
        while (thread.isAlive() && System.nanoTime() < deadline) {
            try {
                thread.join(20L);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IllegalStateException("Scheduler worker did not terminate before teardown");
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
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

    private static FramePacingTiming createTiming(NanoClock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        return new FramePacingTiming(clock::nanoTime);
    }

    private record AcquireLeaseRelease(
            PresentFrame frame,
            FrameGenerationDispatchCompletion completion
    ) {
        private AcquireLeaseRelease {
            if (frame == null || completion == null) {
                throw new IllegalArgumentException(
                        "Acquire lease release dependencies cannot be null"
                );
            }
        }
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
