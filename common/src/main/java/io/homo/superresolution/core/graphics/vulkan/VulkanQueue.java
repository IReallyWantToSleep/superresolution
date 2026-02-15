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

import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;

public class VulkanQueue {
    private final VulkanDevice device;
    private final int queueFamilyIndex;
    private VkQueue queue;

    public VulkanQueue(VulkanDevice device, int queueFamilyIndex) {
        this.device = device;
        this.queueFamilyIndex = queueFamilyIndex;
        getDeviceQueue();
    }

    public int getQueueFamilyIndex() {
        return queueFamilyIndex;
    }

    private void getDeviceQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device.getVkDevice(), queueFamilyIndex, 0, pQueue);
            queue = new VkQueue(pQueue.get(0), device.getVkDevice());
        }
    }

    public VkQueue getQueue() {
        return queue;
    }
}
