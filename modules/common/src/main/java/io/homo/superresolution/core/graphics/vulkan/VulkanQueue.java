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

package io.homo.superresolution.core.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.VK_OBJECT_TYPE_QUEUE;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

public class VulkanQueue {
    private static final ConcurrentMap<Long, Object> SUBMIT_LOCKS = new ConcurrentHashMap<>();
    private final VulkanDevice device;
    private final int queueFamilyIndex;
    private final int queueIndex;
    private final VulkanQueueRole role;
    // Guards every vkQueueSubmit/vkQueuePresentKHR/vkQueueWaitIdle on this queue;
    // required because the frame-generation pacer presents from its own thread.
    private Object submitLock;
    private VkQueue queue;

    public VulkanQueue(VulkanDevice device, int queueFamilyIndex) {
        this(device, queueFamilyIndex, 0, VulkanQueueRole.MAIN);
    }

    public VulkanQueue(
            VulkanDevice device,
            int queueFamilyIndex,
            int queueIndex,
            VulkanQueueRole role
    ) {
        this.device = device;
        this.queueFamilyIndex = queueFamilyIndex;
        this.queueIndex = queueIndex;
        this.role = role;
        getDeviceQueue();
    }

    public VulkanQueue(
            VulkanDevice device,
            VkQueue queue,
            int queueFamilyIndex,
            int queueIndex,
            VulkanQueueRole role
    ) {
        this.device = device;
        this.queueFamilyIndex = queueFamilyIndex;
        this.queueIndex = queueIndex;
        this.role = role;
        this.queue = queue;
        this.submitLock = submissionLock(queue);
    }

    void adoptQueue(VkQueue queue) {
        if (queue == null || queue.address() == 0L) {
            throw new IllegalArgumentException("Vulkan queue must be non-null");
        }
        this.queue = queue;
        this.submitLock = submissionLock(queue);
    }

    public int getQueueFamilyIndex() {
        return queueFamilyIndex;
    }

    public int getQueueIndex() {
        return queueIndex;
    }

    public VulkanQueueRole role() {
        return role;
    }

    private void getDeviceQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device.getVkDevice(), queueFamilyIndex, queueIndex, pQueue);
            queue = new VkQueue(pQueue.get(0), device.getVkDevice());
            if (queue.address() == 0L) {
                throw new IllegalStateException(
                        "Vulkan returned a null " + role + " queue for family "
                                + queueFamilyIndex + " index " + queueIndex
                );
            }
            submitLock = submissionLock(queue);
            device.setDebugName(
                    VK_OBJECT_TYPE_QUEUE,
                    queue.address(),
                    role.debugLabel() + " Queue family=" + queueFamilyIndex
                            + " index=" + queueIndex
            );
        }
    }

    public VkQueue getQueue() {
        return queue;
    }

    public Object submitLock() {
        return submitLock;
    }

    public static Object submissionLock(VkQueue queue) {
        if (queue == null || queue.address() == 0L) {
            throw new IllegalArgumentException("Vulkan queue must be non-null");
        }
        return SUBMIT_LOCKS.computeIfAbsent(queue.address(), ignored -> new Object());
    }

    public void waitIdle() {
        synchronized (submitLock) {
            VK_CHECK(vkQueueWaitIdle(queue), "Failed to wait for queue idle");
        }
    }
}
