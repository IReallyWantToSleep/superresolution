package io.homo.superresolution.core.graphics.vulkan.command;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandManager {
    private final VulkanDevice device;
    private final List<VkCommandBuffer> allocatedBuffers = new ArrayList<>();
    private int graphicsQueueFamilyIndex;
    private long commandPool;
    private VkQueue graphicsQueue;

    public VulkanCommandManager(VulkanDevice device) {
        this.device = device;
    }

    public void init() {
        this.graphicsQueueFamilyIndex = findGraphicsQueueFamilyIndex();
        createCommandPool();
        createGraphicsQueue();
    }

    public List<VkCommandBuffer> getAllocatedBuffers() {
        return allocatedBuffers;
    }

    public long getCommandPool() {
        return commandPool;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    private int findGraphicsQueueFamilyIndex() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDevice physicalDevice = device.getPhysicalDevice();
            int queueFamilyCount = stack.mallocInt(1).get(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.ints(queueFamilyCount), null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount, stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.ints(queueFamilyCount), queueFamilies);
            for (int i = 0; i < queueFamilies.capacity(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    return i;
                }
            }
            throw new VulkanException("No suitable queue family found");
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(graphicsQueueFamilyIndex)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

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

    public VkCommandBuffer createCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            VulkanUtils.VK_CHECK(vkAllocateCommandBuffers(device.getVkDevice(), allocInfo, pCommandBuffer), "Failed to allocate command buffer");
            VkCommandBuffer cmdBuf = new VkCommandBuffer(pCommandBuffer.get(0), device.getVkDevice());
            allocatedBuffers.add(cmdBuf);
            return cmdBuf;
        }
    }

    public void submitCommandBuffer(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));

            vkEndCommandBuffer(commandBuffer);
            VulkanUtils.VK_CHECK(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE), "Failed to submit command buffer");
            vkQueueWaitIdle(graphicsQueue);
        }
    }

    public void destroy() {
        vkDestroyCommandPool(device.getVkDevice(), commandPool, null);
    }
}