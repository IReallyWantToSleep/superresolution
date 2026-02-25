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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandPool;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanQueueUtils;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandPool implements ICommandPool {
    private final VulkanDevice device;
    private final EnumSet<CommandPoolFlags> flags;
    private final List<VulkanCommandBuffer> allocatedBuffers = new ArrayList<>();
    private final VulkanFencePool fencePool;
    private int graphicsQueueFamilyIndex;
    private long commandPool;
    private VkQueue graphicsQueue;

    public VulkanCommandPool(VulkanDevice device, EnumSet<CommandPoolFlags> flags) {
        this.device = device;
        this.flags = flags.clone();
        this.fencePool = new VulkanFencePool(this);
    }

    public void init() {
        this.graphicsQueueFamilyIndex = VulkanQueueUtils.findQueueFamilyIndex(VK_QUEUE_GRAPHICS_BIT, device);
        createCommandPool();
        createGraphicsQueue();
    }

    public List<VulkanCommandBuffer> getAllocatedBuffers() {
        return allocatedBuffers;
    }

    public VulkanDevice getDevice() {
        return device;
    }

    public VulkanFencePool getFencePool() {
        return fencePool;
    }

    public long getCommandPool() {
        return commandPool;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int poolFlags = 0;
            if (flags.contains(CommandPoolFlags.Reset)) {
                poolFlags |= VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
            }
            if (flags.contains(CommandPoolFlags.Transient)) {
                poolFlags |= VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
            }
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(graphicsQueueFamilyIndex)
                    .flags(poolFlags);

            LongBuffer pCommandPool = stack.mallocLong(1);
            VulkanUtils.VK_CHECK(vkCreateCommandPool(device.getVkDevice(), poolInfo, null, pCommandPool), "Failed to create command pool");
            commandPool = pCommandPool.get(0);
        }
    }

    private void createGraphicsQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device.getVkDevice(), graphicsQueueFamilyIndex, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device.getVkDevice());
        }
    }

    void freeCommandBuffer(VkCommandBuffer cmdBuf) {
        VK10.vkFreeCommandBuffers(
                device.getVkDevice(),
                commandPool,
                cmdBuf
        );
    }

    VkCommandBuffer createNativeCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            VulkanUtils.VK_CHECK(vkAllocateCommandBuffers(device.getVkDevice(), allocInfo, pCommandBuffer), "Failed to allocate command buffer");
            return new VkCommandBuffer(pCommandBuffer.get(0), device.getVkDevice());
        }
    }

    @Override
    public VulkanCommandBuffer createCommandBuffer() {
        return createCommandBuffer(CommandBufferBehavior.ReusableSequential);
    }

    @Override
    public VulkanCommandBuffer createCommandBuffer(CommandBufferBehavior behavior) {
        VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(device, this, behavior);
        allocatedBuffers.add(commandBuffer);
        return commandBuffer;
    }

    @Override
    public EnumSet<CommandPoolFlags> flags() {
        return flags.clone();
    }

    public void destroy() {
        for (VulkanCommandBuffer buffer : new ArrayList<>(allocatedBuffers)) {
            buffer.destroy();
        }
        allocatedBuffers.clear();
        fencePool.destroy();
        vkDestroyCommandPool(device.getVkDevice(), commandPool, null);
    }

    @Override
    public void reset() {
        for (VulkanCommandBuffer buffer : allocatedBuffers) {
            if (buffer.state() != io.homo.superresolution.core.graphics.impl.command.CommandBufferState.Destroyed) {
                buffer.reset();
            }
        }
    }

    void onCommandBufferDestroyed(VulkanCommandBuffer commandBuffer) {
        allocatedBuffers.remove(commandBuffer);
    }
}