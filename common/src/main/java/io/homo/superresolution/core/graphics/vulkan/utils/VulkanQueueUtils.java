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

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

public class VulkanQueueUtils {
    public static int findQueueFamilyIndex(MemoryStack stack, int queueType, VkPhysicalDevice physicalDevice) {
        int queueFamilyCount = stack.mallocInt(1).get(0);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.ints(queueFamilyCount), null);
        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount, stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.ints(queueFamilyCount), queueFamilies);
        for (int i = 0; i < queueFamilies.capacity(); i++) {
            if ((queueFamilies.get(i).queueFlags() & queueType) != 0) {
                return i;
            }
        }
        throw new VulkanException("No suitable queue family found");
    }

    public static int findQueueFamilyIndex(int queueType, VulkanDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return findQueueFamilyIndex(stack, queueType, device.getPhysicalDevice());
        }
    }

    public static int findQueueFamilyIndex(int queueType, VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return findQueueFamilyIndex(stack, queueType, physicalDevice);
        }
    }
}
