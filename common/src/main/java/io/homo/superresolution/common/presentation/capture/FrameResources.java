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

import io.homo.superresolution.core.RenderSystems;
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
    private final FrameTextureResource finalColor;
    private final FrameTextureResource worldColor;
    private final FrameTextureResource depth;
    private final FrameTextureResource motionVector;
    private VulkanCommandBuffer submittedCommandBuffer;
    private long fence;
    private long generation;
    private int logicalFrameIndex;
    private boolean sealed;
    private boolean unrecoverable;
    private long dlssGInputCompletionSemaphore;
    private long dlssGInputCompletionValue;

    FrameResources(int index, VulkanDevice device) {
        this.index = index;
        finalColor = new FrameTextureResource(device, "SRPresentationFinalColor-" + index);
        worldColor = new FrameTextureResource(device, "SRPresentationWorldColor-" + index);
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
        worldColor.beginFrame();
        depth.beginFrame();
        motionVector.beginFrame();
        submittedCommandBuffer = null;
        fence = 0L;
        sealed = false;
    }

    void copyFinalColor(ITexture source) {
        requireWritable();
        finalColor.copyFrom(source, false);
    }

    void copyWorldColor(ITexture source) {
        requireWritable();
        worldColor.copyFrom(source, false);
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
    }

    void borrowMotionVector(
            VulkanTexture vkTexture,
            GlImportableTexture2D glTexture,
            VkGlInteropSemaphore ready,
            VkGlInteropSemaphore release
    ) {
        requireWritable();
        motionVector.borrow(vkTexture, glTexture, ready, release);
    }

    void seal() {
        sealed = true;
    }

    public void markSubmitted(VulkanCommandBuffer commandBuffer, long submittedFence) {
        if (!sealed) {
            throw new IllegalStateException("Frame capture must be sealed before submission");
        }
        submittedCommandBuffer = commandBuffer;
        fence = submittedFence;
        finalColor.markSubmitted();
        worldColor.markSubmitted();
        depth.markSubmitted();
        motionVector.markSubmitted();
    }

    public void markUnrecoverable() {
        unrecoverable = true;
    }

    void destroy() {
        awaitReusable();
        finalColor.destroy();
        worldColor.destroy();
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

    public boolean hasWorldColor() {
        return worldColor.isValid();
    }

    public boolean hasDepth() {
        return depth.isValid();
    }

    public boolean hasMotionVector() {
        return motionVector.isValid();
    }

    public boolean hasAnyResource() {
        return hasFinalColor() || hasWorldColor() || hasDepth() || hasMotionVector();
    }

    public boolean isSealed() {
        return sealed;
    }

    public boolean isSubmitted() {
        return fence != 0L;
    }

    public VulkanTexture finalColorVulkanTexture() {
        return finalColor.vkTexture();
    }

    public VulkanTexture worldColorVulkanTexture() {
        return worldColor.vkTexture();
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

    public GlImportableTexture2D worldColorGlTexture() {
        return worldColor.glTexture();
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
        addReadySemaphore(semaphores, worldColor);
        addReadySemaphore(semaphores, depth);
        addReadySemaphore(semaphores, motionVector);
        return semaphores.stream().mapToLong(Long::longValue).toArray();
    }

    public long[] releaseSemaphores() {
        List<Long> semaphores = new ArrayList<>(4);
        addReleaseSemaphore(semaphores, finalColor);
        addReleaseSemaphore(semaphores, worldColor);
        addReleaseSemaphore(semaphores, depth);
        addReleaseSemaphore(semaphores, motionVector);
        return semaphores.stream().mapToLong(Long::longValue).toArray();
    }

    private void awaitReusable() {
        if (submittedCommandBuffer != null) {
            submittedCommandBuffer.waitForFence();
        }
        awaitDlssGInputs();
        finalColor.awaitOwnedRelease();
        worldColor.awaitOwnedRelease();
        depth.awaitOwnedRelease();
        motionVector.awaitOwnedRelease();
        submittedCommandBuffer = null;
        fence = 0L;
    }
    public void setDlssGInputCompletion(long semaphore, long value) {
        if (semaphore == 0L || value <= 0L) {
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
        if (sealed) {
            throw new IllegalStateException("Frame capture is already sealed");
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
            int result = vkWaitSemaphores(RenderSystems.vulkan().device().getVkDevice(), waitInfo, Long.MAX_VALUE);
            if (result != VK_SUCCESS) {
                throw new IllegalStateException(
                        "Failed to wait for DLSS-G input completion, VkResult=" + result
                );
            }
        }
        clearDlssGInputCompletion();
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
