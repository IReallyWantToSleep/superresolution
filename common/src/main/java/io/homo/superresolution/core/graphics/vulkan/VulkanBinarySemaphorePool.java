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

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Fixed pool of binary Vulkan semaphores with explicit exclusive leases.
 * Releasing a lease means the caller has proved the prior signal/wait lifecycle is
 * complete; a ring cursor alone never makes a semaphore reusable.
 */
public final class VulkanBinarySemaphorePool implements AutoCloseable {
    private final VulkanDevice device;
    private final String debugLabel;
    private final long[] semaphores;
    private final BinarySemaphoreLeaseTracker leases;
    private volatile boolean closed;

    public VulkanBinarySemaphorePool(VulkanDevice device, String debugLabel, int capacity) {
        if (device == null) {
            throw new IllegalArgumentException("device cannot be null");
        }
        if (debugLabel == null || debugLabel.isBlank()) {
            throw new IllegalArgumentException("debugLabel cannot be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        this.device = device;
        this.debugLabel = debugLabel;
        this.semaphores = new long[capacity];
        this.leases = new BinarySemaphoreLeaseTracker(capacity);
        createSemaphores();
    }

    public Lease acquire() throws InterruptedException {
        return new Lease(leases.acquire());
    }

    public Lease tryAcquire() {
        BinarySemaphoreLeaseTracker.Token token = leases.tryAcquire();
        return token == null ? null : new Lease(token);
    }

    /**
     * Leases the semaphore permanently associated with a swapchain image/slot.
     */
    public Lease acquireSlot(int slot) {
        return new Lease(leases.acquireSlot(slot));
    }

    public int capacity() {
        return semaphores.length;
    }

    public int activeLeaseCount() {
        return leases.activeLeases();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        leases.close();
        for (int i = 0; i < semaphores.length; i++) {
            if (semaphores[i] != VK_NULL_HANDLE) {
                vkDestroySemaphore(device.getVkDevice(), semaphores[i], null);
                semaphores[i] = VK_NULL_HANDLE;
            }
        }
        closed = true;
    }

    private void createSemaphores() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            for (int i = 0; i < semaphores.length; i++) {
                LongBuffer pointer = stack.mallocLong(1);
                VK_CHECK(
                        vkCreateSemaphore(device.getVkDevice(), createInfo, null, pointer),
                        "Failed to create " + debugLabel + " binary semaphore " + i
                );
                semaphores[i] = pointer.get(0);
                device.setDebugName(
                        VK_OBJECT_TYPE_SEMAPHORE,
                        semaphores[i],
                        debugLabel + "[" + i + "] Binary"
                );
            }
        } catch (Throwable throwable) {
            for (long semaphore : semaphores) {
                if (semaphore != VK_NULL_HANDLE) {
                    vkDestroySemaphore(device.getVkDevice(), semaphore, null);
                }
            }
            throw throwable;
        }
    }

    public final class Lease implements AutoCloseable {
        private final BinarySemaphoreLeaseTracker.Token token;
        private boolean released;

        private Lease(BinarySemaphoreLeaseTracker.Token token) {
            this.token = token;
        }

        public int slot() {
            return token.slot();
        }

        public long semaphore() {
            if (released || closed) {
                throw new IllegalStateException("Binary semaphore lease is not active");
            }
            return semaphores[token.slot()];
        }

        @Override
        public void close() {
            if (released) {
                throw new IllegalStateException("Binary semaphore lease was already released");
            }
            leases.release(token);
            released = true;
        }
    }
}
