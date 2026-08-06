/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchRequest;
import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.FrameGenerationDispatchCompletion;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.framegeneration.FramePresentPlan;
import io.homo.superresolution.api.registry.ProviderInputSnapshot;
import io.homo.superresolution.api.registry.ProviderOutputLease;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBufferRing;
import io.homo.superresolution.core.graphics.vulkan.VulkanBinarySemaphorePool;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanLowLatency;
import io.homo.superresolution.core.graphics.vulkan.VulkanQueue;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_FORMAT_FEATURE_TRANSFER_DST_BIT;

final class VulkanSwapchain {
    private static final int MAX_IN_FLIGHT_FRAMES = 3;
    private static final int DESIRED_SWAPCHAIN_IMAGES = 3;
    // FrameGenerationMode.X6 generates up to 5 frames; each needs its own acquire.
    private static final int MAX_GENERATED_FRAMES = 5;
    private static final int ACQUIRE_SYNC_SLOTS = MAX_IN_FLIGHT_FRAMES * (MAX_GENERATED_FRAMES + 1);
    private static final long MIN_APPLICATION_MANAGED_PRESENT_INTERVAL_NANOS = 500_000L;
    private static final long MAX_APPLICATION_MANAGED_PRESENT_INTERVAL_NANOS = 100_000_000L;
    private static final long ACQUIRE_TIMEOUT_NANOS = 100_000_000L;
    private static final int ACQUIRE_TIMEOUT = -1;
    private static final int ACQUIRE_OUT_OF_DATE = -2;
    private static final long[] NO_SEMAPHORES = new long[0];
    private static final int[] NO_STAGES = new int[0];

    private final VulkanPresentationContext context;
    private final VulkanSurface surface;
    private final VulkanDevice device;
    private final VulkanCommandBufferRing commandBuffers =
            new VulkanCommandBufferRing(MAX_IN_FLIGHT_FRAMES);
    private final PresentPacer pacer = new PresentPacer();
    private final Object swapchainLock = new Object();
    private final Object applicationManagedTargetLock = new Object();
    private final long[] imageAvailable = new long[ACQUIRE_SYNC_SLOTS];
    private long[] renderFinished = new long[0];
    private VulkanCommandBufferRing generatedBlitCommandBuffers;
    private VulkanCommandBufferRing applicationManagedCommandBuffers;
    private VulkanCommandBufferRing applicationManagedRealCommandBuffers;
    private VulkanBinarySemaphorePool applicationManagedAcquireSemaphores;
    private AsyncFrameGenerationScheduler applicationManagedScheduler;
    // 1x1 solid-color sources for the per-present cadence indicator (white = real
    // frame, cyan = interpolated); blitted into a corner of the swapchain image.
    private VulkanTexture realFrameIndicator;
    private VulkanTexture generatedFrameIndicator;
    private boolean indicatorCreationFailed;

    private long swapchain = VK_NULL_HANDLE;
    private long[] images = new long[0];
    private int[] imageLayouts = new int[0];
    private int width;
    private int height;
    private int syncIndex;
    private volatile boolean recreateRequested = true;
    private boolean vsync = true;
    private int imageFormat;
    private int imageCount;
    private int plannedGeneratedFrames;
    private volatile long swapchainGeneration;
    private int applicationManagedTargetCount;

    VulkanSwapchain(VulkanPresentationContext context, VulkanSurface surface) {
        this.context = context;
        this.surface = surface;
        this.device = context.device();
        createImageAvailableSemaphores();
        recreate();
    }

    private static VkImageSubresourceRange colorSubresource(MemoryStack stack) {
        return VkImageSubresourceRange.calloc(stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
    }

    private static int chooseCompositeAlpha(int supported) {
        int[] choices = {
                VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
                VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
                VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR,
                VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR
        };
        for (int choice : choices) {
            if ((supported & choice) != 0) {
                return choice;
            }
        }
        throw new IllegalStateException("Vulkan surface has no supported composite alpha mode");
    }

    private static int sourceAccessMask(int layout) {
        if (layout == VK_IMAGE_LAYOUT_UNDEFINED) {
            return 0;
        }
        if (layout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
            return VK_ACCESS_TRANSFER_READ_BIT;
        }
        return VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT;
    }

    private static int sourceStageMask(int sourceLayout, int swapchainLayout) {
        if (sourceLayout == VK_IMAGE_LAYOUT_UNDEFINED && swapchainLayout == VK_IMAGE_LAYOUT_UNDEFINED) {
            return VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        }
        return VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException("Failed to " + operation + ", VkResult=" + result);
        }
    }

    public void requestRecreate() {
        recreateRequested = true;
        synchronized (applicationManagedTargetLock) {
            applicationManagedTargetLock.notifyAll();
        }
    }

    public void suspendPresentation() {
        requestRecreate();
        if (applicationManagedScheduler != null) {
            applicationManagedScheduler.awaitPresentationDrain();
        } else {
            pacer.awaitIdle();
        }
        device.getMainQueue().waitIdle();
        if (applicationManagedScheduler != null && device.getFrameGenerationQueue() != null) {
            device.getFrameGenerationQueue().waitIdle();
        }
        waitForDedicatedPresentQueueIdle();
    }

    public void setVsync(boolean enabled) {
        if (vsync != enabled) {
            vsync = enabled;
            requestRecreate();
        }
    }

    public boolean present(FrameResources frame) {
        AsyncFrameGenerationScheduler scheduler = ensureApplicationManagedScheduler();
        if (scheduler != null) {
            updateApplicationManagedFramePlan();
            recreateIfRequestedOnControlThread();
            return scheduler.enqueue(frame, true);
        }
        PresentSubmission submission = submitPresentFrame(frame);
        if (submission == null) {
            return false;
        }

        try {
            if (submission.paced()) {
                return true;
            }
            return queuePresent(submission.imageIndex());
        } finally {
            FrameGeneration.finishPresent(frame, submission.plan());
        }
    }

    private PresentSubmission submitPresentFrame(FrameResources frame) {
        try {
            pacer.awaitIdle();
            pacer.throwIfFailed();
            if (pacer.consumeRecreateRequest()) {
                recreateRequested = true;
            }
            if (!frame.hasFinalColor()) {
                consumeWithoutPresentInternal(frame);
                return null;
            }
            plannedGeneratedFrames = Math.min(
                    FrameGeneration.plannedGeneratedFrameCount(),
                    MAX_GENERATED_FRAMES
            );
            if (plannedGeneratedFrames > 0 && imageCount < plannedGeneratedFrames + 2) {
                recreateRequested = true;
            }
            if (recreateRequested) {
                recreate();
            }
            if (swapchain == VK_NULL_HANDLE || width <= 0 || height <= 0) {
                consumeWithoutPresentInternal(frame);
                return null;
            }

            int syncSlot = nextImageAvailableIndex();
            int imageIndex = acquireImage(syncSlot);
            if (imageIndex < 0) {
                recreate();
                if (swapchain == VK_NULL_HANDLE) {
                    consumeWithoutPresentInternal(frame);
                    return null;
                }
                imageIndex = acquireImage(syncSlot);
                if (imageIndex < 0) {
                    requestRecreate();
                    consumeWithoutPresentInternal(frame);
                    return null;
                }
            }

            VulkanCommandBuffer commandBuffer = commandBuffers.acquire(device);
            commandBuffer.reset();
            commandBuffer.begin();
            FramePresentPlan plan = FrameGeneration.prepareFrame(
                    frame,
                    width,
                    height,
                    imageFormat,
                    imageCount,
                    commandBuffer.getNativeCommandBuffer().address()
            );
            // When DLSS-G produced an "output real" frame (indicator passthrough), present
            // it instead of the raw backbuffer so the DLSS-FG indicator stays steady.
            VulkanTexture realFrameSource = plan.realFrame() != null
                    ? plan.realFrame()
                    : frame.finalColorVulkanTexture();
            recordBlit(commandBuffer, realFrameSource, imageIndex, false);
            commandBuffer.end();

            long[] resourceWaits = frame.readySemaphores();
            long[] resourceSignals = frame.releaseSemaphores();
            long[] waits = new long[resourceWaits.length + 1];
            int[] stages = new int[waits.length];
            long[] signals = new long[resourceSignals.length + 1];
            waits[0] = imageAvailable[syncSlot];
            stages[0] = VK_PIPELINE_STAGE_TRANSFER_BIT;
            System.arraycopy(resourceWaits, 0, waits, 1, resourceWaits.length);
            Arrays.fill(stages, 1, stages.length, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
            signals[0] = renderFinished[imageIndex];
            System.arraycopy(resourceSignals, 0, signals, 1, resourceSignals.length);

            long fence = device.submitCommandBuffer(commandBuffer, waits, stages, signals);
            frame.markSubmitted(commandBuffer, fence);

            if (!plan.generatedFrames().isEmpty() && submitGeneratedFrames(plan, imageIndex)) {
                return new PresentSubmission(imageIndex, plan, true);
            }
            return new PresentSubmission(imageIndex, plan, false);
        } catch (Throwable throwable) {
            FrameGeneration.disableFrameGeneration();
            throw throwable;
        }
    }

    FrameBatch submitApplicationManagedFrame(
            RealFrameJob job,
            long firstDisplayIndex,
            long batchId,
            long realPeriodNanos,
            String schedulerProviderId,
            FramePacingEstimator framePacingEstimator
    ) {
        if (job == null) {
            throw new IllegalArgumentException("job cannot be null");
        }
        if (schedulerProviderId == null || schedulerProviderId.isBlank()) {
            throw new IllegalArgumentException("schedulerProviderId cannot be blank");
        }
        if (framePacingEstimator == null) {
            throw new IllegalArgumentException("framePacingEstimator cannot be null");
        }
        if (!job.presentAllowed() || !frameCanBePresented(job.frameResources())) {
            submitRealOnlyWithoutPresent(job.frameResources());
            return null;
        }

        ProviderInputSnapshot snapshot = job.providerInputSnapshot();
        int requestedGeneratedCount = snapshot == null
                ? 0
                : Math.min(
                        snapshot.mode().generatedFrameCount(),
                        AsyncFrameGenerationScheduler.MAX_GENERATED_FRAMES
                );
        if (requestedGeneratedCount > 0
                && imageCount < requestedGeneratedCount + 2) {
            requestRecreate();
        }
        if (recreateRequested || swapchain == VK_NULL_HANDLE || width <= 0 || height <= 0) {
            requestRecreate();
            submitRealOnlyWithoutPresent(job.frameResources());
            return null;
        }

        if (snapshot != null && schedulerProviderId.equals(snapshot.providerId())) {
            FrameBatch generatedBatch = trySubmitProviderBatch(
                    job,
                    snapshot,
                    firstDisplayIndex,
                    batchId,
                    realPeriodNanos,
                    schedulerProviderId,
                    framePacingEstimator
            );
            if (generatedBatch != null) {
                return generatedBatch;
            }
        }

        PresentFrame realFrame = submitRealOnly(
                job,
                firstDisplayIndex,
                batchId,
                batchIntervalNanos(realPeriodNanos, 0)
        );
        return realFrame == null
                ? null
                : createRealOnlyBatch(
                        framePacingEstimator,
                        job,
                        batchId,
                        realFrame
                );
    }

    private static FrameBatch createRealOnlyBatch(
            FramePacingEstimator framePacingEstimator,
            RealFrameJob job,
            long batchId,
            PresentFrame realFrame
    ) {
        framePacingEstimator.onBatchResult(0, null);
        return FrameBatch.realOnly(
                        job.realIndex(),
                        batchId,
                        job.latencyFrameId(),
                        realFrame.presentId(),
                        realFrame.batchIntervalNanos(),
                        realFrame,
                        realFrame.sourceCompletion()
        );
    }

    private FrameBatch trySubmitProviderBatch(
            RealFrameJob job,
            ProviderInputSnapshot snapshot,
            long firstDisplayIndex,
            long batchId,
            long realPeriodNanos,
            String schedulerProviderId,
            FramePacingEstimator framePacingEstimator
    ) {
        boolean recreateAfterAbort = false;
        // Recreate pauses the scheduler before taking swapchainLock. Keeping acquire
        // outside that lock lets the present thread release images for this batch.
        {
            if (!frameCanBePresented(job.frameResources())
                    || recreateRequested
                    || swapchain == VK_NULL_HANDLE
                    || width <= 0
                    || height <= 0) {
                return null;
            }

            VulkanCommandBuffer commandBuffer = applicationManagedCommandBuffers().acquire(device);
            VulkanCommandBuffer realCommandBuffer = null;
            long fgToMainReady = VK_NULL_HANDLE;
            ProviderOutputLease outputLease = null;
            List<ScheduledPresentTarget> targets = new ArrayList<>();
            boolean fgSubmitted = false;
            boolean realSubmitted = false;
            try {
                commandBuffer.reset();
                commandBuffer.begin();
                AsyncFrameGenerationDispatchRequest request =
                        new AsyncFrameGenerationDispatchRequest(
                                job.frameResources(),
                                snapshot,
                                device,
                                commandBuffer.getNativeCommandBuffer().address(),
                                width,
                                height,
                                imageFormat,
                                imageCount
                        );
                AsyncFrameGenerationDispatchResult result;
                try {
                    result = FrameGeneration.dispatchAsync(request);
                } catch (Throwable throwable) {
                    SuperResolution.LOGGER.warn(
                            "Frame generation provider '{}' threw during async dispatch for real frame {}; "
                                    + "using Real-only fallback",
                            schedulerProviderId,
                            job.realIndex(),
                            throwable
                    );
                    commandBuffer.reset();
                    return null;
                }

                String invalidReason = validateDispatchResult(result, request);
                if (invalidReason != null) {
                    if (result != null && result.outputLease() != null) {
                        abortProviderLease(result.outputLease());
                    }
                    if (result != null && result.succeeded()) {
                        SuperResolution.LOGGER.warn(
                                "Frame generation provider '{}' returned an invalid async result for "
                                        + "real frame {}: {}; using Real-only fallback",
                                schedulerProviderId,
                                job.realIndex(),
                                invalidReason
                        );
                    } else {
                        SuperResolution.LOGGER.debug(
                                "Frame generation provider '{}' used Real-only fallback for real frame {}: {}",
                                schedulerProviderId,
                                job.realIndex(),
                                invalidReason
                        );
                    }
                    commandBuffer.reset();
                    return null;
                }

                outputLease = result.outputLease();
                int generatedCount = result.actualGeneratedCount();
                if (imageCount < generatedCount + 2) {
                    abortProviderLease(outputLease);
                    outputLease = null;
                    commandBuffer.reset();
                    requestRecreate();
                    return null;
                }

                acquireScheduledPresentTargets(generatedCount + 1, targets);
                for (int index = 0; index < generatedCount; index++) {
                    recordBlit(
                            commandBuffer,
                            result.generatedOutputs().get(index),
                            targets.get(index).imageIndex(),
                            true
                    );
                }
                VulkanTexture realSource = result.realOutput() != null
                        ? result.realOutput()
                        : job.frameResources().finalColorVulkanTexture();
                commandBuffer.end();

                realCommandBuffer = applicationManagedRealCommandBuffers().acquire(device);
                realCommandBuffer.reset();
                realCommandBuffer.begin();
                recordBlit(
                        realCommandBuffer,
                        realSource,
                        targets.get(targets.size() - 1).imageIndex(),
                        false
                );
                realCommandBuffer.end();

                long[] resourceWaits = job.frameResources().readySemaphores();
                long[] waits = new long[generatedCount + resourceWaits.length];
                int[] stages = new int[waits.length];
                for (int index = 0; index < generatedCount; index++) {
                    waits[index] = targets.get(index).acquireLease().semaphore();
                    stages[index] = VK_PIPELINE_STAGE_TRANSFER_BIT;
                }
                System.arraycopy(
                        resourceWaits,
                        0,
                        waits,
                        generatedCount,
                        resourceWaits.length
                );
                Arrays.fill(
                        stages,
                        generatedCount,
                        stages.length,
                        VK_PIPELINE_STAGE_ALL_COMMANDS_BIT
                );

                long[] resourceSignals = job.frameResources().releaseSemaphores();
                boolean mainReadsCaptureInput = result.realOutput() == null;
                long[] fgResourceSignals = mainReadsCaptureInput ? NO_SEMAPHORES : resourceSignals;
                fgToMainReady = createBinarySemaphore("SR FG-to-Main Ready batch=" + batchId);
                long[] signals = new long[generatedCount + fgResourceSignals.length + 1];
                for (int index = 0; index < generatedCount; index++) {
                    signals[index] = renderFinished[targets.get(index).imageIndex()];
                }
                signals[generatedCount] = fgToMainReady;
                System.arraycopy(
                        fgResourceSignals,
                        0,
                        signals,
                        generatedCount + 1,
                        fgResourceSignals.length
                );

                long realPresentId = job.realPresentId();
                VulkanLowLatency.notifyFrameGenerationQueueOutOfBand(device.requireFgQueue());
                VulkanDevice.IssuedSubmission fgSubmission;
                VulkanLowLatency.renderSubmitMarker(realPresentId, true, true);
                try {
                    fgSubmission = device.submitCommandBufferIssued(
                            device.requireFgQueue(),
                            commandBuffer,
                            waits,
                            stages,
                            signals
                    );
                    fgSubmitted = true;
                } catch (VulkanDevice.SubmissionTicketPublicationException throwable) {
                    fgSubmitted = true;
                    throw throwable;
                } finally {
                    VulkanLowLatency.renderSubmitMarker(realPresentId, true, false);
                }

                ScheduledPresentTarget realTarget = targets.get(targets.size() - 1);
                long[] realWaits = new long[]{
                        realTarget.acquireLease().semaphore(),
                        fgToMainReady
                };
                int[] realStages = new int[]{
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT
                };
                long[] realSignals = new long[1 + (mainReadsCaptureInput ? resourceSignals.length : 0)];
                realSignals[0] = renderFinished[realTarget.imageIndex()];
                if (mainReadsCaptureInput) {
                    System.arraycopy(resourceSignals, 0, realSignals, 1, resourceSignals.length);
                }
                VulkanDevice.IssuedSubmission realSubmission;
                VulkanLowLatency.renderSubmitMarker(realPresentId, false, true);
                try {
                    realSubmission = device.submitCommandBufferIssued(
                            device.getMainQueue(),
                            realCommandBuffer,
                            realWaits,
                            realStages,
                            realSignals,
                            realPresentId
                    );
                    realSubmitted = true;
                } catch (VulkanDevice.SubmissionTicketPublicationException throwable) {
                    realSubmitted = true;
                    throw throwable;
                } finally {
                    VulkanLowLatency.renderSubmitMarker(realPresentId, false, false);
                }
                job.frameResources().markSubmitted(commandBuffer, fgSubmission.fence());

                FrameGenerationDispatchCompletion completion =
                        new SubmittedProviderBatchCompletion(
                                result.completion(),
                                commandBuffer,
                                commandBuffer.submissionGeneration(),
                                realCommandBuffer,
                                realCommandBuffer.submissionGeneration(),
                                device,
                                fgToMainReady
                        );
                VulkanLowLatency.PresentBatchIds presentIds =
                        VulkanLowLatency.reservePresentBatch(realPresentId, generatedCount);
                long[] generatedPresentIds = presentIds.generatedPresentIds();
                realPresentId = presentIds.realPresentId();
                boolean pacingEnabled = framePacingEstimator.onBatchResult(
                        generatedCount,
                        result.historyDisposition()
                );
                long batchIntervalNanos =
                        batchIntervalNanos(realPeriodNanos, generatedCount);
                List<PresentFrame> presentFrames =
                        new ArrayList<>(generatedCount + 1);
                for (int index = 0; index < targets.size(); index++) {
                    boolean generated = index < generatedCount;
                    ScheduledPresentTarget target = targets.get(index);
                    presentFrames.add(new PresentFrame(
                            firstDisplayIndex + index,
                            job.realIndex(),
                            job.latencyFrameId(),
                            generated
                                    ? PresentFrame.Kind.GENERATED
                                    : PresentFrame.Kind.REAL,
                            swapchainGeneration,
                            swapchain,
                            target.imageIndex(),
                            renderFinished[target.imageIndex()],
                            generated
                                    ? fgSubmission.submissionTicket()
                                    : realSubmission.submissionTicket(),
                            generated ? generatedPresentIds[index] : realPresentId,
                            generated,
                            batchId,
                            batchIntervalNanos,
                            generatedCount,
                            pacingEnabled,
                            outputLease,
                            completion,
                            target.acquireLease()::close
                    ));
                }
                FrameBatch batch = FrameBatch.applicationManaged(
                        job.realIndex(),
                        generatedCount,
                        batchId,
                        job.latencyFrameId(),
                        realPresentId,
                        batchIntervalNanos,
                        pacingEnabled,
                        presentFrames,
                        outputLease,
                        completion,
                        result.historyDisposition()
                );
                fgToMainReady = VK_NULL_HANDLE;
                return batch;
            } catch (Throwable throwable) {
                if (fgSubmitted) {
                    job.frameResources().markUnrecoverable();
                    waitAndReleaseAbortedSubmittedBatch(
                            commandBuffer,
                            realCommandBuffer,
                            realSubmitted,
                            targets,
                            outputLease,
                            fgToMainReady
                    );
                    fgToMainReady = VK_NULL_HANDLE;
                    throw throwable;
                }

                if (fgToMainReady != VK_NULL_HANDLE) {
                    vkDestroySemaphore(device.getVkDevice(), fgToMainReady, null);
                    fgToMainReady = VK_NULL_HANDLE;
                }
                try {
                    commandBuffer.reset();
                    if (realCommandBuffer != null) {
                        realCommandBuffer.reset();
                    }
                } catch (Throwable resetFailure) {
                    throwable.addSuppressed(resetFailure);
                }
                if (!targets.isEmpty()) {
                    returnAbortedScheduledTargets(targets, 0);
                }
                ScheduledTargetAcquireException targetAcquireException =
                        throwable instanceof ScheduledTargetAcquireException exception
                                ? exception
                                : null;
                recreateAfterAbort = targetAcquireException != null
                        && targetAcquireException.requiresRecreate();
                abortProviderLease(outputLease);
                if (targetAcquireException != null
                        && targetAcquireException.isExpectedFallback()) {
                    SuperResolution.LOGGER.debug(
                            "Deferred async frame-generation batch for provider '{}' and real frame {}: {}; "
                                    + "using Real-only fallback",
                            schedulerProviderId,
                            job.realIndex(),
                            targetAcquireException.getMessage()
                    );
                } else {
                    SuperResolution.LOGGER.warn(
                            "Failed to build a complete async frame-generation batch for provider '{}' "
                                    + "and real frame {}; using Real-only fallback",
                            schedulerProviderId,
                            job.realIndex(),
                            throwable
                    );
                }
            }
        }

        if (recreateAfterAbort) {
            requestRecreate();
        }
        return null;
    }

    private PresentFrame submitRealOnly(
            RealFrameJob job,
            long displayIndex,
            long batchId,
            long batchIntervalNanos
    ) {
        if (!job.presentAllowed() || !frameCanBePresented(job.frameResources())) {
            submitRealOnlyWithoutPresent(job.frameResources());
            return null;
        }
        if (recreateRequested || swapchain == VK_NULL_HANDLE || width <= 0 || height <= 0) {
            requestRecreate();
            submitRealOnlyWithoutPresent(job.frameResources());
            return null;
        }
        {
            if (!frameCanBePresented(job.frameResources())
                    || recreateRequested
                    || swapchain == VK_NULL_HANDLE
                    || width <= 0
                    || height <= 0) {
                submitRealOnlyWithoutPresent(job.frameResources());
                return null;
            }

            VulkanBinarySemaphorePool.Lease acquireLease = null;
            boolean submitted = false;
            boolean targetReserved = false;
            boolean targetOwnershipTransferred = false;
            try {
                ensureApplicationManagedBatchFits(1);
                reserveApplicationManagedTarget();
                targetReserved = true;
                acquireLease = acquireApplicationManagedSemaphore();
                int imageIndex = acquireImage(acquireLease.semaphore());
                if (imageIndex < 0) {
                    acquireLease.close();
                    acquireLease = null;
                    if (imageIndex == ACQUIRE_OUT_OF_DATE) {
                        requestRecreate();
                    }
                    submitRealOnlyWithoutPresent(job.frameResources());
                    return null;
                }

                VulkanCommandBuffer commandBuffer =
                        applicationManagedRealCommandBuffers().acquire(device);
                commandBuffer.reset();
                commandBuffer.begin();
                recordBlit(
                        commandBuffer,
                        job.frameResources().finalColorVulkanTexture(),
                        imageIndex,
                        false
                );
                commandBuffer.end();

                long[] resourceWaits = job.frameResources().readySemaphores();
                long[] resourceSignals = job.frameResources().releaseSemaphores();
                long[] waits = new long[resourceWaits.length + 1];
                int[] stages = new int[waits.length];
                long[] signals = new long[resourceSignals.length + 1];
                waits[0] = acquireLease.semaphore();
                stages[0] = VK_PIPELINE_STAGE_TRANSFER_BIT;
                System.arraycopy(resourceWaits, 0, waits, 1, resourceWaits.length);
                Arrays.fill(stages, 1, stages.length, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
                signals[0] = renderFinished[imageIndex];
                System.arraycopy(resourceSignals, 0, signals, 1, resourceSignals.length);

                long realPresentId = VulkanLowLatency
                        .reservePresentBatch(job.realPresentId(), 0)
                        .realPresentId();
                VulkanDevice.IssuedSubmission submission;
                VulkanLowLatency.renderSubmitMarker(realPresentId, false, true);
                try {
                    submission = device.submitCommandBufferIssued(
                            device.getMainQueue(),
                            commandBuffer,
                            waits,
                            stages,
                            signals,
                            realPresentId
                    );
                } catch (VulkanDevice.SubmissionTicketPublicationException throwable) {
                    job.frameResources().markSubmitted(commandBuffer, throwable.fence());
                    commandBuffer.waitForSubmission(throwable.submissionGeneration());
                    acquireLease.close();
                    acquireLease = null;
                    submitted = true;
                    throw throwable;
                } finally {
                    VulkanLowLatency.renderSubmitMarker(realPresentId, false, false);
                }
                job.frameResources().markSubmitted(commandBuffer, submission.fence());
                submitted = true;
                FrameGenerationDispatchCompletion completion =
                        new SubmittedCommandBufferCompletion(
                                commandBuffer,
                                commandBuffer.submissionGeneration()
                        );
                PresentFrame presentFrame = new PresentFrame(
                        displayIndex,
                        job.realIndex(),
                        job.latencyFrameId(),
                        PresentFrame.Kind.REAL,
                        swapchainGeneration,
                        swapchain,
                        imageIndex,
                        renderFinished[imageIndex],
                        submission.submissionTicket(),
                        realPresentId,
                        false,
                        batchId,
                        batchIntervalNanos,
                        0,
                        false,
                        null,
                        completion,
                        acquireLease::close
                );
                targetOwnershipTransferred = true;
                return presentFrame;
            } catch (ScheduledTargetAcquireException exception) {
                requestRecreate();
                submitRealOnlyWithoutPresent(job.frameResources());
                return null;
            } finally {
                if (!submitted && acquireLease != null) {
                    acquireLease.close();
                }
                if (!targetOwnershipTransferred && targetReserved) {
                    releaseApplicationManagedTarget();
                }
            }
        }
    }

    int presentScheduledFrame(PresentFrame frame) {
        synchronized (swapchainLock) {
            if (!isCurrentGeneration(frame)) {
                return VK_ERROR_OUT_OF_DATE_KHR;
            }
            return presentImage(
                    frame.swapchainImageIndex(),
                    frame.outOfBand(),
                    frame.swapchainHandle(),
                    frame.presentReadyBinary(),
                    frame.presentId(),
                    true
            );
        }
    }

    void discardScheduledFrame(PresentFrame frame) {
        if (frame != null) {
            frame.releaseAcquireLease();
        }
    }

    void releaseScheduledTarget(PresentFrame frame) {
        if (frame != null) {
            releaseApplicationManagedTarget();
        }
    }

    boolean isCurrentGeneration(PresentFrame frame) {
        return frame != null
                && frame.swapchainGeneration() == swapchainGeneration
                && frame.swapchainHandle() == swapchain;
    }

    /**
     * Acquires and blits one swapchain image per interpolated frame, then hands the
     * whole batch (interpolated frames first, real frame last) to the pacer thread.
     * Returns false when the batch could not be set up; the caller then presents the
     * real frame inline.
     */
    private boolean submitGeneratedFrames(FramePresentPlan plan, int realImageIndex) {
        List<VulkanTexture> generated = plan.generatedFrames();
        if (imageCount < generated.size() + 2) {
            requestRecreate();
            return false;
        }
        List<Integer> presentOrder = new ArrayList<>(generated.size() + 1);
        for (VulkanTexture generatedTexture : generated) {
            int syncSlot = nextImageAvailableIndex();
            int generatedImageIndex = acquireImage(syncSlot);
            if (generatedImageIndex < 0) {
                requestRecreate();
                presentAcquiredUnpaced(presentOrder);
                return false;
            }
            VulkanCommandBuffer blitCommandBuffer = generatedCommandBuffers().acquire(device);
            blitCommandBuffer.reset();
            blitCommandBuffer.begin();
            recordBlit(blitCommandBuffer, generatedTexture, generatedImageIndex, true);
            blitCommandBuffer.end();
            device.submitCommandBuffer(
                    blitCommandBuffer,
                    new long[]{imageAvailable[syncSlot]},
                    new int[]{VK_PIPELINE_STAGE_TRANSFER_BIT},
                    new long[]{renderFinished[generatedImageIndex]}
            );
            presentOrder.add(generatedImageIndex);
        }
        presentOrder.add(realImageIndex);
        // Present ids for the whole batch are fixed now so the next frame starting
        // on the render thread cannot shift them under the pacer thread.
        VulkanLowLatency.expectGeneratedBatch(generated.size());
        pacer.submitBatch(presentOrder, index -> presentImage(index, index != realImageIndex));
        return true;
    }

    private void submitRealOnlyWithoutPresent(FrameResources frame) {
        try {
            VulkanCommandBuffer commandBuffer = applicationManagedRealCommandBuffers().acquire(device);
            commandBuffer.reset();
            commandBuffer.begin();
            commandBuffer.end();

            long[] waits = frame.readySemaphores();
            int[] stages = waits.length == 0 ? NO_STAGES : new int[waits.length];
            Arrays.fill(stages, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
            long[] signals = frame.releaseSemaphores();
            long fence = device.submitCommandBuffer(
                    device.getMainQueue(),
                    commandBuffer,
                    waits.length == 0 ? NO_SEMAPHORES : waits,
                    stages,
                    signals.length == 0 ? NO_SEMAPHORES : signals
            );
            frame.markSubmitted(commandBuffer, fence);
        } catch (Throwable throwable) {
            frame.markUnrecoverable();
            throw throwable;
        }
    }

    private void presentAcquiredUnpaced(List<Integer> imageIndices) {
        for (int imageIndex : imageIndices) {
            queuePresent(imageIndex);
        }
    }

    private VulkanCommandBufferRing generatedCommandBuffers() {
        if (generatedBlitCommandBuffers == null) {
            generatedBlitCommandBuffers =
                    new VulkanCommandBufferRing(MAX_IN_FLIGHT_FRAMES * MAX_GENERATED_FRAMES);
        }
        return generatedBlitCommandBuffers;
    }

    public void consumeWithoutPresent(FrameResources frame) {
        AsyncFrameGenerationScheduler scheduler = ensureApplicationManagedScheduler();
        if (scheduler != null) {
            scheduler.enqueue(frame, false);
            return;
        }
        try {
            consumeWithoutPresentInternal(frame);
        } finally {
        }
    }

    private void consumeWithoutPresentInternal(FrameResources frame) {
        FrameGeneration.disableFrameGeneration();
        try {
            VulkanCommandBuffer commandBuffer = commandBuffers.acquire(device);
            commandBuffer.reset();
            commandBuffer.begin();
            commandBuffer.end();

            long[] waits = frame.readySemaphores();
            int[] stages = waits.length == 0 ? NO_STAGES : new int[waits.length];
            Arrays.fill(stages, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
            long[] signals = frame.releaseSemaphores();
            long fence = device.submitCommandBuffer(
                    commandBuffer,
                    waits.length == 0 ? NO_SEMAPHORES : waits,
                    stages,
                    signals.length == 0 ? NO_SEMAPHORES : signals
            );
            frame.markSubmitted(commandBuffer, fence);
        } catch (Throwable throwable) {
            frame.markUnrecoverable();
            throw throwable;
        }
    }

    boolean shutdownApplicationManagedProvider(
            String providerId,
            Runnable teardown
    ) {
        AsyncFrameGenerationScheduler scheduler = applicationManagedScheduler;
        if (scheduler == null || !scheduler.providerId().equals(providerId)) {
            return false;
        }
        scheduler.shutdownProviderOnFgThread(providerId, teardown);
        return true;
    }

    public void destroy() {
        Throwable failure = null;
        AsyncFrameGenerationScheduler scheduler = applicationManagedScheduler;
        applicationManagedScheduler = null;
        try {
            if (scheduler != null) {
                scheduler.close();
            }
        } catch (Throwable throwable) {
            failure = throwable;
        } finally {
            pacer.shutdown();
            device.getMainQueue().waitIdle();
            if (device.getFrameGenerationQueue() != null) {
                device.getFrameGenerationQueue().waitIdle();
            }
            waitForDedicatedPresentQueueIdle();
            commandBuffers.destroy();
            if (applicationManagedCommandBuffers != null) {
                applicationManagedCommandBuffers.destroy();
                applicationManagedCommandBuffers = null;
            }
            if (applicationManagedRealCommandBuffers != null) {
                applicationManagedRealCommandBuffers.destroy();
                applicationManagedRealCommandBuffers = null;
            }
            if (generatedBlitCommandBuffers != null) {
                generatedBlitCommandBuffers.destroy();
                generatedBlitCommandBuffers = null;
            }
            if (applicationManagedAcquireSemaphores != null) {
                applicationManagedAcquireSemaphores.close();
                applicationManagedAcquireSemaphores = null;
            }
            destroyIndicatorTextures();
            destroySwapchain();
            for (int i = 0; i < imageAvailable.length; i++) {
                if (imageAvailable[i] != VK_NULL_HANDLE) {
                    vkDestroySemaphore(device.getVkDevice(), imageAvailable[i], null);
                    imageAvailable[i] = VK_NULL_HANDLE;
                }
            }
            destroySemaphores(renderFinished);
            renderFinished = new long[0];
        }
        if (failure != null) {
            throw new IllegalStateException("Application-managed scheduler shutdown failed", failure);
        }
    }

    private boolean queuePresent(int imageIndex) {
        int result = presentImage(imageIndex, false);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            recreateRequested = true;
            return false;
        }
        if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new IllegalStateException("Failed to present Vulkan swapchain image, VkResult=" + result);
        }
        if (result == VK_SUBOPTIMAL_KHR) {
            recreateRequested = true;
        }
        return true;
    }

    /**
     * Presents one swapchain image and returns the raw VkResult. Safe to call from
     * the pacer thread; the queue lock serializes it against command submissions.
     * Interpolated frames present as out-of-band so Reflex pacing only tracks the
     * real frame.
     */
    private int presentImage(int imageIndex, boolean outOfBandPresent) {
        return presentImage(
                imageIndex,
                outOfBandPresent,
                swapchain,
                renderFinished[imageIndex],
                0L,
                false
        );
    }

    private int presentImage(
            int imageIndex,
            boolean outOfBandPresent,
            long targetSwapchain,
            long presentReadyBinary,
            long immutablePresentId,
            boolean applicationManaged
    ) {
        return VulkanLowLatency.synchronizedSwapchainOperation(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                        .pWaitSemaphores(stack.longs(presentReadyBinary))
                        .swapchainCount(1)
                        .pSwapchains(stack.longs(targetSwapchain))
                        .pImageIndices(stack.ints(imageIndex));
                long presentId = applicationManaged
                        ? immutablePresentId
                        : VulkanLowLatency.beginPresent(outOfBandPresent);
                if (presentId != 0L) {
                    VkPresentIdKHR presentIdInfo = VkPresentIdKHR.calloc(stack)
                            .sType(KHRPresentId.VK_STRUCTURE_TYPE_PRESENT_ID_KHR)
                            .swapchainCount(1)
                            .pPresentIds(stack.longs(presentId));
                    presentInfo.pNext(presentIdInfo.address());
                }
                if (applicationManaged) {
                    VulkanLowLatency.presentMarker(presentId, outOfBandPresent, true);
                } else {
                    LowLatency.beginPresent();
                }
                try {
                    VulkanQueue presentQueue = applicationManaged
                            ? device.getApplicationManagedPresentQueue()
                            : device.getMainQueue();
                    synchronized (presentQueue.submitLock()) {
                        return vkQueuePresentKHR(presentQueue.getQueue(), presentInfo);
                    }
                } finally {
                    if (applicationManaged) {
                        VulkanLowLatency.presentMarker(presentId, outOfBandPresent, false);
                    } else {
                        LowLatency.endPresent();
                        VulkanLowLatency.endPresent();
                    }
                }
            }
        });
    }

    private int acquireImage(int syncSlot) {
        return acquireImage(imageAvailable[syncSlot]);
    }

    private int acquireImage(long acquireSemaphore) {
        return VulkanLowLatency.synchronizedSwapchainOperation(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer imageIndex = stack.mallocInt(1);
                int result = vkAcquireNextImageKHR(
                        device.getVkDevice(),
                        swapchain,
                        ACQUIRE_TIMEOUT_NANOS,
                        acquireSemaphore,
                        VK_NULL_HANDLE,
                        imageIndex
                );
                if (result == VK_TIMEOUT) {
                    return ACQUIRE_TIMEOUT;
                }
                if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                    recreateRequested = true;
                    return ACQUIRE_OUT_OF_DATE;
                }
                if (result == VK_SUBOPTIMAL_KHR) {
                    recreateRequested = true;
                } else if (result != VK_SUCCESS) {
                    throw new IllegalStateException(
                            "Failed to acquire Vulkan swapchain image, VkResult=" + result
                    );
                }
                return imageIndex.get(0);
            }
        });
    }

    private boolean frameCanBePresented(FrameResources frame) {
        return frame != null
                && frame.hasFinalColor()
                && swapchain != VK_NULL_HANDLE
                && width > 0
                && height > 0;
    }

    private synchronized AsyncFrameGenerationScheduler ensureApplicationManagedScheduler() {
        if (applicationManagedScheduler != null) {
            if (!FrameGeneration.isApplicationManagedSchedulerCompatible(
                    applicationManagedScheduler.providerId()
            )) {
                throw new IllegalStateException(
                        "Frame generation provider changed before the application-managed "
                                + "scheduler was drained"
                );
            }
            return applicationManagedScheduler;
        }
        String providerId = FrameGeneration.activeApplicationManagedProviderId();
        if (providerId.isEmpty()) {
            return null;
        }
        if (!device.asyncDispatchCapabilities().available()) {
            return null;
        }
        applicationManagedScheduler =
                new AsyncFrameGenerationScheduler(this, device, providerId);
        return applicationManagedScheduler;
    }

    private void updateApplicationManagedFramePlan() {
        int requestedGeneratedFrames = Math.min(
                Math.max(0, FrameGeneration.plannedGeneratedFrameCount()),
                MAX_GENERATED_FRAMES
        );
        if (plannedGeneratedFrames != requestedGeneratedFrames) {
            plannedGeneratedFrames = requestedGeneratedFrames;
            requestRecreate();
        }
    }

    FramePacingTiming framePacingTiming() {
        return context.framePacingTiming();
    }

    long swapchainGeneration() {
        return swapchainGeneration;
    }

    private VulkanCommandBufferRing applicationManagedCommandBuffers() {
        if (applicationManagedCommandBuffers == null) {
            applicationManagedCommandBuffers = new VulkanCommandBufferRing(
                    MAX_IN_FLIGHT_FRAMES,
                    device.requireFgCommandPool()
            );
        }
        return applicationManagedCommandBuffers;
    }

    private VulkanCommandBufferRing applicationManagedRealCommandBuffers() {
        if (applicationManagedRealCommandBuffers == null) {
            applicationManagedRealCommandBuffers = new VulkanCommandBufferRing(
                    MAX_IN_FLIGHT_FRAMES,
                    device.requireFgCommandPool()
            );
        }
        return applicationManagedRealCommandBuffers;
    }

    private VulkanBinarySemaphorePool applicationManagedAcquireSemaphores() {
        if (applicationManagedAcquireSemaphores == null) {
            applicationManagedAcquireSemaphores = device.createBinarySemaphorePool(
                    "SR ApplicationManaged Acquire",
                    ACQUIRE_SYNC_SLOTS
            );
        }
        return applicationManagedAcquireSemaphores;
    }

    private VulkanBinarySemaphorePool.Lease acquireApplicationManagedSemaphore() {
        try {
            return applicationManagedAcquireSemaphores().acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while acquiring an application-managed semaphore",
                    e
            );
        }
    }

    private void ensureApplicationManagedBatchFits(int targetCount) {
        synchronized (applicationManagedTargetLock) {
            int capacity = applicationManagedTargetCapacity();
            if (recreateRequested || targetCount > capacity) {
                throw new ScheduledTargetAcquireException(
                        "Application-managed swapchain batch requires " + targetCount
                                + " targets but only " + capacity + " are available",
                        true
                );
            }
        }
    }

    private void reserveApplicationManagedTarget() {
        synchronized (applicationManagedTargetLock) {
            while (true) {
                if (recreateRequested) {
                    throw new ScheduledTargetAcquireException(
                            "Swapchain recreation was requested while reserving an async frame target",
                            true
                    );
                }
                if (applicationManagedTargetCount < applicationManagedTargetCapacity()) {
                    applicationManagedTargetCount++;
                    return;
                }
                try {
                    applicationManagedTargetLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for an application-managed swapchain target",
                            e
                    );
                }
            }
        }
    }

    private void releaseApplicationManagedTarget() {
        synchronized (applicationManagedTargetLock) {
            if (applicationManagedTargetCount <= 0) {
                throw new IllegalStateException("No application-managed swapchain target is reserved");
            }
            applicationManagedTargetCount--;
            applicationManagedTargetLock.notifyAll();
        }
    }

    private int applicationManagedTargetCapacity() {
        if (swapchain == VK_NULL_HANDLE || width <= 0 || height <= 0) {
            return 0;
        }
        // Keep one image unreserved so the foreground render path can always make progress.
        return Math.max(0, imageCount - 1);
    }

    private void acquireScheduledPresentTargets(
            int count,
            List<ScheduledPresentTarget> targets
    ) {
        ensureApplicationManagedBatchFits(count);
        for (int index = 0; index < count; index++) {
            reserveApplicationManagedTarget();
            VulkanBinarySemaphorePool.Lease acquireLease = null;
            boolean targetAcquired = false;
            try {
                acquireLease = acquireApplicationManagedSemaphore();
                int imageIndex = acquireImage(acquireLease.semaphore());
                if (imageIndex < 0) {
                    acquireLease.close();
                    acquireLease = null;
                    throw new ScheduledTargetAcquireException(
                            imageIndex == ACQUIRE_OUT_OF_DATE
                                    ? VK_ERROR_OUT_OF_DATE_KHR
                                    : VK_TIMEOUT
                    );
                }
                targets.add(new ScheduledPresentTarget(
                        imageIndex,
                        swapchain,
                        images[imageIndex],
                        imageLayouts[imageIndex],
                        renderFinished[imageIndex],
                        acquireLease
                ));
                targetAcquired = true;
            } finally {
                if (!targetAcquired) {
                    if (acquireLease != null) {
                        acquireLease.close();
                    }
                    releaseApplicationManagedTarget();
                }
            }
        }
    }

    /**
     * Returns images acquired for a batch that will never reach the normal present queue.
     * The FG queue first consumes every acquire semaphore and signals the image-specific
     * present semaphore; the exceptional direct presents then return the images to the
     * swapchain without claiming a latency present id.
     */
    private void returnAbortedScheduledTargets(
            List<ScheduledPresentTarget> targets,
            int renderedTargetCount
    ) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        if (renderedTargetCount < 0 || renderedTargetCount > targets.size()) {
            throw new IllegalArgumentException("renderedTargetCount is outside the acquired target range");
        }

        if (renderedTargetCount < targets.size()) {
            VulkanCommandBuffer commandBuffer =
                    applicationManagedCommandBuffers().acquire(device);
            commandBuffer.reset();
            commandBuffer.begin();
            recordAbortedTargetPresentLayouts(commandBuffer, targets, renderedTargetCount);
            commandBuffer.end();

            int pendingCount = targets.size() - renderedTargetCount;
            long[] waits = new long[pendingCount];
            int[] stages = new int[pendingCount];
            long[] signals = new long[pendingCount];
            for (int index = 0; index < pendingCount; index++) {
                ScheduledPresentTarget target = targets.get(renderedTargetCount + index);
                waits[index] = target.acquireLease().semaphore();
                stages[index] = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                signals[index] = target.renderFinishedSemaphore();
            }
            device.submitCommandBuffer(
                    device.requireFgQueue(),
                    commandBuffer,
                    waits,
                    stages,
                    signals
            );
            commandBuffer.waitForFence();
            markAbortedTargetsPresented(targets, renderedTargetCount);
        }

        Throwable failure = null;
        boolean recreate = false;
        for (ScheduledPresentTarget target : targets) {
            try {
                int result = presentImage(
                        target.imageIndex(),
                        true,
                        target.swapchainHandle(),
                        target.renderFinishedSemaphore(),
                        0L,
                        true
                );
                if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                    recreate = true;
                } else if (result != VK_SUCCESS) {
                    throw new IllegalStateException(
                            "Failed to return an aborted Vulkan swapchain image, VkResult=" + result
                    );
                }
            } catch (Throwable throwable) {
                recreate = true;
                if (failure == null) {
                    failure = throwable;
                } else {
                    failure.addSuppressed(throwable);
                }
            } finally {
                try {
                    target.acquireLease().close();
                } finally {
                    releaseApplicationManagedTarget();
                }
            }
        }
        if (recreate) {
            requestRecreate();
        }
        if (failure != null) {
            SuperResolution.LOGGER.warn(
                    "Failed while returning aborted application-managed swapchain images",
                    failure
            );
        }
    }

    private void recordAbortedTargetPresentLayouts(
            VulkanCommandBuffer commandBuffer,
            List<ScheduledPresentTarget> targets,
            int renderedTargetCount
    ) {
        int transitionCount = 0;
        int sourceStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        for (int index = renderedTargetCount; index < targets.size(); index++) {
            int layoutAtAcquire = targets.get(index).layoutAtAcquire();
            if (layoutAtAcquire != VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
                transitionCount++;
                if (layoutAtAcquire != VK_IMAGE_LAYOUT_UNDEFINED) {
                    sourceStageMask = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
                }
            }
        }
        if (transitionCount == 0) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barriers =
                    VkImageMemoryBarrier.calloc(transitionCount, stack);
            int barrierIndex = 0;
            for (int index = renderedTargetCount; index < targets.size(); index++) {
                ScheduledPresentTarget target = targets.get(index);
                int layoutAtAcquire = target.layoutAtAcquire();
                if (layoutAtAcquire == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
                    continue;
                }
                barriers.get(barrierIndex++)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .srcAccessMask(sourceAccessMask(layoutAtAcquire))
                        .dstAccessMask(0)
                        .oldLayout(layoutAtAcquire)
                        .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(target.imageHandle())
                        .subresourceRange(colorSubresource(stack));
            }
            vkCmdPipelineBarrier(
                    commandBuffer.getNativeCommandBuffer(),
                    sourceStageMask,
                    VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    barriers
            );
        }
    }

    private void markAbortedTargetsPresented(
            List<ScheduledPresentTarget> targets,
            int renderedTargetCount
    ) {
        for (int index = renderedTargetCount; index < targets.size(); index++) {
            ScheduledPresentTarget target = targets.get(index);
            int imageIndex = target.imageIndex();
            if (target.swapchainHandle() == swapchain
                    && imageIndex < imageLayouts.length
                    && images[imageIndex] == target.imageHandle()) {
                imageLayouts[imageIndex] = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
            }
        }
    }

    private void waitAndReleaseAbortedSubmittedBatch(
            VulkanCommandBuffer fgCommandBuffer,
            VulkanCommandBuffer realCommandBuffer,
            boolean realSubmitted,
            List<ScheduledPresentTarget> targets,
            ProviderOutputLease outputLease,
            long fgToMainReady
    ) {
        fgCommandBuffer.waitForFence();
        if (realSubmitted && realCommandBuffer != null) {
            realCommandBuffer.waitForFence();
        }
        int renderedTargetCount = realSubmitted ? targets.size() : Math.max(0, targets.size() - 1);
        returnAbortedScheduledTargets(targets, renderedTargetCount);
        if (outputLease != null) {
            outputLease.completion().awaitCompletion();
            if (!outputLease.isReleased()) {
                outputLease.release();
            }
        }
        if (fgToMainReady != VK_NULL_HANDLE) {
            vkDestroySemaphore(device.getVkDevice(), fgToMainReady, null);
        }
    }

    private void abortProviderLease(ProviderOutputLease outputLease) {
        if (outputLease != null && !outputLease.isReleased()) {
            outputLease.abort();
        }
    }

    private String validateDispatchResult(
            AsyncFrameGenerationDispatchResult result,
            AsyncFrameGenerationDispatchRequest request
    ) {
        if (result == null) {
            return "Provider returned null";
        }
        if (!result.succeeded()) {
            return result.failureReason() == null
                    ? "Provider dispatch failed without a reason"
                    : result.failureReason();
        }
        ProviderOutputLease lease = result.outputLease();
        if (lease == null) {
            return "Successful dispatch did not return an output lease";
        }
        if (lease.isReleased()) {
            return "Successful dispatch returned an already released output lease";
        }
        if (result.actualGeneratedCount() > request.requestedGeneratedFrameCount()) {
            return "Provider returned more generated frames than requested";
        }
        if (result.actualGeneratedCount()
                > AsyncFrameGenerationScheduler.MAX_GENERATED_FRAMES) {
            return "Provider exceeded the scheduler generated-frame limit";
        }
        if (result.generatedOutputs().size() != result.actualGeneratedCount()) {
            return "Generated output count does not match actualGeneratedCount";
        }

        ProviderOutputLease.OutputKey outputKey = lease.outputKey();
        if (outputKey.width() != request.outputWidth()
                || outputKey.height() != request.outputHeight()) {
            return "Provider output lease dimensions do not match the swapchain extent";
        }
        for (VulkanTexture output : result.generatedOutputs()) {
            String reason = validateProviderOutputTexture(output, outputKey);
            if (reason != null) {
                return reason;
            }
        }
        if (result.realOutput() != null) {
            String reason = validateProviderOutputTexture(
                    result.realOutput(),
                    outputKey
            );
            if (reason != null) {
                return "Provider real output is invalid: " + reason;
            }
        }
        return null;
    }

    private String validateProviderOutputTexture(
            VulkanTexture texture,
            ProviderOutputLease.OutputKey outputKey
    ) {
        if (texture == null) {
            return "Provider output texture is null";
        }
        if (texture.getWidth() != outputKey.width()
                || texture.getHeight() != outputKey.height()) {
            return "Provider output texture dimensions do not match its lease key";
        }
        if (texture.getTextureFormat().vk() != outputKey.format()) {
            return "Provider output texture format does not match its lease key";
        }
        return null;
    }

    private static long batchIntervalNanos(long realPeriodNanos, int generatedCount) {
        long interval = realPeriodNanos / Math.max(1L, generatedCount + 1L);
        return Math.max(
                MIN_APPLICATION_MANAGED_PRESENT_INTERVAL_NANOS,
                Math.min(MAX_APPLICATION_MANAGED_PRESENT_INTERVAL_NANOS, interval)
        );
    }

    private void recordBlit(
            VulkanCommandBuffer commandBuffer,
            VulkanTexture source,
            int imageIndex,
            boolean generatedFrame
    ) {
        VkCommandBuffer nativeCommandBuffer = commandBuffer.getNativeCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int oldSourceLayout = source.getCurrentLayout();
            int oldSwapchainLayout = imageLayouts[imageIndex];
            VkImageMemoryBarrier.Buffer barriers = VkImageMemoryBarrier.calloc(2, stack);
            barriers.get(0)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(sourceAccessMask(oldSourceLayout))
                    .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                    .oldLayout(oldSourceLayout)
                    .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(source.handle())
                    .subresourceRange(colorSubresource(stack));
            barriers.get(1)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(oldSwapchainLayout == VK_IMAGE_LAYOUT_UNDEFINED ? 0 : VK_ACCESS_MEMORY_READ_BIT)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .oldLayout(oldSwapchainLayout)
                    .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(images[imageIndex])
                    .subresourceRange(colorSubresource(stack));
            vkCmdPipelineBarrier(
                    nativeCommandBuffer,
                    sourceStageMask(oldSourceLayout, oldSwapchainLayout),
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    null,
                    null,
                    barriers
            );

            VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
            blit.srcSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            blit.srcOffsets(0).set(0, 0, 0);
            blit.srcOffsets(1).set(source.getWidth(), source.getHeight(), 1);
            blit.dstSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            blit.dstOffsets(0).set(0, 0, 0);
            blit.dstOffsets(1).set(width, height, 1);
            vkCmdBlitImage(
                    nativeCommandBuffer,
                    source.handle(),
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    images[imageIndex],
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit,
                    VK_FILTER_NEAREST
            );

            if (SuperResolutionConfig.isEnablePresentIndicator()) {
                recordPresentIndicator(nativeCommandBuffer, stack, imageIndex, generatedFrame);
            }

            VkImageMemoryBarrier.Buffer presentBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(0)
                    .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(images[imageIndex])
                    .subresourceRange(colorSubresource(stack));
            vkCmdPipelineBarrier(
                    nativeCommandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    presentBarrier
            );
        }
        source.setCurrentLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        imageLayouts[imageIndex] = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    }

    /**
     * Blits a small solid square into the top-right corner of the swapchain image
     * (white = real frame, cyan = interpolated) so the frame generation cadence is
     * visible on every present — the raw-NVNGX equivalent of the overlay Streamline
     * stamps when it owns the swapchain. The swapchain image is still in
     * TRANSFER_DST here, so this costs one tiny blit and no extra barriers.
     */
    private void recordPresentIndicator(
            VkCommandBuffer nativeCommandBuffer,
            MemoryStack stack,
            int imageIndex,
            boolean generatedFrame
    ) {
        if (!ensureIndicatorTextures()) {
            return;
        }
        int size = Math.max(8, Math.min(width, height) / 64);
        int margin = Math.max(4, size / 2);
        if (width < size + margin * 2 || height < size + margin * 2) {
            return;
        }
        int x = width - margin - size;
        int y = margin;
        VulkanTexture indicator = generatedFrame ? generatedFrameIndicator : realFrameIndicator;
        VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
        blit.srcSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1);
        blit.srcOffsets(0).set(0, 0, 0);
        blit.srcOffsets(1).set(1, 1, 1);
        blit.dstSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1);
        blit.dstOffsets(0).set(x, y, 0);
        blit.dstOffsets(1).set(x + size, y + size, 1);
        vkCmdBlitImage(
                nativeCommandBuffer,
                indicator.handle(),
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                images[imageIndex],
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                blit,
                VK_FILTER_NEAREST
        );
    }

    private boolean ensureIndicatorTextures() {
        if (realFrameIndicator != null && generatedFrameIndicator != null) {
            return true;
        }
        if (indicatorCreationFailed) {
            return false;
        }
        try {
            realFrameIndicator = createIndicatorTexture("SRPresentIndicatorReal");
            generatedFrameIndicator = createIndicatorTexture("SRPresentIndicatorGenerated");
            fillIndicatorTextures();
            return true;
        } catch (RuntimeException | Error e) {
            indicatorCreationFailed = true;
            destroyIndicatorTextures();
            SuperResolution.LOGGER.warn("Failed to create the present indicator textures", e);
            return false;
        }
    }

    private VulkanTexture createIndicatorTexture(String label) {
        TextureDescription description = TextureDescription.create()
                .type(TextureType.Texture2D)
                .format(TextureFormat.RGBA8)
                .size(1, 1)
                .usages(TextureUsages.create()
                        .sampler()
                        .transferSource()
                        .transferDestination())
                .label(label)
                .build();
        return (VulkanTexture) device.createTexture(description);
    }

    private void fillIndicatorTextures() {
        VulkanCommandBuffer commandBuffer = applicationManagedScheduler != null
                ? device.requireFgCommandPool().createCommandBuffer()
                : device.createCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            commandBuffer.begin();
            recordIndicatorFill(commandBuffer.getNativeCommandBuffer(), stack, realFrameIndicator, 1.0f, 1.0f, 1.0f);
            recordIndicatorFill(commandBuffer.getNativeCommandBuffer(), stack, generatedFrameIndicator, 0.0f, 1.0f, 1.0f);
            commandBuffer.end();
            if (applicationManagedScheduler != null) {
                device.submitCommandBuffer(device.requireFgQueue(), commandBuffer);
            } else {
                device.submitCommandBuffer(commandBuffer);
            }
            commandBuffer.waitForFence();
            realFrameIndicator.setCurrentLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            generatedFrameIndicator.setCurrentLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        } finally {
            commandBuffer.destroy();
        }
    }

    private void recordIndicatorFill(
            VkCommandBuffer nativeCommandBuffer,
            MemoryStack stack,
            VulkanTexture texture,
            float red,
            float green,
            float blue
    ) {
        VkImageMemoryBarrier.Buffer toTransferDst = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(texture.handle())
                .subresourceRange(colorSubresource(stack));
        vkCmdPipelineBarrier(
                nativeCommandBuffer,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                0,
                null,
                null,
                toTransferDst
        );

        VkClearColorValue clearColor = VkClearColorValue.calloc(stack)
                .float32(stack.floats(red, green, blue, 1.0f));
        VkImageSubresourceRange.Buffer clearRange = VkImageSubresourceRange.calloc(1, stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
        vkCmdClearColorImage(
                nativeCommandBuffer,
                texture.handle(),
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                clearColor,
                clearRange
        );

        VkImageMemoryBarrier.Buffer toTransferSrc = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(texture.handle())
                .subresourceRange(colorSubresource(stack));
        vkCmdPipelineBarrier(
                nativeCommandBuffer,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                0,
                null,
                null,
                toTransferSrc
        );
    }

    private void destroyIndicatorTextures() {
        if (realFrameIndicator != null) {
            realFrameIndicator.destroy();
            realFrameIndicator = null;
        }
        if (generatedFrameIndicator != null) {
            generatedFrameIndicator.destroy();
            generatedFrameIndicator = null;
        }
    }

    private void recreateIfRequestedOnControlThread() {
        if (recreateRequested) {
            recreate();
        }
    }

    private void waitForDedicatedPresentQueueIdle() {
        VulkanQueue presentQueue = device.getDedicatedPresentQueue();
        if (presentQueue != null) {
            presentQueue.waitIdle();
        }
    }

    private void recreate() {
        AsyncFrameGenerationScheduler scheduler = applicationManagedScheduler;
        if (scheduler != null && scheduler.isWorkerThread()) {
            throw new IllegalStateException(
                    "Scheduler workers must request swapchain recreation from the control thread"
            );
        }
        if (Thread.holdsLock(swapchainLock)) {
            recreateLocked();
            return;
        }
        if (scheduler == null) {
            pacer.awaitIdle();
            pacer.invalidatePacing();
        } else {
            scheduler.awaitPresentationDrain();
        }
        try {
            if (scheduler != null && device.getFrameGenerationQueue() != null) {
                device.getFrameGenerationQueue().waitIdle();
            }
            synchronized (swapchainLock) {
                recreateLocked();
            }
        } finally {
            if (scheduler != null) {
                scheduler.resumePresenting();
            }
        }
    }

    private void recreateLocked() {
        if (applicationManagedScheduler == null) {
            pacer.invalidatePacing();
        } else if (device.getFrameGenerationQueue() != null) {
            device.getFrameGenerationQueue().waitIdle();
        }
        surface.refreshFramebufferSize();
        if (surface.framebufferWidth() <= 0 || surface.framebufferHeight() <= 0) {
            width = 0;
            height = 0;
            recreateRequested = true;
            return;
        }
        device.getMainQueue().waitIdle();
        waitForDedicatedPresentQueueIdle();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    context.physicalDevice(),
                    context.surface(),
                    capabilities
            ), "query surface capabilities");

            SurfaceFormat format = chooseSurfaceFormat(stack, context.physicalDevice());
            int presentMode = vsync
                    ? VK_PRESENT_MODE_FIFO_KHR
                    : chooseNonVsyncPresentMode(stack, context.physicalDevice());
            int[] extent = chooseExtent(capabilities);
            int requestedImageCount = chooseImageCount(capabilities);

            // Reflex: the swapchain must opt into latency mode at creation for
            // vkSetLatencySleepModeNV / latency markers to be legal on it.
            boolean latencyMode = VulkanLowLatency.isSupported();
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(context.surface())
                    .minImageCount(requestedImageCount)
                    .imageFormat(format.format())
                    .imageColorSpace(format.colorSpace())
                    .imageExtent(VkExtent2D.calloc(stack).set(extent[0], extent[1]))
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(capabilities.currentTransform())
                    .compositeAlpha(chooseCompositeAlpha(capabilities.supportedCompositeAlpha()))
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(swapchain);
            if (latencyMode) {
                VkSwapchainLatencyCreateInfoNV latencyCreateInfo = VkSwapchainLatencyCreateInfoNV.calloc(stack)
                        .sType(NVLowLatency2.VK_STRUCTURE_TYPE_SWAPCHAIN_LATENCY_CREATE_INFO_NV)
                        .latencyModeEnable(true);
                createInfo.pNext(latencyCreateInfo.address());
            }

            LongBuffer swapchainPointer = stack.mallocLong(1);
            check(vkCreateSwapchainKHR(device.getVkDevice(), createInfo, null, swapchainPointer), "create swapchain");
            long newSwapchain = swapchainPointer.get(0);
            long oldSwapchain = swapchain;
            long[] oldRenderFinished = renderFinished;

            IntBuffer imageCount = stack.ints(0);
            check(vkGetSwapchainImagesKHR(device.getVkDevice(), newSwapchain, imageCount, null),
                    "query swapchain images");
            LongBuffer imageBuffer = stack.mallocLong(imageCount.get(0));
            check(vkGetSwapchainImagesKHR(device.getVkDevice(), newSwapchain, imageCount, imageBuffer),
                    "get swapchain images");
            long[] newImages = new long[imageCount.get(0)];
            imageBuffer.get(newImages);
            swapchain = newSwapchain;
            images = newImages;
            imageFormat = format.format();
            this.imageCount = newImages.length;
            imageLayouts = new int[newImages.length];
            width = extent[0];
            height = extent[1];
            renderFinished = createSemaphores(stack, newImages.length);
            swapchainGeneration++;
            VulkanLowLatency.onSwapchainCreated(newSwapchain, latencyMode);
            SuperResolution.LOGGER.info(
                    "Created Vulkan presentation swapchain: {}x{}, images={} "
                            + "(requested={}, min={}, max={}, plannedGeneratedFrames={})",
                    width,
                    height,
                    this.imageCount,
                    requestedImageCount,
                    capabilities.minImageCount(),
                    capabilities.maxImageCount(),
                    plannedGeneratedFrames
            );

            destroySemaphores(oldRenderFinished);
            if (oldSwapchain != VK_NULL_HANDLE) {
                VulkanLowLatency.onSwapchainDestroyed(oldSwapchain);
                vkDestroySwapchainKHR(device.getVkDevice(), oldSwapchain, null);
            }
            recreateRequested = false;
        }
    }

    private SurfaceFormat chooseSurfaceFormat(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        IntBuffer count = stack.ints(0);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, context.surface(), count, null),
                "query surface formats");
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(count.get(0), stack);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, context.surface(), count, formats),
                "get surface formats");

        if (formats.capacity() == 1 && formats.get(0).format() == VK_FORMAT_UNDEFINED) {
            if (supportsBlitDestination(VK_FORMAT_B8G8R8A8_UNORM)) {
                return new SurfaceFormat(VK_FORMAT_B8G8R8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
            }
            return new SurfaceFormat(VK_FORMAT_R8G8B8A8_UNORM, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
        }
        SurfaceFormat fallback = null;
        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR candidate = formats.get(i);
            if (!supportsBlitDestination(candidate.format())) {
                continue;
            }
            SurfaceFormat value = new SurfaceFormat(candidate.format(), candidate.colorSpace());
            if (fallback == null) {
                fallback = value;
            }
            if (candidate.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
                    && candidate.format() == VK_FORMAT_B8G8R8A8_UNORM) {
                return value;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("Vulkan surface has no usable transfer destination format");
        }
        return fallback;
    }

    private int chooseNonVsyncPresentMode(MemoryStack stack, VkPhysicalDevice physicalDevice) {
        IntBuffer count = stack.ints(0);
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, context.surface(), count, null),
                "query present modes");
        IntBuffer modes = stack.mallocInt(count.get(0));
        check(vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, context.surface(), count, modes),
                "get present modes");
        for (int i = 0; i < modes.capacity(); i++) {
            if (modes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR) {
                return VK_PRESENT_MODE_IMMEDIATE_KHR;
            }
        }
        for (int i = 0; i < modes.capacity(); i++) {
            if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return VK_PRESENT_MODE_MAILBOX_KHR;
            }
        }


        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private int[] chooseExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != -1) {
            return new int[]{capabilities.currentExtent().width(), capabilities.currentExtent().height()};
        }
        return new int[]{
                Math.max(capabilities.minImageExtent().width(),
                        Math.min(surface.framebufferWidth(), capabilities.maxImageExtent().width())),
                Math.max(capabilities.minImageExtent().height(),
                        Math.min(surface.framebufferHeight(), capabilities.maxImageExtent().height()))
        };
    }

    private int chooseImageCount(VkSurfaceCapabilitiesKHR capabilities) {
        int desired = DESIRED_SWAPCHAIN_IMAGES;
        if (plannedGeneratedFrames > 0) {
            // Frame generation holds one acquired image per interpolated frame plus
            // the real frame, so the swapchain needs that many beyond the minimum.
            desired = Math.max(desired, capabilities.minImageCount() + plannedGeneratedFrames + 1);
        }
        int requested = Math.max(capabilities.minImageCount(), desired);
        return capabilities.maxImageCount() > 0
                ? Math.min(requested, capabilities.maxImageCount())
                : requested;
    }

    private long createBinarySemaphore(String debugLabel) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            LongBuffer pointer = stack.mallocLong(1);
            check(vkCreateSemaphore(device.getVkDevice(), createInfo, null, pointer), "create binary semaphore");
            long semaphore = pointer.get(0);
            device.setDebugName(VK_OBJECT_TYPE_SEMAPHORE, semaphore, debugLabel);
            return semaphore;
        }
    }

    private void createImageAvailableSemaphores() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long[] semaphores = createSemaphores(stack, imageAvailable.length);
            System.arraycopy(semaphores, 0, imageAvailable, 0, semaphores.length);
        }
    }

    private long[] createSemaphores(MemoryStack stack, int count) {
        long[] semaphores = new long[count];
        VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        for (int i = 0; i < count; i++) {
            LongBuffer pointer = stack.mallocLong(1);
            check(vkCreateSemaphore(device.getVkDevice(), createInfo, null, pointer), "create semaphore");
            semaphores[i] = pointer.get(0);
        }
        return semaphores;
    }

    private void destroySwapchain() {
        images = new long[0];
        imageLayouts = new int[0];
        width = 0;
        height = 0;
        synchronized (applicationManagedTargetLock) {
            applicationManagedTargetCount = 0;
            applicationManagedTargetLock.notifyAll();
        }
        if (swapchain != VK_NULL_HANDLE) {
            VulkanLowLatency.onSwapchainDestroyed(swapchain);
            vkDestroySwapchainKHR(device.getVkDevice(), swapchain, null);
            swapchain = VK_NULL_HANDLE;
        }
    }

    private void destroySemaphores(long[] semaphores) {
        for (long semaphore : semaphores) {
            if (semaphore != VK_NULL_HANDLE) {
                vkDestroySemaphore(device.getVkDevice(), semaphore, null);
            }
        }
    }

    private boolean supportsBlitDestination(int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFormatProperties properties = VkFormatProperties.calloc(stack);
            vkGetPhysicalDeviceFormatProperties(context.physicalDevice(), format, properties);
            int required = VK_FORMAT_FEATURE_TRANSFER_DST_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT;
            return (properties.optimalTilingFeatures() & required) == required;
        }
    }

    private int nextImageAvailableIndex() {
        int index = syncIndex;
        syncIndex = (syncIndex + 1) % imageAvailable.length;
        return index;
    }

    private record SurfaceFormat(int format,

                                 int colorSpace) {
    }

    private record ScheduledPresentTarget(
            int imageIndex,
            long swapchainHandle,
            long imageHandle,
            int layoutAtAcquire,
            long renderFinishedSemaphore,
            VulkanBinarySemaphorePool.Lease acquireLease
    ) {
        private ScheduledPresentTarget {
            if (imageIndex < 0
                    || swapchainHandle == VK_NULL_HANDLE
                    || imageHandle == VK_NULL_HANDLE
                    || renderFinishedSemaphore == VK_NULL_HANDLE
                    || acquireLease == null) {
                throw new IllegalArgumentException("Scheduled present target is invalid");
            }
        }
    }

    private static final class ScheduledTargetAcquireException extends IllegalStateException {
        private final boolean requiresRecreate;
        private final boolean expectedFallback;

        private ScheduledTargetAcquireException(int result) {
            super("Failed to acquire an async frame batch target, VkResult=" + result);
            this.requiresRecreate = result == VK_ERROR_OUT_OF_DATE_KHR;
            this.expectedFallback = result == VK_TIMEOUT;
        }

        private ScheduledTargetAcquireException(String message, boolean requiresRecreate) {
            super(message);
            this.requiresRecreate = requiresRecreate;
            this.expectedFallback = true;
        }

        private boolean requiresRecreate() {
            return requiresRecreate;
        }

        private boolean isExpectedFallback() {
            return expectedFallback;
        }
    }

    private static final class SubmittedCommandBufferCompletion
            implements FrameGenerationDispatchCompletion {
        private final VulkanCommandBuffer commandBuffer;
        private final long submissionGeneration;

        private SubmittedCommandBufferCompletion(
                VulkanCommandBuffer commandBuffer,
                long submissionGeneration
        ) {
            if (commandBuffer == null) {
                throw new IllegalArgumentException("Submitted command buffer cannot be null");
            }
            this.commandBuffer = commandBuffer;
            this.submissionGeneration = submissionGeneration;
        }

        @Override
        public boolean isComplete() {
            return commandBuffer.isSubmissionComplete(submissionGeneration);
        }

        @Override
        public void awaitCompletion() {
            commandBuffer.waitForSubmission(submissionGeneration);
        }
    }

    private static final class SubmittedProviderBatchCompletion
            implements FrameGenerationDispatchCompletion {
        private final FrameGenerationDispatchCompletion providerCompletion;
        private final VulkanCommandBuffer fgCommandBuffer;
        private final long fgSubmissionGeneration;
        private final VulkanCommandBuffer realCommandBuffer;
        private final long realSubmissionGeneration;
        private final VulkanDevice device;
        private final long fgToMainReady;
        private final AtomicBoolean semaphoreDestroyed = new AtomicBoolean();

        private SubmittedProviderBatchCompletion(
                FrameGenerationDispatchCompletion providerCompletion,
                VulkanCommandBuffer fgCommandBuffer,
                long fgSubmissionGeneration,
                VulkanCommandBuffer realCommandBuffer,
                long realSubmissionGeneration,
                VulkanDevice device,
                long fgToMainReady
        ) {
            if (providerCompletion == null
                    || fgCommandBuffer == null
                    || realCommandBuffer == null
                    || device == null
                    || fgToMainReady == VK_NULL_HANDLE) {
                throw new IllegalArgumentException(
                        "Submitted provider completion dependencies cannot be null/zero"
                );
            }
            this.providerCompletion = providerCompletion;
            this.fgCommandBuffer = fgCommandBuffer;
            this.fgSubmissionGeneration = fgSubmissionGeneration;
            this.realCommandBuffer = realCommandBuffer;
            this.realSubmissionGeneration = realSubmissionGeneration;
            this.device = device;
            this.fgToMainReady = fgToMainReady;
        }

        @Override
        public boolean isComplete() {
            boolean complete = providerCompletion.isComplete()
                    && fgCommandBuffer.isSubmissionComplete(fgSubmissionGeneration)
                    && realCommandBuffer.isSubmissionComplete(realSubmissionGeneration);
            if (complete) {
                destroySemaphore();
            }
            return complete;
        }

        @Override
        public void awaitCompletion() {
            providerCompletion.awaitCompletion();
            fgCommandBuffer.waitForSubmission(fgSubmissionGeneration);
            realCommandBuffer.waitForSubmission(realSubmissionGeneration);
            destroySemaphore();
        }

        private void destroySemaphore() {
            if (semaphoreDestroyed.compareAndSet(false, true)) {
                vkDestroySemaphore(device.getVkDevice(), fgToMainReady, null);
            }
        }
    }

    private record PresentSubmission(int imageIndex, FramePresentPlan plan, boolean paced) {
    }
}
