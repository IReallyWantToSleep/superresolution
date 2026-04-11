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

import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
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

public class VulkanVertexBuffer implements IVertexBuffer {
    private final VulkanDevice device;
    private final int sizeInBytes;
    private final boolean dynamic;
    private final VertexFormat vertexFormat;
    private long buffer = VK_NULL_HANDLE;
    private long memory = VK_NULL_HANDLE;
    private ByteBuffer mappedBuffer = null;
    private boolean isMapped = false;

    public VulkanVertexBuffer(VulkanDevice device, VertexBufferDescription description) {
        this.device = device;
        this.sizeInBytes = description.getSizeInBytes();
        this.dynamic = description.isDynamic();
        this.vertexFormat = description.getVertexFormat();

        try (MemoryStack stack = stackPush()) {
            createBuffer(stack);
            allocateMemory(stack);
        }
    }

    private void createBuffer(MemoryStack stack) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(sizeInBytes)
                .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer pBuffer = stack.mallocLong(1);
        VK_CHECK(vkCreateBuffer(device.getVkDevice(), bufferInfo, null, pBuffer),
                "Failed to create vertex buffer");
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
                "Failed to allocate vertex buffer memory");
        memory = pMemory.get(0);

        VK_CHECK(vkBindBufferMemory(device.getVkDevice(), buffer, memory, 0),
                "Failed to bind vertex buffer memory");
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
        throw new VulkanException("Failed to find suitable memory type for vertex buffer");
    }

    @Override
    public long handle() {
        return buffer;
    }

    @Override
    public int getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }

    @Override
    public ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write) {
        if (isMapped) {
            throw new IllegalStateException("Vertex buffer is already mapped");
        }
        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            VK_CHECK(vkMapMemory(device.getVkDevice(), memory, offsetInBytes, lengthInBytes, 0, ppData),
                    "Failed to map vertex buffer memory");
            mappedBuffer = MemoryUtil.memByteBuffer(ppData.get(0), lengthInBytes);
            isMapped = true;
            return mappedBuffer;
        }
    }

    @Override
    public void unmap() {
        if (!isMapped) {
            throw new IllegalStateException("Vertex buffer is not mapped");
        }
        vkUnmapMemory(device.getVkDevice(), memory);
        mappedBuffer = null;
        isMapped = false;
    }

    @Override
    public void updateData(ByteBuffer data, int offsetInBytes) {
        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            int length = data.remaining();
            VK_CHECK(vkMapMemory(device.getVkDevice(), memory, offsetInBytes, length, 0, ppData),
                    "Failed to map vertex buffer for update");
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), ppData.get(0), length);
            vkUnmapMemory(device.getVkDevice(), memory);
        }
    }

    @Override
    public void updateData(byte[] data, int offsetInBytes, int lengthInBytes) {
        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            VK_CHECK(vkMapMemory(device.getVkDevice(), memory, offsetInBytes, lengthInBytes, 0, ppData),
                    "Failed to map vertex buffer for update");
            long dst = ppData.get(0);
            ByteBuffer temp = MemoryUtil.memByteBuffer(dst, lengthInBytes);
            temp.put(data, offsetInBytes, lengthInBytes);
            vkUnmapMemory(device.getVkDevice(), memory);
        }
    }

    @Override
    public void destroy() {
        if (isMapped) {
            unmap();
        }
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
