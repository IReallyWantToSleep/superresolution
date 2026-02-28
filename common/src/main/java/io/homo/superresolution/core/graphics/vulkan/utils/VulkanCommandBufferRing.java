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

import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

public class VulkanCommandBufferRing {
    private final int bufferCount;
    private final VulkanCommandBuffer[] commandBuffers;
    private boolean initialized;
    private int cursor = 0;

    public VulkanCommandBufferRing(int bufferCount) {
        if (bufferCount <= 0) {
            throw new IllegalArgumentException("initialBufferCount must be greater than 0");
        }
        this.bufferCount = bufferCount;
        this.commandBuffers = new VulkanCommandBuffer[bufferCount];
        initialized = false;
    }

    public VulkanCommandBuffer acquire(VulkanDevice device) {
        if (!initialized) {
            for (int i = 0; i < bufferCount; i++) {
                commandBuffers[i] = (VulkanCommandBuffer) device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.ReusableSequential);
            }
            initialized = true;
        }

        VulkanCommandBuffer commandBuffer = commandBuffers[cursor];
        commandBuffer.waitForFence();
        cursor = (cursor + 1) % bufferCount;
        return commandBuffer;
    }

    public void destroy() {
        for (int i = 0; i < bufferCount; i++) {
            VulkanCommandBuffer commandBuffer = commandBuffers[i];
            if (commandBuffer == null) {
                continue;
            }
            try {
                commandBuffer.destroy();
            } catch (IllegalStateException ignored) {
            }
            commandBuffers[i] = null;
        }
        cursor = 0;
        initialized = false;
    }
}
