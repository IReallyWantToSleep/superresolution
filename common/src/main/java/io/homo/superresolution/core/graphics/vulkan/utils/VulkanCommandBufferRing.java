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

package io.homo.superresolution.core.graphics.vulkan.utils;

import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

public class VulkanCommandBufferRing {
    private final int initialBufferCount;
    private final int maxBufferCount;
    private final List<VulkanCommandBuffer> commandBuffers = new ArrayList<>();
    private int cursor = 0;

    public VulkanCommandBufferRing(int initialBufferCount, int maxBufferCount) {
        if (initialBufferCount <= 0) {
            throw new IllegalArgumentException("initialBufferCount must be greater than 0");
        }
        if (maxBufferCount < initialBufferCount) {
            throw new IllegalArgumentException("maxBufferCount must be greater than or equal to initialBufferCount");
        }
        this.initialBufferCount = initialBufferCount;
        this.maxBufferCount = maxBufferCount;
    }

    private VulkanCommandBuffer allocateCommandBuffer(VulkanDevice device) {
        VulkanCommandBuffer commandBuffer = (VulkanCommandBuffer) device.defaultCommandPool().createCommandBuffer();
        commandBuffers.add(commandBuffer);
        return commandBuffer;
    }

    public VulkanCommandBuffer acquire(VulkanDevice device) {
        if (commandBuffers.isEmpty()) {
            for (int i = 0; i < initialBufferCount; i++) {
                allocateCommandBuffer(device);
            }
        }

        for (int i = 0; i < commandBuffers.size(); i++) {
            int index = (cursor + i) % commandBuffers.size();
            VulkanCommandBuffer commandBuffer = commandBuffers.get(index);
            if (!commandBuffer.isFenceSignaled()) {
                continue;
            }
            try {
                commandBuffer.reset();
                cursor = (index + 1) % commandBuffers.size();
                return commandBuffer;
            } catch (IllegalStateException ignored) {
            }
        }

        if (commandBuffers.size() < maxBufferCount) {
            VulkanCommandBuffer commandBuffer = allocateCommandBuffer(device);
            cursor = 0;
            return commandBuffer;
        }

        vkQueueWaitIdle(device.getMainQueue().getQueue());
        for (VulkanCommandBuffer commandBuffer : commandBuffers) {
            commandBuffer.reset();
        }
        VulkanCommandBuffer commandBuffer = commandBuffers.get(0);
        cursor = commandBuffers.size() > 1 ? 1 : 0;
        return commandBuffer;
    }

    public void destroy() {
        for (VulkanCommandBuffer commandBuffer : commandBuffers) {
            try {
                commandBuffer.destroy();
            } catch (IllegalStateException ignored) {
            }
        }
        commandBuffers.clear();
        cursor = 0;
    }
}
