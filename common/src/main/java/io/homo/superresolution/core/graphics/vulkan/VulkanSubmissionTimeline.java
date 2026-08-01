/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreSignalInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;

/**
 * Host-signaled timeline whose values mean only that a queue submission was issued.
 * GPU completion remains represented by fences or binary semaphores owned elsewhere.
 */
public final class VulkanSubmissionTimeline implements AutoCloseable {
    private final VulkanDevice device;
    private final SubmissionTicketSequence tickets = new SubmissionTicketSequence();
    private final Object signalLock = new Object();
    private long semaphore;

    VulkanSubmissionTimeline(VulkanDevice device, String debugLabel) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo typeCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
                    .initialValue(0L);
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(typeCreateInfo.address());
            LongBuffer pointer = stack.mallocLong(1);
            VK_CHECK(
                    vkCreateSemaphore(device.getVkDevice(), createInfo, null, pointer),
                    "Failed to create submission-issued timeline semaphore"
            );
            semaphore = pointer.get(0);
            device.setDebugName(VK_OBJECT_TYPE_SEMAPHORE, semaphore, debugLabel);
        }
    }

    /**
     * Publishes the next ticket after {@code vkQueueSubmit} has returned successfully.
     */
    public long publishSubmissionIssued() {
        synchronized (signalLock) {
            requireOpen();
            long ticket = tickets.next();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSemaphoreSignalInfo signalInfo = VkSemaphoreSignalInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
                        .semaphore(semaphore)
                        .value(ticket);
                VK_CHECK(
                        vkSignalSemaphore(device.getVkDevice(), signalInfo),
                        "Failed to publish Vulkan submission ticket " + ticket
                );
            }
            return ticket;
        }
    }

    public long currentValue() {
        requireOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer value = stack.mallocLong(1);
            VK_CHECK(
                    vkGetSemaphoreCounterValue(device.getVkDevice(), semaphore, value),
                    "Failed to query the submission-issued timeline"
            );
            return value.get(0);
        }
    }

    public boolean isIssued(long ticket) {
        requireValidTicket(ticket);
        return currentValue() >= ticket;
    }

    public void awaitIssued(long ticket) {
        awaitIssued(ticket, Long.MAX_VALUE);
    }

    public void awaitIssued(long ticket, long timeoutNanos) {
        requireValidTicket(ticket);
        if (timeoutNanos < 0L) {
            throw new IllegalArgumentException("timeoutNanos cannot be negative");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .semaphoreCount(1)
                    .pSemaphores(stack.longs(semaphore))
                    .pValues(stack.longs(ticket));
            VK_CHECK(
                    vkWaitSemaphores(device.getVkDevice(), waitInfo, timeoutNanos),
                    "Failed to wait for Vulkan submission ticket " + ticket
            );
        }
    }

    public long handle() {
        requireOpen();
        return semaphore;
    }

    @Override
    public void close() {
        synchronized (signalLock) {
            if (semaphore == VK_NULL_HANDLE) {
                return;
            }
            vkDestroySemaphore(device.getVkDevice(), semaphore, null);
            semaphore = VK_NULL_HANDLE;
        }
    }

    private void requireValidTicket(long ticket) {
        requireOpen();
        if (ticket <= 0L || ticket > tickets.current()) {
            throw new IllegalArgumentException("Unknown Vulkan submission ticket " + ticket);
        }
    }

    private void requireOpen() {
        if (semaphore == VK_NULL_HANDLE) {
            throw new IllegalStateException("Vulkan submission timeline is closed");
        }
    }
}
