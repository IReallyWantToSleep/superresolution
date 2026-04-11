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

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanBuffer implements IBuffer {
    private final VulkanDevice device;
    private final long size;
    private final BufferUsage usage;
    private long buffer = VK_NULL_HANDLE;
    private long memory = VK_NULL_HANDLE;
    private IBufferData bufferData;

    public VulkanBuffer(VulkanDevice device, BufferDescription description) {
        this.device = device;
        this.size = description.size();
        this.usage = description.usage();

        try (MemoryStack stack = stackPush()) {
            createBuffer(stack);
            allocateMemory(stack);
        }
    }

    private void createBuffer(MemoryStack stack) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(translateUsage(usage))
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer pBuffer = stack.mallocLong(1);
        VK_CHECK(vkCreateBuffer(device.getVkDevice(), bufferInfo, null, pBuffer),
                "Failed to create buffer");
        buffer = pBuffer.get(0);
    }

    private void allocateMemory(MemoryStack stack) {
        VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
        vkGetBufferMemoryRequirements(device.getVkDevice(), buffer, memReqs);

        int memoryProperties = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReqs.size())
                .memoryTypeIndex(findMemoryType(memReqs.memoryTypeBits(), memoryProperties));

        LongBuffer pMemory = stack.mallocLong(1);
        VK_CHECK(vkAllocateMemory(device.getVkDevice(), allocInfo, null, pMemory),
                "Failed to allocate buffer memory");
        memory = pMemory.get(0);

        VK_CHECK(vkBindBufferMemory(device.getVkDevice(), buffer, memory, 0),
                "Failed to bind buffer memory");
    }

    private int findMemoryType(int typeFilter, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 &&
                        (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new VulkanException("Failed to find suitable memory type for buffer");
    }

    private static int translateUsage(BufferUsage usage) {
        return switch (usage) {
            case Ubo -> VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
            case StaticDraw, DynamicDraw -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
            case CopySrc -> VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            case CopyDst -> VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        };
    }

    @Override
    public IBufferData data() {
        return bufferData;
    }

    @Override
    public void upload() {
        if (bufferData == null) {
            throw new IllegalStateException("No buffer data to upload");
        }

        ByteBuffer src = bufferData.asByteBuffer();
        long uploadSize = Math.min(src.remaining(), size);

        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            VK_CHECK(vkMapMemory(device.getVkDevice(), memory, 0, uploadSize, 0, ppData),
                    "Failed to map buffer memory");
            long mappedPtr = ppData.get(0);
            MemoryUtil.memCopy(MemoryUtil.memAddress(src), mappedPtr, uploadSize);
            vkUnmapMemory(device.getVkDevice(), memory);
        }
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public BufferUsage getUsage() {
        return usage;
    }

    @Override
    public void setBufferData(IBufferData bufferData) {
        this.bufferData = bufferData;
    }

    @Override
    public long handle() {
        return buffer;
    }

    @Override
    public void destroy() {
        if (buffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(device.getVkDevice(), buffer, null);
            buffer = VK_NULL_HANDLE;
        }
        if (memory != VK_NULL_HANDLE) {
            vkFreeMemory(device.getVkDevice(), memory, null);
            memory = VK_NULL_HANDLE;
        }
    }
}
