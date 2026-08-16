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

import io.homo.superresolution.common.presentation.vulkan.FramePacingTiming;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.texture.GlImportableTexture2D;
import io.homo.superresolution.core.graphics.vulkan.VkGlInteropSemaphore;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO;
import static org.lwjgl.vulkan.VK12.vkWaitSemaphores;

public final class FrameResources {
    private final int index;
    private final VulkanDevice device;
    private final FramePacingTiming framePacingTiming;
    private final FrameTextureResource finalColor;
    private final FrameTextureResource hudlessColor;
    private final FrameTextureResource depth;
    private final FrameTextureResource motionVector;
    private final FrameResourceLifecycle lifecycle = new FrameResourceLifecycle();
    private final Object borrowedInputReleaseMonitor = new Object();
    private volatile VulkanCommandBuffer submittedCommandBuffer;
    private volatile long submittedCommandBufferGeneration;
    private volatile long fence;
    private volatile long generation;
    private volatile int logicalFrameIndex;
    private volatile boolean unrecoverable;
    private volatile long dlssGInputCompletionSemaphore;
    private volatile long dlssGInputCompletionValue;
    private boolean borrowedInputReleaseRequired;
    private boolean borrowedInputReleaseSubmitted;
    private boolean borrowedInputReleaseFailed;

    FrameResources(
            int index,
            VulkanDevice device,
            FramePacingTiming framePacingTiming
    ) {
        if (framePacingTiming == null) {
            throw new IllegalArgumentException("framePacingTiming cannot be null");
        }
        this.index = index;
        this.device = device;
        this.framePacingTiming = framePacingTiming;
        finalColor = new FrameTextureResource(device, "SRPresentationFinalColor-" + index);
        hudlessColor = new FrameTextureResource(device, "SRPresentationHudlessColor-" + index);
        depth = new FrameTextureResource(device, "SRPresentationDepth-" + index);
        motionVector = new FrameTextureResource(device, "SRPresentationMotionVector-" + index);
    }

    void begin(long newGeneration, int newLogicalFrameIndex) {
        if (unrecoverable) {
            throw new IllegalStateException("Frame capture slot cannot be reused after ownership recovery failed");
        }
        awaitReusable();
        generation = newGeneration;
        logicalFrameIndex = newLogicalFrameIndex;
        finalColor.beginFrame();
        hudlessColor.beginFrame();
        depth.beginFrame();
        motionVector.beginFrame();
        submittedCommandBuffer = null;
        submittedCommandBufferGeneration = 0L;
        fence = 0L;
        clearDlssGInputCompletion();
        resetBorrowedInputReleaseSubmission();
        lifecycle.beginRecording();
    }

    void copyFinalColor(ITexture source) {
        requireWritable();
        finalColor.copyFrom(source, false);
    }

    void copyHudlessColor(ITexture source) {
        requireWritable();
        hudlessColor.copyFrom(source, false);
    }

    void copyDepth(ITexture source) {
        requireWritable();
        depth.copyFrom(source, false);
    }

    void copyMotionVector(ITexture source) {
        requireWritable();
        motionVector.copyFrom(source, true);
    }

    void borrowDepth(
            VulkanTexture vkTexture,
            GlImportableTexture2D glTexture,
            VkGlInteropSemaphore ready,
            VkGlInteropSemaphore release
    ) {
        requireWritable();
        depth.borrow(vkTexture, glTexture, ready, release);
        if (depth.isValid()) {
            requireBorrowedInputReleaseSubmission();
        }
    }

    void borrowMotionVector(
            VulkanTexture vkTexture,
            GlImportableTexture2D glTexture,
            VkGlInteropSemaphore ready,
            VkGlInteropSemaphore release
    ) {
        requireWritable();
        motionVector.borrow(vkTexture, glTexture, ready, release);
        if (motionVector.isValid()) {
            requireBorrowedInputReleaseSubmission();
        }
    }

    void seal() {
        lifecycle.seal();
    }

    void discardEmptyRecording() {
        if (hasAnyResource()) {
            throw new IllegalStateException("Cannot discard a capture slot that owns resources");
        }
        lifecycle.discardEmptyRecording();
    }

    public void markQueued() {
        lifecycle.markQueued();
    }

    public void markDispatching() {
        lifecycle.markDispatching();
    }

    public void markSubmitted(VulkanCommandBuffer commandBuffer, long submittedFence) {
        if (commandBuffer == null || submittedFence == 0L) {
            throw new IllegalArgumentException(
                    "Submitted command buffer and fence must be non-null/non-zero"
            );
        }
        lifecycle.requireSubmittable();
        long commandBufferGeneration = commandBuffer.submissionGeneration();
        if (commandBufferGeneration <= 0L) {
            throw new IllegalStateException(
                    "Frame capture cannot track a command buffer without a submission generation"
            );
        }
        submittedCommandBuffer = commandBuffer;
        submittedCommandBufferGeneration = commandBufferGeneration;
        fence = submittedFence;
        finalColor.markSubmitted();
        hudlessColor.markSubmitted();
        depth.markSubmitted();
        motionVector.markSubmitted();
        lifecycle.markSubmitted();
        publishBorrowedInputReleaseSubmission();
    }

    public void markUnrecoverable() {
        unrecoverable = true;
        failBorrowedInputReleaseSubmission();
    }

    /**
     * Waits until a queue submission has published the release signals for borrowed
     * interop inputs. OpenGL must not wait on those semaphores before that submission exists.
     */
    public void awaitBorrowedInputReleaseSubmission() {
        synchronized (borrowedInputReleaseMonitor) {
            while (borrowedInputReleaseRequired
                    && !borrowedInputReleaseSubmitted
                    && !borrowedInputReleaseFailed) {
                long waitStartedAtNanos = System.nanoTime();
                try {
                    borrowedInputReleaseMonitor.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for borrowed interop input release submission",
                            exception
                    );
                } finally {
                    framePacingTiming.recordExcludedWait(
                            Math.max(0L, System.nanoTime() - waitStartedAtNanos)
                    );
                }
            }
            if (borrowedInputReleaseFailed) {
                throw new IllegalStateException(
                        "Borrowed interop input release could not be submitted"
                );
            }
        }
    }

    void destroy() {
        awaitReusable();
        finalColor.destroy();
        hudlessColor.destroy();
        depth.destroy();
        motionVector.destroy();
    }

    public int index() {
        return index;
    }

    public long generation() {
        return generation;
    }

    public int logicalFrameIndex() {
        return logicalFrameIndex;
    }

    public boolean hasFinalColor() {
        return finalColor.isValid();
    }

    public boolean hasHudlessColor() {
        return hudlessColor.isValid();
    }

    public boolean hasDepth() {
        return depth.isValid();
    }

    public boolean hasMotionVector() {
        return motionVector.isValid();
    }

    public boolean hasAnyResource() {
        return hasFinalColor() || hasHudlessColor() || hasDepth() || hasMotionVector();
    }

    public boolean isSealed() {
        return switch (lifecycle.state()) {
            case SEALED, QUEUED, DISPATCHING, SUBMITTED -> true;
            case REUSABLE, RECORDING -> false;
        };
    }

    public boolean isSubmitted() {
        return lifecycle.state() == FrameResourceState.SUBMITTED && fence != 0L;
    }

    public FrameResourceState state() {
        return lifecycle.state();
    }

    public VulkanTexture finalColorVulkanTexture() {
        return finalColor.vkTexture();
    }

    public VulkanTexture hudlessColorVulkanTexture() {
        return hudlessColor.vkTexture();
    }

    public VulkanTexture depthVulkanTexture() {
        return depth.vkTexture();
    }

    public VulkanTexture motionVectorVulkanTexture() {
        return motionVector.vkTexture();
    }

    public GlImportableTexture2D finalColorGlTexture() {
        return finalColor.glTexture();
    }

    public GlImportableTexture2D hudlessColorGlTexture() {
        return hudlessColor.glTexture();
    }

    public GlImportableTexture2D depthGlTexture() {
        return depth.glTexture();
    }

    public GlImportableTexture2D motionVectorGlTexture() {
        return motionVector.glTexture();
    }

    public long[] readySemaphores() {
        List<Long> semaphores = new ArrayList<>(4);
        addReadySemaphore(semaphores, finalColor);
        addReadySemaphore(semaphores, hudlessColor);
        addReadySemaphore(semaphores, depth);
        addReadySemaphore(semaphores, motionVector);
        return semaphores.stream().mapToLong(Long::longValue).toArray();
    }

    public long[] releaseSemaphores() {
        List<Long> semaphores = new ArrayList<>(4);
        addReleaseSemaphore(semaphores, finalColor);
        addReleaseSemaphore(semaphores, hudlessColor);
        addReleaseSemaphore(semaphores, depth);
        addReleaseSemaphore(semaphores, motionVector);
        return semaphores.stream().mapToLong(Long::longValue).toArray();
    }

    private void awaitReusable() {
        FrameResourceState state = lifecycle.state();
        if (state == FrameResourceState.REUSABLE) {
            return;
        }
        if (state != FrameResourceState.SUBMITTED) {
            throw new IllegalStateException(
                    "Frame capture slot is still owned in state " + state
            );
        }
        VulkanCommandBuffer commandBuffer = submittedCommandBuffer;
        long commandBufferGeneration = submittedCommandBufferGeneration;
        if (commandBuffer != null && commandBufferGeneration > 0L) {
            commandBuffer.waitForSubmission(commandBufferGeneration);
        }
        awaitDlssGInputs();
        finalColor.awaitOwnedRelease();
        hudlessColor.awaitOwnedRelease();
        depth.awaitOwnedRelease();
        motionVector.awaitOwnedRelease();
        submittedCommandBuffer = null;
        submittedCommandBufferGeneration = 0L;
        fence = 0L;
        lifecycle.markReusable();
    }
    public void setDlssGInputCompletion(long semaphore, long value) {
        if (!isSubmitted() || semaphore == 0L || value <= 0L) {
            throw new IllegalArgumentException("Invalid DLSS-G input completion timeline");
        }
        dlssGInputCompletionSemaphore = semaphore;
        dlssGInputCompletionValue = value;
    }

    public void clearDlssGInputCompletion() {
        dlssGInputCompletionSemaphore = 0L;
        dlssGInputCompletionValue = 0L;
    }
    private void requireWritable() {
        if (lifecycle.state() != FrameResourceState.RECORDING) {
            throw new IllegalStateException(
                    "Frame capture is not writable in state " + lifecycle.state()
            );
        }
    }
    private void awaitDlssGInputs() {
        if (dlssGInputCompletionSemaphore == 0L) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .semaphoreCount(1)
                    .pSemaphores(stack.longs(dlssGInputCompletionSemaphore))
                    .pValues(stack.longs(dlssGInputCompletionValue));
            int result = vkWaitSemaphores(device.getVkDevice(), waitInfo, Long.MAX_VALUE);
            if (result != VK_SUCCESS) {
                throw new IllegalStateException(
                        "Failed to wait for DLSS-G input completion, VkResult=" + result
                );
            }
        }
        clearDlssGInputCompletion();
    }

    private void resetBorrowedInputReleaseSubmission() {
        synchronized (borrowedInputReleaseMonitor) {
            borrowedInputReleaseRequired = false;
            borrowedInputReleaseSubmitted = false;
            borrowedInputReleaseFailed = false;
        }
    }

    private void requireBorrowedInputReleaseSubmission() {
        synchronized (borrowedInputReleaseMonitor) {
            borrowedInputReleaseRequired = true;
        }
    }

    private void publishBorrowedInputReleaseSubmission() {
        synchronized (borrowedInputReleaseMonitor) {
            if (borrowedInputReleaseRequired) {
                borrowedInputReleaseSubmitted = true;
                borrowedInputReleaseMonitor.notifyAll();
            }
        }
    }

    private void failBorrowedInputReleaseSubmission() {
        synchronized (borrowedInputReleaseMonitor) {
            if (borrowedInputReleaseRequired) {
                borrowedInputReleaseFailed = true;
                borrowedInputReleaseMonitor.notifyAll();
            }
        }
    }

    private static void addReadySemaphore(List<Long> semaphores, FrameTextureResource resource) {
        if (resource.isValid()) {
            semaphores.add(resource.readySemaphore());
        }
    }

    private static void addReleaseSemaphore(List<Long> semaphores, FrameTextureResource resource) {
        if (resource.isValid()) {
            semaphores.add(resource.releaseSemaphore());
        }
    }
}
