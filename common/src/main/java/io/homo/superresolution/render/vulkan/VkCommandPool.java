package io.homo.superresolution.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkCommandPool {
    private final VkApplication application;
    public long commandPool;
    public ArrayList<VkCommandBuffer> commandBuffers;

    public VkCommandPool(VkApplication application) {
        this.application = application;
    }

    public void createCommandPool() {
        try(MemoryStack stack = stackPush()) {
            QueueFamilyIndices queueFamilyIndices = application.deviceManager.findQueueFamilies(application.deviceManager.physicalDevice);
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(application.deviceManager.device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = pCommandPool.get(0);
        }
    }

    public void createCommandBuffers() {
        final int commandBuffersCount = application.swapChainManager.swapChainFramebuffers.size();
        commandBuffers = new ArrayList<>(commandBuffersCount);
        try(MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);
            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);
            if(vkAllocateCommandBuffers(application.deviceManager.device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for(int i = 0;i < commandBuffersCount;i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), application.deviceManager.device));
            }
        }
    }
}
