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

import io.homo.superresolution.api.registry.framegeneration.*;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.core.graphics.vulkan.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.vulkan.VK10.*;

final class FrameGenerationWorker {
    private final AsyncFramePresenter presenter;
    private final VulkanSwapchain swapchain;
    private final VulkanDevice device;
    private final String providerId;
    private final FramePacingEstimator estimator;
    private final VulkanCommandBufferRing commandBuffers;
    private final ArrayDeque<Long> semaphorePool = new ArrayDeque<>();
    private final ConcurrentLinkedQueue<RetiredBatch> pendingReleases = new ConcurrentLinkedQueue<>();
    private final Thread thread;
    private long nextDisplayIndex;
    private boolean paused;
    private boolean inFlight;
    private volatile boolean terminated;

    FrameGenerationWorker(
            AsyncFramePresenter presenter, VulkanSwapchain swapchain, VulkanDevice device,
            String providerId, FramePacingTiming timing, AsyncFramePresenter.NanoClock clock
    ) {
        this.presenter = presenter;
        this.swapchain = swapchain;
        this.device = device;
        this.providerId = providerId;
        this.estimator = new FramePacingEstimator(providerId, timing, clock::nanoTime);
        this.commandBuffers = new VulkanCommandBufferRing(
                (AsyncFramePresenter.GENERATION_QUEUE_CAPACITY + 1) * AsyncFramePresenter.MAX_GENERATED_FRAMES,
                device.requireFgCommandPool()
        );
        this.thread = new Thread(this::runLoop, "SR-FrameGeneration-FG");
        thread.setDaemon(true);
    }

    private static long intervalNanos(long period, int generatedCount) {
        return Math.max(500_000L, Math.min(100_000_000L, period / (generatedCount + 1L)));
    }

    private static String validateDispatchResult(
            FrameGenerationDispatchResult result, FrameGenerationDispatchInput input
    ) {
        if (result == null) {
            return "Provider returned null";
        }
        if (!result.succeeded()) {
            return result.failureReason();
        }
        FrameGenerationProviderOutput output = result.output();
        if (output == null) {
            return "Successful dispatch did not return an output lease";
        }
        if (output.isReleased()) {
            return "Provider returned an already released output lease";
        }
        if (result.actualGeneratedCount() > input.requestedGeneratedFrameCount()
                || result.actualGeneratedCount() > input.commandBufferCount()) {
            return "Provider returned more outputs than the dispatch can submit";
        }
        if (result.generatedOutputs().size() != result.actualGeneratedCount()) {
            return "Generated output count does not match actualGeneratedCount";
        }
        FrameGenerationProviderOutput.OutputKey key = output.outputKey();
        if (key.width() != input.outputWidth() || key.height() != input.outputHeight()) {
            return "Provider output extent does not match the dispatch";
        }
        List<VulkanTexture> outputs = new ArrayList<>(result.generatedOutputs());
        if (result.realOutput() != null) {
            outputs.add(result.realOutput());
        }
        for (VulkanTexture texture : outputs) {
            if (texture.getWidth() != key.width() || texture.getHeight() != key.height()
                    || texture.getTextureFormat().vk() != key.format()) {
                return "Provider output does not match its lease key";
            }
        }
        return null;
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

    void retireBatch(RetiredBatch batch) {
        pendingReleases.add(batch);
        presenter.generationQueue.signalConsumer();
    }

    private void runLoop() {
        try {
            while (true) {
                releasePendingBatches();
                FrameQueue.HeadResult<FrameGenerationWork> head =
                        presenter.generationQueue.awaitHead(() -> !pendingReleases.isEmpty());
                if (head.closedAndEmpty()) {
                    break;
                }
                if (head.externalWake()) {
                    continue;
                }
                FrameGenerationWork work = head.value();
                synchronized (presenter.stateLock) {
                    while (paused) {
                        presenter.stateLock.wait();
                    }
                    inFlight = true;
                }
                try {
                    work.frameResources().markDispatching();
                    ProviderInputSnapshot snapshot = work.providerInputSnapshot();
                    int requiredCapacity = snapshot == null ? 1
                            : Math.min(snapshot.mode().generatedFrameCount(), AsyncFramePresenter.MAX_GENERATED_FRAMES) + 1;
                    presenter.presentationQueue.awaitCapacityFor(requiredCapacity);
                    PresentImageBatch batch = dispatch(work);
                    try {
                        presenter.presentationQueue.put(batch);
                    } catch (Throwable throwable) {
                        work.frameResources().markUnrecoverable();
                        releaseUnpublishedBatch(batch);
                        throw throwable;
                    }
                    nextDisplayIndex += batch.imageCount();
                    presenter.generationQueue.removeHead(work);
                } finally {
                    synchronized (presenter.stateLock) {
                        inFlight = false;
                        presenter.stateLock.notifyAll();
                    }
                }
            }
        } catch (Throwable throwable) {
            presenter.fail(throwable);
            markRemainingInputsUnrecoverable();
        } finally {
            presenter.presentationQueue.close();
            boolean presentDrained = false;
            try {
                presenter.awaitPresentTermination(this::releasePendingBatches);
                presentDrained = true;
                releasePendingBatches();
                presenter.runTerminalTeardown();
            } catch (Throwable throwable) {
                presenter.fail(throwable);
            } finally {
                try {
                    if (presentDrained) {
                        commandBuffers.destroy();
                        while (!semaphorePool.isEmpty()) {
                            destroySemaphore(semaphorePool.removeFirst());
                        }
                    }
                } catch (Throwable throwable) {
                    presenter.fail(throwable);
                } finally {
                    synchronized (presenter.stateLock) {
                        terminated = true;
                        presenter.stateLock.notifyAll();
                    }
                }
            }
        }
    }

    private PresentImageBatch dispatch(FrameGenerationWork work) {
        VulkanSwapchain.PresentationConfiguration configuration = swapchain.presentationConfiguration();
        long batchId = presenter.nextBatchId();
        long period = estimator.observeRealFrame(work, configuration.generation());
        ProviderInputSnapshot snapshot = work.providerInputSnapshot();
        if (!work.presentAllowed() || !work.frameResources().hasFinalColor()
                || !configuration.available() || snapshot == null) {
            return buildRealOnlyBatch(work, batchId, configuration, period);
        }

        List<VulkanCommandBuffer> buffers = new ArrayList<>();
        FrameGenerationDispatchResult result = null;
        long[] handoffs = new long[0];
        int submittedCount = 0;
        VulkanTimestampProfiler profiler = device.timestampProfiler();
        int timestampSlot = -1;
        try {
            int bufferCount = Math.max(1,
                    Math.min(snapshot.mode().generatedFrameCount(), AsyncFramePresenter.MAX_GENERATED_FRAMES));
            long[] handles = new long[bufferCount];
            for (int index = 0; index < bufferCount; index++) {
                VulkanCommandBuffer buffer = commandBuffers.acquire(device);
                buffers.add(buffer);
                buffer.reset();
                buffer.begin();
                handles[index] = buffer.getNativeCommandBuffer().address();
            }
            if (profiler != null) {
                timestampSlot = profiler.beginRegion(buffers.get(0).getNativeCommandBuffer(),
                        PerformanceTracker.VK_FRAME_GEN);
            }
            FrameGenerationDispatchInput input = new FrameGenerationDispatchInput(
                    work.frameResources(), snapshot, device, handles,
                    configuration.width(), configuration.height(), configuration.format()
            );
            result = FrameGeneration.dispatchAsync(input);
            String invalidReason = validateDispatchResult(result, input);
            if (invalidReason != null) {
                abortDispatch(result, buffers, handoffs, 0);
                if (profiler != null && timestampSlot >= 0) {
                    profiler.cancelRegion(timestampSlot);
                }
                SuperResolution.LOGGER.debug("Provider '{}' used real-only fallback for frame {}: {}",
                        providerId, work.realIndex(), invalidReason);
                return buildRealOnlyBatch(work, batchId, configuration, period);
            }

            int submissionCount = Math.max(1, result.actualGeneratedCount());
            if (profiler != null && timestampSlot >= 0) {
                profiler.endRegion(buffers.get(submissionCount - 1).getNativeCommandBuffer(), timestampSlot);
            }
            for (int index = 0; index < buffers.size(); index++) {
                if (index < submissionCount) {
                    buffers.get(index).end();
                } else {
                    buffers.get(index).reset();
                }
            }
            handoffs = new long[result.actualGeneratedCount() + 1];
            for (int index = 0; index < handoffs.length; index++) {
                handoffs[index] = acquireSemaphore();
            }

            FrameGenerationDispatchCompletion[] completions =
                    new FrameGenerationDispatchCompletion[handoffs.length];
            long[] inputWaits = work.frameResources().readySemaphores();
            long[] captureSignals = result.realOutput() != null
                    ? work.frameResources().releaseSemaphores()
                    : new long[0];
            VulkanLowLatency.notifyFrameGenerationQueueOutOfBand(device.requireFgQueue());
            VulkanLowLatency.renderSubmitMarker(work.realPresentId(), true, true);
            long lastFence = 0L;
            try {
                for (int index = 0; index < submissionCount; index++) {
                    boolean last = index == submissionCount - 1;
                    long[] waits = index == 0 ? inputWaits : new long[0];
                    int[] stages = new int[waits.length];
                    Arrays.fill(stages, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
                    int outputSignals = result.actualGeneratedCount() > 0 ? 1 : 0;
                    long[] signals = new long[outputSignals + (last ? 1 + captureSignals.length : 0)];
                    int cursor = 0;
                    if (outputSignals != 0) {
                        signals[cursor++] = handoffs[index];
                    }
                    if (last) {
                        signals[cursor++] = handoffs[handoffs.length - 1];
                        System.arraycopy(captureSignals, 0, signals, cursor, captureSignals.length);
                    }
                    lastFence = submitProviderWork(buffers.get(index), waits, stages, signals);
                    submittedCount++;
                    FrameGenerationDispatchCompletion completion = GpuReadyFences.completion(buffers.get(index));
                    if (outputSignals != 0) {
                        completions[index] = completion;
                    }
                    if (last) {
                        completions[completions.length - 1] = completion;
                    }
                }
            } finally {
                VulkanLowLatency.renderSubmitMarker(work.realPresentId(), true, false);
            }
            GpuReadyFences ready = new GpuReadyFences(completions, handoffs,
                    semaphorePool::addLast, this::destroySemaphore);
            PresentImageBatch batch = buildPresentImageBatch(work, batchId, configuration, period, result, ready);
            if (result.realOutput() != null) {
                work.frameResources().markSubmitted(buffers.get(submissionCount - 1), lastFence);
            }
            return batch;
        } catch (Throwable throwable) {
            if (submittedCount > 0) {
                work.frameResources().markUnrecoverable();
                abortDispatch(result, buffers, handoffs, submittedCount);
                throw throwable;
            }
            abortDispatch(result, buffers, handoffs, 0);
            if (profiler != null && timestampSlot >= 0) {
                profiler.cancelRegion(timestampSlot);
            }
            SuperResolution.LOGGER.warn("Provider '{}' dispatch failed for frame {}; using real-only fallback",
                    providerId, work.realIndex(), throwable);
            return buildRealOnlyBatch(work, batchId, configuration, period);
        }
    }

    private long submitProviderWork(
            VulkanCommandBuffer buffer, long[] waits, int[] stages, long[] signals
    ) {
        return device.submitCommandBuffer(device.requireFgQueue(), buffer, waits, stages, signals);
    }

    private PresentImageBatch buildPresentImageBatch(
            FrameGenerationWork work, long batchId, VulkanSwapchain.PresentationConfiguration configuration,
            long period, FrameGenerationDispatchResult result, GpuReadyFences ready
    ) {
        int count = result.actualGeneratedCount();
        VulkanLowLatency.PresentBatchIds ids = VulkanLowLatency.reservePresentBatch(work.realPresentId(), count);
        long[] generatedIds = ids.generatedPresentIds();
        List<PresentImage> images = new ArrayList<>(count + 1);
        for (int index = 0; index < count; index++) {
            images.add(new PresentImage(nextDisplayIndex + index, work.realIndex(), work.latencyFrameId(),
                    PresentImage.Kind.GENERATED, result.generatedOutputs().get(index), generatedIds[index], true));
        }
        images.add(new PresentImage(nextDisplayIndex + count, work.realIndex(), work.latencyFrameId(),
                PresentImage.Kind.REAL, result.realOutput() != null ? result.realOutput()
                : work.frameResources().finalColorVulkanTexture(), ids.realPresentId(), false));
        return new PresentImageBatch(work, batchId, images, configuration, intervalNanos(period, count),
                estimator.onBatchResult(count, result.historyDisposition()), result.output(),
                ready, result.historyDisposition());
    }

    private PresentImageBatch buildRealOnlyBatch(
            FrameGenerationWork work, long batchId, VulkanSwapchain.PresentationConfiguration configuration,
            long period
    ) {
        estimator.onBatchResult(0, null);
        long presentId = work.presentAllowed()
                ? VulkanLowLatency.reservePresentBatch(work.realPresentId(), 0).realPresentId() : 0L;
        PresentImage real = new PresentImage(nextDisplayIndex, work.realIndex(), work.latencyFrameId(),
                PresentImage.Kind.REAL, work.frameResources().finalColorVulkanTexture(), presentId, false);
        return new PresentImageBatch(work, batchId, List.of(real), configuration, intervalNanos(period, 0),
                false, null, GpuReadyFences.none(), null);
    }

    private void abortDispatch(
            FrameGenerationDispatchResult result, List<VulkanCommandBuffer> buffers,
            long[] handoffs, int submittedCount
    ) {
        for (int index = 0; index < buffers.size(); index++) {
            if (index < submittedCount) {
                buffers.get(index).waitForFence();
            } else {
                buffers.get(index).reset();
            }
        }
        for (long semaphore : handoffs) {
            if (semaphore != 0L) {
                if (submittedCount == 0) {
                    semaphorePool.addLast(semaphore);
                } else {
                    destroySemaphore(semaphore);
                }
            }
        }
        if (result != null && result.output() != null && !result.output().isReleased()) {
            if (submittedCount == 0) {
                result.output().abort();
            } else {
                result.completion().awaitCompletion();
                result.output().release();
            }
        }
    }

    private void releaseUnpublishedBatch(PresentImageBatch batch) {
        releaseBatch(new RetiredBatch(batch, List.of()));
    }

    private void releasePendingBatches() {
        RetiredBatch batch;
        while ((batch = pendingReleases.poll()) != null) {
            releaseBatch(batch);
        }
    }

    private void releaseBatch(RetiredBatch retired) {
        PresentImageBatch batch = retired.batch();
        for (FrameGenerationDispatchCompletion completion : retired.presentationCompletions()) {
            completion.awaitCompletion();
        }
        batch.gpuReadyFences().awaitAll();
        batch.providerCompletion().awaitCompletion();
        if (batch.output() != null) {
            batch.output().release();
        }
        batch.gpuReadyFences().release();
    }

    private long acquireSemaphore() {
        if (!semaphorePool.isEmpty()) {
            return semaphorePool.removeFirst();
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pointer = stack.mallocLong(1);
            VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            int result = vkCreateSemaphore(device.getVkDevice(), info, null, pointer);
            if (result != VK_SUCCESS) {
                throw new IllegalStateException("Failed to create FG output semaphore, VkResult=" + result);
            }
            return pointer.get(0);
        }
    }

    private void destroySemaphore(long semaphore) {
        vkDestroySemaphore(device.getVkDevice(), semaphore, null);
    }

    private void markRemainingInputsUnrecoverable() {
        boolean interrupted = Thread.interrupted();
        try {
            while (true) {
                FrameQueue.TakeResult<FrameGenerationWork> result = presenter.generationQueue.takeResult();
                if (result.closedAndEmpty()) {
                    return;
                }
                result.value().frameResources().markUnrecoverable();
            }
        } catch (InterruptedException exception) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    record RetiredBatch(
            PresentImageBatch batch,

            List<FrameGenerationDispatchCompletion> presentationCompletions
    ) {
        RetiredBatch {
            presentationCompletions = List.copyOf(presentationCompletions);
        }
    }
}
