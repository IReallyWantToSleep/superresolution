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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffer implements IVertexBuffer {
    private final VulkanDevice device;
    private final VulkanMemoryAllocator allocator;
    private final int sizeInBytes;
    private final boolean dynamic;
    private final VertexFormat vertexFormat;
    private long buffer = VK_NULL_HANDLE;
    private long vmaAllocation;
    private ByteBuffer mappedBuffer = null;
    private boolean isMapped = false;

    public VulkanVertexBuffer(VulkanDevice device, VertexBufferDescription description) {
        this.device = device;
        this.allocator = device.getMemoryAllocator();
        this.sizeInBytes = description.getSizeInBytes();
        this.dynamic = description.isDynamic();
        this.vertexFormat = description.getVertexFormat();

        createBufferVma();
    }

    private void createBufferVma() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(sizeInBytes)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VmaAllocationCreateInfo allocCI = VulkanMemoryAllocator.buildAllocCreateInfo(
                    stack,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            );

            PointerBuffer pAllocation = stack.mallocPointer(1);
            buffer = allocator.createBufferVma(bufferInfo, allocCI, pAllocation);
            vmaAllocation = pAllocation.get(0);
            device.setDebugName(VK_OBJECT_TYPE_BUFFER, buffer, "VulkanVertexBuffer size=" + sizeInBytes + " dynamic=" + dynamic + " format=" + vertexFormat);
        }
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
        mappedBuffer = allocator.mapMemory(vmaAllocation, offsetInBytes, lengthInBytes);
        isMapped = true;
        return mappedBuffer;
    }

    @Override
    public void unmap() {
        if (!isMapped) {
            throw new IllegalStateException("Vertex buffer is not mapped");
        }
        allocator.unmapMemory(vmaAllocation);
        mappedBuffer = null;
        isMapped = false;
    }

    @Override
    public void updateData(ByteBuffer data, int offsetInBytes) {
        int length = data.remaining();
        ByteBuffer mapped = allocator.mapMemory(vmaAllocation, offsetInBytes, length);
        try {
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress(mapped), length);
        } finally {
            allocator.unmapMemory(vmaAllocation);
        }
    }

    @Override
    public void updateData(byte[] data, int offsetInBytes, int lengthInBytes) {
        ByteBuffer mapped = allocator.mapMemory(vmaAllocation, offsetInBytes, lengthInBytes);
        try {
            mapped.put(data, offsetInBytes, lengthInBytes);
        } finally {
            allocator.unmapMemory(vmaAllocation);
        }
    }

    @Override
    public void destroy() {
        if (isMapped) {
            unmap();
        }
        long bufferToDestroy = buffer;
        long allocationToDestroy = vmaAllocation;
        buffer = VK_NULL_HANDLE;
        vmaAllocation = 0;
        if (bufferToDestroy == VK_NULL_HANDLE || allocationToDestroy == 0) {
            return;
        }
        device.queueForDestroy(() -> allocator.freeBuffer(bufferToDestroy, allocationToDestroy));
    }
}
