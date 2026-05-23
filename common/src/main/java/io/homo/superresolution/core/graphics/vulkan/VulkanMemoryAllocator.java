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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class VulkanMemoryAllocator {

    private final VulkanDevice device;
    private long vmaAllocator;

    public VulkanMemoryAllocator(VulkanDevice device) {
        this.device = device;
        initVma();
    }

    private void initVma() {
        try (MemoryStack stack = stackPush()) {
            VmaAllocatorCreateInfo createInfo = VmaAllocatorCreateInfo.calloc(stack);
            VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(stack);
            vulkanFunctions.set(
                    device.getVkInstance(),
                    device.getVkDevice()
            );
            createInfo.flags(Vma.VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT
                    | Vma.VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT);
            createInfo.physicalDevice(device.getPhysicalDevice());
            createInfo.device(device.getVkDevice());
            createInfo.instance(device.getVkInstance());
            createInfo.vulkanApiVersion(VK_API_VERSION_1_2);
            createInfo.pVulkanFunctions(vulkanFunctions);

            PointerBuffer pAllocator = stack.mallocPointer(1);
            int result = Vma.vmaCreateAllocator(createInfo, pAllocator);
            if (result != VK_SUCCESS) {
                throw new VulkanException("Failed to create VMA allocator: VkResult=" + result);
            }
            vmaAllocator = pAllocator.get(0);
        }
    }
    public long allocateBufferMemory(long buffer, int requiredMemoryProperties) {
        try (MemoryStack stack = stackPush()) {
            VmaAllocationCreateInfo allocCI = buildBufferAllocCreateInfo(stack, requiredMemoryProperties);

            PointerBuffer pAllocation = stack.mallocPointer(1);
            int result = Vma.vmaAllocateMemoryForBuffer(vmaAllocator, buffer, allocCI, pAllocation, null);
            VK_CHECK(result, "VMA failed to allocate buffer memory");
            long vmaAlloc = pAllocation.get(0);

            VK_CHECK(Vma.vmaBindBufferMemory(vmaAllocator, vmaAlloc, buffer),
                    "VMA failed to bind buffer memory");

            return vmaAlloc;
        }
    }

    public void freeBuffer(long buffer, long vmaAllocation) {
        if (vmaAllocation != NULL && vmaAllocator != NULL) {
            Vma.vmaDestroyBuffer(vmaAllocator, buffer, vmaAllocation);
        } else {
            if (buffer != VK_NULL_HANDLE) {
                vkDestroyBuffer(device.getVkDevice(), buffer, null);
            }
            if (vmaAllocation != NULL) {
                Vma.vmaFreeMemory(vmaAllocator, vmaAllocation);
            }
        }
    }

    public long allocateImageMemoryVma(long image, int requiredMemoryProperties) {
        try (MemoryStack stack = stackPush()) {
            VmaAllocationCreateInfo allocCI = VmaAllocationCreateInfo.calloc(stack);
            allocCI.usage(Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);
            allocCI.flags(Vma.VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT);
            allocCI.requiredFlags(requiredMemoryProperties);

            PointerBuffer pAllocation = stack.mallocPointer(1);
            int result = Vma.vmaAllocateMemoryForImage(vmaAllocator, image, allocCI, pAllocation, null);
            VK_CHECK(result, "VMA failed to allocate image memory");
            long vmaAlloc = pAllocation.get(0);

            VK_CHECK(Vma.vmaBindImageMemory(vmaAllocator, vmaAlloc, image),
                    "VMA failed to bind image memory");

            return vmaAlloc;
        }
    }

    public long getDeviceMemoryFromAllocation(long vmaAllocation) {
        try (MemoryStack stack = stackPush()) {
            VmaAllocationInfo allocInfo = VmaAllocationInfo.calloc(stack);
            Vma.vmaGetAllocationInfo(vmaAllocator, vmaAllocation, allocInfo);
            return allocInfo.deviceMemory();
        }
    }

    public void freeImageVma(long image, long vmaAllocation) {
        if (vmaAllocation != NULL && vmaAllocator != NULL) {
            Vma.vmaDestroyImage(vmaAllocator, image, vmaAllocation);
        } else {
            if (image != VK_NULL_HANDLE) {
                vkDestroyImage(device.getVkDevice(), image, null);
            }
            if (vmaAllocation != NULL) {
                Vma.vmaFreeMemory(vmaAllocator, vmaAllocation);
            }
        }
    }

    public long allocateImageMemory(long image, int requiredMemoryProperties, long pNext) {
        try (MemoryStack stack = stackPush()) {
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.getVkDevice(), image, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(memReqs.memoryTypeBits(), requiredMemoryProperties));

            if (pNext != 0) {
                allocInfo.pNext(pNext);
            }

            LongBuffer pMemory = stack.mallocLong(1);
            VK_CHECK(vkAllocateMemory(device.getVkDevice(), allocInfo, null, pMemory),
                    "Failed to allocate image memory");
            long memory = pMemory.get(0);

            VK_CHECK(vkBindImageMemory(device.getVkDevice(), image, memory, 0),
                    "Failed to bind image memory");

            return memory;
        }
    }

    public long getImageMemoryRequirements(long image) {
        try (MemoryStack stack = stackPush()) {
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.getVkDevice(), image, memReqs);
            return memReqs.size();
        }
    }

    public void freeImage(long image, long memory) {
        if (image != VK_NULL_HANDLE) {
            vkDestroyImage(device.getVkDevice(), image, null);
        }
        if (memory != VK_NULL_HANDLE) {
            vkFreeMemory(device.getVkDevice(), memory, null);
        }
    }

    public ByteBuffer mapMemory(long vmaAllocation, long offset, long size) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer ppData = stack.mallocPointer(1);
            VK_CHECK(Vma.vmaMapMemory(vmaAllocator, vmaAllocation, ppData),
                    "VMA failed to map memory");
            long ptr = ppData.get(0);
            return memByteBuffer(ptr + offset, (int) size);
        }
    }

    public void unmapMemory(long vmaAllocation) {
        Vma.vmaUnmapMemory(vmaAllocator, vmaAllocation);
    }

    public int findMemoryType(int typeFilter, int requiredProperties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0
                        && (memProperties.memoryTypes(i).propertyFlags() & requiredProperties) == requiredProperties) {
                    return i;
                }
            }
        }
        throw new VulkanException("Failed to find suitable memory type");
    }

    public void destroy() {
        if (vmaAllocator != NULL) {
            Vma.vmaDestroyAllocator(vmaAllocator);
            vmaAllocator = NULL;
        }
    }

    private static VmaAllocationCreateInfo buildBufferAllocCreateInfo(MemoryStack stack, int requiredFlags) {
        VmaAllocationCreateInfo ci = VmaAllocationCreateInfo.calloc(stack);

        boolean hostVisible = (requiredFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0;
        boolean deviceLocal = (requiredFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0;

        if (hostVisible) {
            ci.usage(Vma.VMA_MEMORY_USAGE_AUTO);
            ci.flags(Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);
        } else if (deviceLocal) {
            ci.usage(Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);
        } else {
            ci.usage(Vma.VMA_MEMORY_USAGE_AUTO);
        }

        return ci;
    }
}
