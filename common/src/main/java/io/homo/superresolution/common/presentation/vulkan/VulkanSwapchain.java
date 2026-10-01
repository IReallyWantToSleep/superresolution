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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.framegeneration.ExternalFrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchCompletion;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.perf.FramePacingTrace;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_FORMAT_FEATURE_TRANSFER_DST_BIT;

final class VulkanSwapchain {
    private static final int MAX_IN_FLIGHT_FRAMES = 3;
    private static final int DESIRED_SWAPCHAIN_IMAGES = 3;
    // FrameGenerationMode.X6 generates up to 5 frames; each needs its own acquire.
    private static final int MAX_GENERATED_FRAMES = AsyncFramePresenter.MAX_GENERATED_FRAMES;
    private static final int ACQUIRE_SYNC_SLOTS = MAX_IN_FLIGHT_FRAMES * (MAX_GENERATED_FRAMES + 1);
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
    private final Object swapchainLock = new Object();
    private final Object applicationManagedTargetLock = new Object();
    private final long[] imageAvailable = new long[ACQUIRE_SYNC_SLOTS];
    private final AtomicLong nextPresentTimingId = new AtomicLong(1L);
#if MC_VER >= MC_26_1
    private final PresentTimingSupport presentTimingSupport;
#endif
    private long[] renderFinished = new long[0];
    private VulkanCommandPool presentationCommandPool;
    private VulkanCommandBufferRing presentationCommandBuffers;
    private VulkanBinarySemaphorePool applicationManagedAcquireSemaphores;
    private AsyncFramePresenter asyncFramePresenter;
    private boolean presentationSuspended;
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
    // Whether the live swapchain was built for frame generation. Present mode and the
    // effective vsync state are fixed at creation, so a change here needs a recreate.
    private boolean swapchainFrameGenerationActive;
    private volatile long swapchainGeneration;
    private int applicationManagedTargetCount;

    VulkanSwapchain(VulkanPresentationContext context, VulkanSurface surface) {
        this.context = context;
        this.surface = surface;
        this.device = context.device();
#if MC_VER >= MC_26_1
        this.presentTimingSupport = new PresentTimingSupport(context);
#endif
        this.presentationSuspended = surface.isMinimized();
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

    private static String presentModeName(int presentMode) {
        return switch (presentMode) {
            case VK_PRESENT_MODE_IMMEDIATE_KHR -> "IMMEDIATE";
            case VK_PRESENT_MODE_MAILBOX_KHR -> "MAILBOX";
            case VK_PRESENT_MODE_FIFO_KHR -> "FIFO";
            case VK_PRESENT_MODE_FIFO_RELAXED_KHR -> "FIFO_RELAXED";
            default -> Integer.toString(presentMode);
        };
    }

    public void requestRecreate() {
        recreateRequested = true;
        synchronized (applicationManagedTargetLock) {
            applicationManagedTargetLock.notifyAll();
        }
    }

    long reservePresentTimingId(long preferredId) {
#if MC_VER >= MC_26_1
        if (presentTimingSupport == null || !presentTimingSupport.isEnabled()) {
            return 0L;
        }
        if (preferredId > 0L) {
            nextPresentTimingId.updateAndGet(
                    current -> Math.max(current, preferredId == Long.MAX_VALUE ? Long.MAX_VALUE : preferredId + 1L)
            );
            return preferredId;
        }
        return nextPresentTimingId.getAndIncrement();
#else
        return 0L;
#endif
    }

    public void suspendPresentation() {
        requestRecreate();
        if (asyncFramePresenter != null) {
            asyncFramePresenter.awaitPresentationDrain();
        }
        device.getMainQueue().waitIdle();
        if (asyncFramePresenter != null && device.getFrameGenerationQueue() != null) {
            device.getFrameGenerationQueue().waitIdle();
        }
        waitForDedicatedPresentQueueIdle();
        // Keep the workers paused until a presentable framebuffer exists again. Frames
        // captured while suspended bypass them and only submit the resource-release work.
        presentationSuspended = true;
    }

    public void resumePresentation() {
        presentationSuspended = false;
        if (asyncFramePresenter != null) {
            asyncFramePresenter.resumePresenting();
        }
    }

    public void setVsync(boolean enabled) {
        if (vsync != enabled) {
            vsync = enabled;
            requestRecreate();
        }
    }

    public boolean present(FrameResources frame) {
        AsyncFramePresenter scheduler = ensureAsyncFramePresenter();
        if (scheduler != null) {
            updateApplicationManagedFramePlan();
            recreateIfRequestedOnControlThread();
            FramePacingTrace.Span enqueueTrace = FramePacingTrace.INSTANCE.begin(
                    "presentation_enqueue",
                    frame.logicalFrameIndex(),
                    -1L,
                    -1L,
                    -1L,
                    -1L,
                    "REAL",
                    scheduler.providerId()
            );
            try {
                return scheduler.enqueue(frame, true);
            } finally {
                enqueueTrace.close();
            }
        }
        String externalProviderId = FrameGeneration.mode().getId();
        FramePacingTrace.Span externalTrace = FramePacingTrace.INSTANCE.begin(
                "external_present",
                frame.logicalFrameIndex(),
                -1L,
                -1L,
                -1L,
                -1L,
                "REAL",
                externalProviderId
        );
        try {
            ExternalPresentSubmission submission = submitPresentFrame(frame);
            if (submission == null) {
                externalTrace.complete("fallback", "present_submission_unavailable");
                return false;
            }

            long timingPresentId = reservePresentTimingId(0L);
            try {
                boolean presented = queuePresent(
                        submission.imageIndex(),
                        frame.logicalFrameIndex(),
                        externalProviderId,
                        timingPresentId
                );
                externalTrace.complete(
                        "complete",
                        "presented=" + presented
                );
                return presented;
            } finally {
                FrameGeneration.finishExternalFrame(frame, submission.result());
            }
        } catch (Throwable throwable) {
            externalTrace.complete(
                    "failed",
                    throwable.getClass().getSimpleName() + ": "
                            + String.valueOf(throwable.getMessage())
            );
            throw throwable;
        } finally {
            externalTrace.close();
        }
    }

    private ExternalPresentSubmission submitPresentFrame(FrameResources frame) {
        try {
            if (!frame.hasFinalColor()) {
                consumeWithoutPresentInternal(frame);
                return null;
            }
            plannedGeneratedFrames = 0;
            if (swapchainFrameGenerationActive) {
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
            String providerId = FrameGeneration.mode().getId();
            ExternalFrameGenerationDispatchResult externalResult;
            FramePacingTrace.Span prepareTrace = FramePacingTrace.INSTANCE.begin(
                    "external_frame_generation_prepare",
                    frame.logicalFrameIndex(),
                    -1L,
                    -1L,
                    imageIndex,
                    -1L,
                    "REAL",
                    providerId
            );
            try {
                externalResult = FrameGeneration.prepareExternalFrame(
                        frame,
                        width,
                        height,
                        imageFormat,
                        imageCount,
                        commandBuffer.getNativeCommandBuffer().address()
                );
                prepareTrace.complete("complete", "");
            } catch (Throwable throwable) {
                prepareTrace.complete(
                        "failed",
                        throwable.getClass().getSimpleName() + ": "
                                + String.valueOf(throwable.getMessage())
                );
                throw throwable;
            } finally {
                prepareTrace.close();
            }

            FramePacingTrace.Span blitTrace = FramePacingTrace.INSTANCE.begin(
                    "external_present_blit_submit",
                    frame.logicalFrameIndex(),
                    -1L,
                    -1L,
                    imageIndex,
                    -1L,
                    "REAL",
                    providerId
            );
            try {
                recordBlit(commandBuffer, frame.finalColorVulkanTexture(), imageIndex, false);
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
                blitTrace.complete("complete", "");
            } catch (Throwable throwable) {
                blitTrace.complete(
                        "failed",
                        throwable.getClass().getSimpleName() + ": "
                                + String.valueOf(throwable.getMessage())
                );
                throw throwable;
            } finally {
                blitTrace.close();
            }

            return new ExternalPresentSubmission(imageIndex, externalResult);
        } catch (Throwable throwable) {
            FrameGeneration.disableFrameGeneration();
            throw throwable;
        }
    }

    public void consumeWithoutPresent(FrameResources frame) {
        if (presentationSuspended || surface.isMinimized()) {
            consumeWithoutPresentInternal(frame);
            return;
        }
        AsyncFramePresenter scheduler = ensureAsyncFramePresenter();
        if (scheduler != null) {
            scheduler.enqueue(frame, false);
            return;
        }
        consumeWithoutPresentInternal(frame);
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
        AsyncFramePresenter scheduler = asyncFramePresenter;
        if (scheduler == null || !scheduler.providerId().equals(providerId)) {
            return false;
        }
        scheduler.shutdownProviderOnFrameGenerationThread(providerId, teardown);
        return true;
    }

    public void destroy() {
        Throwable failure = null;
        AsyncFramePresenter presenter = asyncFramePresenter;
        try {
            if (presenter != null) {
                presenter.close();
            }
        } catch (Throwable throwable) {
            failure = throwable;
        }
        if (presenter != null && !presenter.isTerminated()) {
            throw new IllegalStateException("Cannot destroy presentation resources while a worker is active", failure);
        }
        asyncFramePresenter = null;
        device.getMainQueue().waitIdle();
        if (device.getFrameGenerationQueue() != null) {
            device.getFrameGenerationQueue().waitIdle();
        }
        waitForDedicatedPresentQueueIdle();
        commandBuffers.destroy();
        if (presentationCommandBuffers != null) {
            presentationCommandBuffers.destroy();
            presentationCommandBuffers = null;
            presentationCommandPool.destroy();
            presentationCommandPool = null;
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
        if (failure != null) {
            throw new IllegalStateException("Application-managed presenter shutdown failed", failure);
        }
    }

    private boolean queuePresent(
            int imageIndex,
            int logicalFrame,
            String providerId,
            long timingPresentId
    ) {
        FramePacingTrace.Span presentTrace = FramePacingTrace.INSTANCE.begin(
                "present_call",
                logicalFrame,
                -1L,
                -1L,
                imageIndex,
                timingPresentId,
                "REAL",
                providerId
        );
        int result;
        try {
            result = presentImage(
                    imageIndex,
                    false,
                    swapchain,
                    renderFinished[imageIndex],
                    0L,
                    timingPresentId,
                    0L,
                    false,
                    new FramePacingTrace.Context(
                            logicalFrame,
                            -1L,
                            -1L,
                            imageIndex,
                            timingPresentId,
                            "REAL",
                            providerId
                    )
            );
            presentTrace.complete("complete", "vk_result=" + result);
        } catch (Throwable throwable) {
            presentTrace.complete(
                    "failed",
                    throwable.getClass().getSimpleName() + ": "
                            + String.valueOf(throwable.getMessage())
            );
            throw throwable;
        } finally {
            presentTrace.close();
        }
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
     * the present thread; the queue lock serializes it against command submissions.
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
                0L,
                0L,
                false,
                FramePacingTrace.Context.empty()
        );
    }

    private int presentImage(
            int imageIndex,
            boolean outOfBandPresent,
            long targetSwapchain,
            long presentReadyBinary,
            long immutablePresentId,
            long timingPresentId,
            long latencyMarkerId,
            boolean applicationManaged,
            FramePacingTrace.Context timingContext
    ) {
        if (!surface.isShown()) {
            return VK_SUCCESS;
        }
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
                long markerId = applicationManaged ? latencyMarkerId : presentId;
                long pNext = 0L;
                if (presentId != 0L) {
                    VkPresentIdKHR presentIdInfo = VkPresentIdKHR.calloc(stack)
                            .sType(KHRPresentId.VK_STRUCTURE_TYPE_PRESENT_ID_KHR)
                            .pNext(pNext)
                            .swapchainCount(1)
                            .pPresentIds(stack.longs(presentId));
                    pNext = presentIdInfo.address();
                }
#if MC_VER >= MC_26_1
                long timingBasePNext = pNext;
                if (timingPresentId != 0L && presentTimingSupport.isEnabled()) {
                    VkPresentId2KHR presentId2Info = VkPresentId2KHR.calloc(stack)
                            .sType(KHRPresentId2.VK_STRUCTURE_TYPE_PRESENT_ID_2_KHR)
                            .pNext(pNext)
                            .swapchainCount(1)
                            .pPresentIds(stack.longs(timingPresentId));
                    pNext = presentId2Info.address();
                    pNext = presentTimingSupport.appendPresentInfo(stack, timingPresentId, pNext);
                    presentTimingSupport.register(timingPresentId, timingContext);
                }
#endif
                if (pNext != 0L) {
                    presentInfo.pNext(pNext);
                }
                if (applicationManaged) {
                    VulkanLowLatency.presentMarker(markerId, outOfBandPresent, true);
                } else {
                    LowLatency.beginPresent();
                }
                try {
                    VulkanQueue presentQueue = applicationManaged
                            ? device.getApplicationManagedPresentQueue()
                            : device.getMainQueue();
                    int result;
                    synchronized (presentQueue.submitLock()) {
                        result = vkQueuePresentKHR(presentQueue.getQueue(), presentInfo);
                    }
#if MC_VER >= MC_26_1
                    if (timingPresentId != 0L && presentTimingSupport.isEnabled()) {
                        presentTimingSupport.collect();
                        if (result == EXTPresentTiming.VK_ERROR_PRESENT_TIMING_QUEUE_FULL_EXT) {
                            presentTimingSupport.discard(timingPresentId);
                            presentInfo.pNext(timingBasePNext);
                            synchronized (presentQueue.submitLock()) {
                                result = vkQueuePresentKHR(presentQueue.getQueue(), presentInfo);
                            }
                        }
                    }
#endif
                    return result;
                } catch (Throwable throwable) {
#if MC_VER >= MC_26_1
                    if (timingPresentId != 0L) {
                        presentTimingSupport.discard(timingPresentId);
                    }
#endif
                    throw throwable;
                } finally {
                    if (applicationManaged) {
                        VulkanLowLatency.presentMarker(markerId, outOfBandPresent, false);
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

    private synchronized AsyncFramePresenter ensureAsyncFramePresenter() {
        if (asyncFramePresenter != null) {
            return asyncFramePresenter;
        }
        String providerId = FrameGeneration.activeApplicationManagedProviderId();
        if (providerId.isEmpty()) {
            return null;
        }
        if (!device.asyncDispatchCapabilities().available()) {
            return null;
        }
        asyncFramePresenter =
                new AsyncFramePresenter(this, device, providerId);
        return asyncFramePresenter;
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

    PresentPacer presentPacer() {
        return context.presentPacer();
    }

    PresentationConfiguration presentationConfiguration() {
        synchronized (swapchainLock) {
            return new PresentationConfiguration(swapchainGeneration, swapchain, width, height,
                    imageFormat, imageCount,
                    !recreateRequested && swapchain != VK_NULL_HANDLE && width > 0 && height > 0);
        }
    }

    boolean isCurrentConfiguration(PresentationConfiguration configuration) {
        return configuration.generation() == swapchainGeneration && configuration.handle() == swapchain;
    }

    void ensurePresentBatchFits(int targetCount) {
        if (recreateRequested || targetCount > Math.max(0, imageCount - 1)) {
            requestRecreate();
            throw new PresentTargetUnavailableException();
        }
    }

    PresentTarget acquirePresentTarget() {
        synchronized (applicationManagedTargetLock) {
            if (recreateRequested || applicationManagedTargetCount >= imageCount - 1) {
                throw new PresentTargetUnavailableException();
            }
            applicationManagedTargetCount++;
        }
        VulkanBinarySemaphorePool.Lease lease = null;
        boolean acquired = false;
        try {
            lease = acquireApplicationManagedSemaphore();
            int imageIndex = acquireImage(lease.semaphore());
            if (imageIndex < 0) {
                if (imageIndex == ACQUIRE_OUT_OF_DATE) {
                    requestRecreate();
                }
                throw new PresentTargetUnavailableException();
            }
            acquired = true;
            return new PresentTarget(swapchainGeneration, swapchain, imageIndex,
                    images[imageIndex], imageLayouts[imageIndex], renderFinished[imageIndex], lease);
        } finally {
            if (!acquired) {
                if (lease != null) {
                    lease.close();
                }
                releasePresentTarget();
            }
        }
    }

    PresentBlitSubmission submitPresentBlit(
            PresentTarget target, PresentImage image, long[] sourceWaits, long[] captureSignals
    ) {
        VulkanCommandBuffer buffer = presentationCommandBuffers().acquire(device);
        buffer.reset();
        buffer.begin();
        recordBlit(buffer, image.source(), target.imageIndex(), image.kind() == PresentImage.Kind.GENERATED);
        buffer.end();
        long[] waits = new long[sourceWaits.length + 1];
        waits[0] = target.acquireLease().semaphore();
        System.arraycopy(sourceWaits, 0, waits, 1, sourceWaits.length);
        int[] stages = new int[waits.length];
        Arrays.fill(stages, VK_PIPELINE_STAGE_TRANSFER_BIT);
        long[] signals = new long[captureSignals.length + 1];
        signals[0] = target.renderFinishedSemaphore();
        System.arraycopy(captureSignals, 0, signals, 1, captureSignals.length);
        VulkanLowLatency.renderSubmitMarker(image.latencyMarkerId(), image.outOfBand(), true);
        try {
            VulkanDevice.IssuedSubmission issued = device.submitCommandBufferIssued(
                    device.getMainQueue(), buffer, waits, stages, signals,
                    image.outOfBand() ? 0L : image.latencyMarkerId());
            return new PresentBlitSubmission(buffer, issued.fence(), issued.submissionTicket(),
                    GpuReadyFences.completion(buffer));
        } catch (VulkanDevice.SubmissionTicketPublicationException exception) {
            // The GPU wait landed even though its CPU-issued ticket could not be published.
            buffer.waitForSubmission(exception.submissionGeneration());
            throw exception;
        } finally {
            VulkanLowLatency.renderSubmitMarker(image.latencyMarkerId(), image.outOfBand(), false);
        }
    }

    PresentBlitSubmission submitPresentationRelease(long[] waits, long[] signals) {
        VulkanCommandBuffer buffer = presentationCommandBuffers().acquire(device);
        buffer.reset();
        buffer.begin();
        buffer.end();
        int[] stages = new int[waits.length];
        Arrays.fill(stages, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        long fence = device.submitCommandBuffer(device.getMainQueue(), buffer, waits, stages, signals);
        return new PresentBlitSubmission(buffer, fence, 0L, GpuReadyFences.completion(buffer));
    }

    int presentTarget(
            PresentTarget target,
            PresentImage image,
            FramePacingTrace.Context timingContext
    ) {
        synchronized (swapchainLock) {
            if (target.generation() != swapchainGeneration || target.swapchainHandle() != swapchain) {
                return VK_ERROR_OUT_OF_DATE_KHR;
            }
            return presentImage(target.imageIndex(), image.outOfBand(), target.swapchainHandle(),
                    target.renderFinishedSemaphore(), image.presentId(), image.timingPresentId(),
                    image.latencyMarkerId(), true, timingContext);
        }
    }

    void releasePresentTarget() {
        synchronized (applicationManagedTargetLock) {
            applicationManagedTargetCount--;
            applicationManagedTargetLock.notifyAll();
        }
    }

    void discardPresentTarget(PresentTarget target, boolean rendered) {
        try {
            if (!rendered) {
                VulkanCommandBuffer buffer = presentationCommandBuffers().acquire(device);
                buffer.reset();
                buffer.begin();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    if (target.layoutAtAcquire() != VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
                        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                                .srcAccessMask(sourceAccessMask(target.layoutAtAcquire()))
                                .dstAccessMask(0)
                                .oldLayout(target.layoutAtAcquire())
                                .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                                .image(target.imageHandle())
                                .subresourceRange(colorSubresource(stack));
                        vkCmdPipelineBarrier(buffer.getNativeCommandBuffer(),
                                VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                                0, null, null, barrier);
                    }
                }
                buffer.end();
                device.submitCommandBuffer(device.getMainQueue(), buffer,
                        new long[]{target.acquireLease().semaphore()},
                        new int[]{VK_PIPELINE_STAGE_ALL_COMMANDS_BIT},
                        new long[]{target.renderFinishedSemaphore()});
                buffer.waitForFence();
                imageLayouts[target.imageIndex()] = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
            }
            int result = presentImage(target.imageIndex(), true, target.swapchainHandle(),
                    target.renderFinishedSemaphore(), 0L, 0L, 0L, true, FramePacingTrace.Context.empty());
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                requestRecreate();
            } else {
                check(result, "return an unpresented swapchain image");
            }
        } finally {
            target.acquireLease().close();
            releasePresentTarget();
        }
    }

    private VulkanCommandBufferRing presentationCommandBuffers() {
        if (presentationCommandBuffers == null) {
            // The present thread must not record into the render or FG thread's command pool.
            presentationCommandPool = device.createCommandPool(device.getMainQueue(), "PresentationCommandPool",
                    CommandPoolFlags.Reset, CommandPoolFlags.Transient);
            presentationCommandBuffers = new VulkanCommandBufferRing(
                    MAX_IN_FLIGHT_FRAMES * (MAX_GENERATED_FRAMES + 1) + 1, presentationCommandPool);
        }
        return presentationCommandBuffers;
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

    private void recordBlit(
            VulkanCommandBuffer commandBuffer,
            VulkanTexture source,
            int imageIndex,
            boolean generatedFrame
    ) {
        VkCommandBuffer nativeCommandBuffer = commandBuffer.getNativeCommandBuffer();
        VulkanTimestampProfiler profiler = device.timestampProfiler();
        int timestampSlot = profiler == null
                ? -1
                : profiler.beginRegion(nativeCommandBuffer, PerformanceTracker.VK_PRESENT_BLIT);
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
        if (timestampSlot >= 0) {
            profiler.endRegion(nativeCommandBuffer, timestampSlot);
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
        VulkanCommandBuffer commandBuffer = presentationCommandPool != null
                ? presentationCommandPool.createCommandBuffer()
                : device.createCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            commandBuffer.begin();
            recordIndicatorFill(commandBuffer.getNativeCommandBuffer(), stack, realFrameIndicator, 1.0f, 1.0f, 1.0f);
            recordIndicatorFill(commandBuffer.getNativeCommandBuffer(), stack, generatedFrameIndicator, 0.0f, 1.0f, 1.0f);
            commandBuffer.end();
            device.submitCommandBuffer(commandBuffer);
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
        AsyncFramePresenter scheduler = asyncFramePresenter;
        if (scheduler != null) {
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
        if (asyncFramePresenter != null
                && device.getFrameGenerationQueue() != null) {
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
#if MC_VER >= MC_26_1
            PresentTimingSupport.SurfaceSupport timingSurfaceSupport =
                    PresentTimingSupport.querySurfaceSupport(context, stack);
#endif

            SurfaceFormat format = chooseSurfaceFormat(stack, context.physicalDevice());
            // Frame generation meters its own presents, so the display must not gate them
            // as well: FIFO caps the batch at the refresh rate and pushes the backpressure
            // onto vkAcquireNextImageKHR, which this swapchain gives up on after
            // ACQUIRE_TIMEOUT_NANOS and falls back to a Real-only frame. Vsync is therefore
            // forced off for as long as generation is running.
            boolean frameGenerationActive = plannedGeneratedFrames > 0;
            int presentMode = (vsync && !frameGenerationActive)
                    ? VK_PRESENT_MODE_FIFO_KHR
                    : chooseNonVsyncPresentMode(
                    stack,
                    context.physicalDevice(),
                    frameGenerationActive
            );
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
#if MC_VER >= MC_26_1
            if (timingSurfaceSupport.presentId2Supported()
                    && context.renderSystem().isPresentId2Enabled()) {
                createInfo.flags(
                        createInfo.flags() | KHRPresentId2.VK_SWAPCHAIN_CREATE_PRESENT_ID_2_BIT_KHR
                );
            }
            if (timingSurfaceSupport.presentTimingSupported()
                    && context.renderSystem().isPresentTimingEnabled()) {
                createInfo.flags(
                        createInfo.flags() | EXTPresentTiming.VK_SWAPCHAIN_CREATE_PRESENT_TIMING_BIT_EXT
                );
            }
#endif
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
            swapchainFrameGenerationActive = frameGenerationActive;
#if MC_VER >= MC_26_1
            presentTimingSupport.configure(newSwapchain, timingSurfaceSupport);
#endif
            VulkanLowLatency.onSwapchainCreated(newSwapchain, latencyMode);
            SuperResolution.LOGGER.info(
                    "Created Vulkan presentation swapchain: {}x{}, images={}, presentMode={} "
                            + "(requested={}, min={}, max={}, plannedGeneratedFrames={})",
                    width,
                    height,
                    this.imageCount,
                    presentModeName(presentMode),
                    requestedImageCount,
                    capabilities.minImageCount(),
                    capabilities.maxImageCount(),
                    plannedGeneratedFrames
            );
            if (frameGenerationActive && vsync) {
                SuperResolution.LOGGER.info(
                        "Vsync is ignored while frame generation is active: the pacer meters "
                                + "presents itself and FIFO would cap them at the refresh rate"
                );
            }

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

    private int chooseNonVsyncPresentMode(
            MemoryStack stack,
            VkPhysicalDevice physicalDevice,
            boolean frameGenerationActive
    ) {
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
        // Mailbox replaces the queued image rather than displaying it, so every generated
        // frame the pacer spaces out inside one refresh interval is discarded - exactly
        // the frames generation just paid for. It stays the right low-latency choice while
        // generation is off, where there is no metered cadence to destroy.
        if (!frameGenerationActive) {
            for (int i = 0; i < modes.capacity(); i++) {
                if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    return VK_PRESENT_MODE_MAILBOX_KHR;
                }
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
#if MC_VER >= MC_26_1
        presentTimingSupport.reset();
#endif
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

    record PresentationConfiguration(
            long generation,

            long handle,

            int width,

            int height,

            int format,

            int imageCount,

            boolean available
    ) {
    }

    record PresentTarget(
            long generation,

            long swapchainHandle,

            int imageIndex,

            long imageHandle,

            int layoutAtAcquire,

            long renderFinishedSemaphore,

            VulkanBinarySemaphorePool.Lease acquireLease
    ) {
    }

    record PresentBlitSubmission(
            VulkanCommandBuffer commandBuffer,

            long fence,

            long submissionTicket,

            FrameGenerationDispatchCompletion completion
    ) {
    }

    static final class PresentTargetUnavailableException extends IllegalStateException {
    }

    private record ExternalPresentSubmission(
            int imageIndex,

            ExternalFrameGenerationDispatchResult result
    ) {
    }
}
