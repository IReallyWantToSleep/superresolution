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

import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchCompletion;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/** Owns swapchain preparation, ordered presentation and all pacing sleeps. */
final class PresentWorker {
    private final AsyncFramePresenter presenter;
    private final VulkanSwapchain swapchain;
    private final VulkanDevice device;
    private final Thread thread;
    private final List<PendingAcquireRelease> pendingAcquireReleases = new ArrayList<>();
    private boolean paused;
    private boolean inFlight;
    private volatile boolean terminated;

    PresentWorker(
            AsyncFramePresenter presenter, VulkanSwapchain swapchain, VulkanDevice device
    ) {
        this.presenter = presenter;
        this.swapchain = swapchain;
        this.device = device;
        this.thread = new Thread(this::runLoop, "SR-FrameGeneration-Present");
        thread.setDaemon(true);
    }

    void start() {
        thread.start();
    }

    Thread thread() {
        return thread;
    }

    void pause() {
        paused = true;
    }

    void resume() {
        paused = false;
        presenter.stateLock.notifyAll();
    }

    boolean isInFlight() {
        return inFlight;
    }

    boolean isTerminated() {
        return terminated;
    }

    void awaitIdle() {
        synchronized (presenter.stateLock) {
            pause();
            presenter.awaitState(() -> presenter.presentationQueue.isEmpty() && !inFlight);
        }
        presenter.throwIfFailed();
    }

    private void runLoop() {
        try {
            while (true) {
                FrameQueue.HeadResult<PresentImageBatch> head = presenter.presentationQueue.awaitHead();
                if (head.closedAndEmpty()) {
                    return;
                }
                PresentImageBatch batch = head.value();
                boolean discard;
                synchronized (presenter.stateLock) {
                    inFlight = true;
                    discard = paused || presenter.hasFailed();
                }
                try {
                    releaseCompletedAcquires(false);
                    if (discard || !batch.presentAllowed()
                            || batch.images().get(batch.imageCount() - 1).source() == null
                            || !batch.configuration().available()
                            || !swapchain.isCurrentConfiguration(batch.configuration())) {
                        presenter.pacer.reset();
                        discardBatch(batch);
                    } else {
                        presentBatch(batch, head.waited());
                    }
                } catch (Throwable throwable) {
                    batch.frameResources().markUnrecoverable();
                    throw throwable;
                } finally {
                    presenter.presentationQueue.removeHead(batch);
                    synchronized (presenter.stateLock) {
                        inFlight = false;
                        presenter.stateLock.notifyAll();
                    }
                }
            }
        } catch (Throwable throwable) {
            presenter.fail(throwable);
            discardRemainingBatches();
        } finally {
            try {
                releaseCompletedAcquires(true);
            } catch (Throwable throwable) {
                presenter.fail(throwable);
            }
            presenter.pacer.reset();
            synchronized (presenter.stateLock) {
                inFlight = false;
                terminated = true;
                presenter.stateLock.notifyAll();
            }
        }
    }

    private void presentBatch(PresentImageBatch batch, boolean waited) {
        List<PreparedImage> prepared = new ArrayList<>(batch.imageCount());
        List<FrameGenerationDispatchCompletion> completions = new ArrayList<>();
        boolean captureReleased = batch.captureReleasedByGeneration();
        try {
            swapchain.ensurePresentBatchFits(batch.imageCount());
            // Submit the real image before pacing, so borrowed-input release publication
            // is not held behind the generated images' display intervals.
            for (int index = 0; index < batch.imageCount(); index++) {
                PreparedImage image = new PreparedImage(swapchain.acquirePresentTarget(), batch.images().get(index));
                prepared.add(image);
                boolean real = index == batch.generatedCount();
                long[] waits;
                if (batch.gpuReadyFences().size() == 0) {
                    waits = index == 0 ? batch.frameResources().readySemaphores() : new long[0];
                } else {
                    waits = new long[]{batch.gpuReadyFences().handoffSemaphoreFor(index)};
                }
                long[] signals = real && !captureReleased ? batch.captureReleaseSemaphores() : new long[0];
                try {
                    image.submission = swapchain.submitPresentBlit(image.target, image.image, waits, signals);
                    image.rendered = true;
                } catch (VulkanDevice.SubmissionTicketPublicationException exception) {
                    image.rendered = true;
                    if (batch.gpuReadyFences().size() != 0) {
                        batch.gpuReadyFences().markConsumed(index);
                    }
                    if (real && !captureReleased) {
                        captureReleased = true;
                        batch.frameResources().markUnrecoverable();
                    }
                    throw exception;
                }
                completions.add(image.submission.completion());
                if (batch.gpuReadyFences().size() != 0) {
                    batch.gpuReadyFences().markConsumed(index);
                }
                if (real && !captureReleased) {
                    batch.frameResources().markSubmitted(
                            image.submission.commandBuffer(), image.submission.fence());
                    captureReleased = true;
                }
            }
            PresentPacer pacer = presenter.pacer;
            try {
                pacer.beginPresentFrameBatch(
                        waited, batch.pacingEnabled(), batch.generatedCount(), batch.intervalNanos());
                for (PreparedImage image : prepared) {
                    if (isPaused()) {
                        pacer.reset();
                        break;
                    }
                    if (image.image.kind() == PresentImage.Kind.GENERATED) {
                        pacer.sleepAtPresentGeneratedFrame();
                    } else {
                        pacer.sleepAtPresentRealFrame();
                    }
                    boolean presented = false;
                    pacer.beginPresentFrame();
                    try {
                        presentImage(image);
                        presented = true;
                    } finally {
                        pacer.endPresentFrame(presented && batch.pacingEnabled());
                    }
                }
            } finally {
                pacer.endPresentFrameBatch();
            }
        } catch (VulkanSwapchain.PresentTargetUnavailableException exception) {
            presenter.pacer.reset();
            swapchain.requestRecreate();
        } finally {
            try {
                releaseBatch(batch, prepared, completions, captureReleased);
            } catch (Throwable throwable) {
                batch.frameResources().markUnrecoverable();
                throw throwable;
            }
        }
    }

    private boolean isPaused() {
        synchronized (presenter.stateLock) {
            return paused || presenter.hasFailed();
        }
    }

    private void presentImage(PreparedImage image) {
        device.requirePresentSubmitTimeline().awaitIssued(image.submission.submissionTicket());
        int result = swapchain.presentTarget(image.target, image.image);
        image.presented = true;
        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
            swapchain.requestRecreate();
        } else if (result != VK_SUCCESS) {
            throw new IllegalStateException("Failed to present an application-managed image, VkResult=" + result);
        }
    }

    private void discardBatch(PresentImageBatch batch) {
        releaseBatch(batch, List.of(), new ArrayList<>(), batch.captureReleasedByGeneration());
    }

    private void releaseBatch(
            PresentImageBatch batch, List<PreparedImage> prepared,
            List<FrameGenerationDispatchCompletion> completions, boolean captureReleased
    ) {
        Throwable failure = null;
        try {
            List<Long> pendingWaits = new ArrayList<>();
            GpuReadyFences ready = batch.gpuReadyFences();
            for (int index = 0; index < ready.size(); index++) {
                if (!ready.isConsumed(index)) {
                    pendingWaits.add(ready.handoffSemaphoreFor(index));
                }
            }
            if (ready.size() == 0 && !captureReleased) {
                for (long semaphore : batch.frameResources().readySemaphores()) {
                    pendingWaits.add(semaphore);
                }
            }
            if (!captureReleased || !pendingWaits.isEmpty()) {
                long[] waits = pendingWaits.stream().mapToLong(Long::longValue).toArray();
                VulkanSwapchain.PresentBlitSubmission release = swapchain.submitPresentationRelease(waits,
                        captureReleased ? new long[0] : batch.captureReleaseSemaphores());
                completions.add(release.completion());
                for (int index = 0; index < ready.size(); index++) {
                    ready.markConsumed(index);
                }
                if (!captureReleased) {
                    batch.frameResources().markSubmitted(release.commandBuffer(), release.fence());
                }
            }
        } catch (Throwable throwable) {
            batch.frameResources().markUnrecoverable();
            failure = throwable;
        }
        try {
            for (PreparedImage image : prepared) {
                try {
                    if (image.presented) {
                        swapchain.releasePresentTarget();
                        pendingAcquireReleases.add(
                                new PendingAcquireRelease(image.target, image.submission.completion()));
                    } else {
                        if (image.submission != null) {
                            image.submission.completion().awaitCompletion();
                        }
                        swapchain.discardPresentTarget(image.target, image.rendered);
                    }
                } catch (Throwable throwable) {
                    if (failure == null) {
                        failure = throwable;
                    } else {
                        failure.addSuppressed(throwable);
                    }
                }
            }
        } finally {
            presenter.retireBatch(new FrameGenerationWorker.RetiredBatch(batch, completions));
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to retire a presentation batch", failure);
        }
    }

    private void releaseCompletedAcquires(boolean awaitAll) {
        Iterator<PendingAcquireRelease> iterator = pendingAcquireReleases.iterator();
        while (iterator.hasNext()) {
            PendingAcquireRelease release = iterator.next();
            if (awaitAll) {
                release.completion().awaitCompletion();
            } else if (!release.completion().isComplete()) {
                continue;
            }
            release.target().acquireLease().close();
            iterator.remove();
        }
    }

    private void discardRemainingBatches() {
        try {
            while (true) {
                FrameQueue.TakeResult<PresentImageBatch> result = presenter.presentationQueue.takeResult();
                if (result.closedAndEmpty()) {
                    return;
                }
                try {
                    discardBatch(result.value());
                } catch (Throwable throwable) {
                    result.value().frameResources().markUnrecoverable();
                    presenter.fail(throwable);
                }
            }
        } catch (Throwable throwable) {
            presenter.fail(throwable);
        } finally {
            synchronized (presenter.stateLock) {
                presenter.stateLock.notifyAll();
            }
        }
    }

    private static final class PreparedImage {
        private final VulkanSwapchain.PresentTarget target;
        private final PresentImage image;
        private VulkanSwapchain.PresentBlitSubmission submission;
        private boolean rendered;
        private boolean presented;

        private PreparedImage(VulkanSwapchain.PresentTarget target, PresentImage image) {
            this.target = target;
            this.image = image;
        }
    }

    private record PendingAcquireRelease(
            VulkanSwapchain.PresentTarget target,

            FrameGenerationDispatchCompletion completion
    ) {
    }
}
