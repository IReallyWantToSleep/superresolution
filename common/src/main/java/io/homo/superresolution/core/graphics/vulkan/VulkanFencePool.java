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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanFencePool {
    private final VulkanCommandPool commandPool;
    private final List<Long> allocatedFences = new ArrayList<>();

    public VulkanFencePool(VulkanCommandPool commandPool) {
        this.commandPool = commandPool;
    }

    public long createFence() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo createInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(0);
            LongBuffer pFence = stack.mallocLong(1);
            VK_CHECK(vkCreateFence(commandPool.getDevice().getVkDevice(), createInfo, null, pFence), "Failed to create fence");
            long fence = pFence.get(0);
            allocatedFences.add(fence);
            return fence;
        }
    }

    public void destroyFence(long fence) {
        if (fence == VK_NULL_HANDLE) {
            return;
        }
        vkDestroyFence(commandPool.getDevice().getVkDevice(), fence, null);
        allocatedFences.remove(fence);
    }

    public void destroy() {
        for (long fence : new ArrayList<>(allocatedFences)) {
            vkDestroyFence(commandPool.getDevice().getVkDevice(), fence, null);
        }
        allocatedFences.clear();
    }
}
