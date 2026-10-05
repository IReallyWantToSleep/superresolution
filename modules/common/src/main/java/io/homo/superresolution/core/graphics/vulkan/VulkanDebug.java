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

import io.homo.superresolution.common.config.SuperResolutionConfig;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDebugUtilsLabelEXT;
import org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class VulkanDebug {
    private static final VulkanDebugBackend DEBUG_IMPL = new DebugImpl();
    private static final VulkanDebugBackend NO_OP_IMPL = new NoOpImpl();
    private static volatile VulkanDebugBackend backend = SuperResolutionConfig.isEnableDebug() ? DEBUG_IMPL : NO_OP_IMPL;

    public static void setObjectName(VkDevice device, int objectType, long handle, String label) {
        backend.setObjectName(device, objectType, handle, label);
    }

    public static void beginLabel(VkCommandBuffer commandBuffer, String name) {
        backend.beginLabel(commandBuffer, name);
    }

    public static void endLabel(VkCommandBuffer commandBuffer) {
        backend.endLabel(commandBuffer);
    }

    public static void insertLabel(VkCommandBuffer commandBuffer, String name) {
        backend.insertLabel(commandBuffer, name);
    }

    public static void refreshFromConfig() {
        setEnabled(SuperResolutionConfig.isEnableDebug());
    }

    public static boolean isEnabled() {
        return backend == DEBUG_IMPL;
    }

    public static void setEnabled(boolean enabled) {
        backend = enabled ? DEBUG_IMPL : NO_OP_IMPL;
    }

    private static String sanitizeLabel(String label) {
        String sanitized = label == null || label.isBlank() ? "Unnamed" : label;
        return StringUtils.abbreviate(sanitized, 255);
    }

    private interface VulkanDebugBackend {
        void setObjectName(VkDevice device, int objectType, long handle, String label);

        void beginLabel(VkCommandBuffer commandBuffer, String name);

        void endLabel(VkCommandBuffer commandBuffer);

        void insertLabel(VkCommandBuffer commandBuffer, String name);
    }

    private static final class NoOpImpl implements VulkanDebugBackend {
        @Override
        public void setObjectName(VkDevice device, int objectType, long handle, String label) {
        }

        @Override
        public void beginLabel(VkCommandBuffer commandBuffer, String name) {
        }

        @Override
        public void endLabel(VkCommandBuffer commandBuffer) {
        }

        @Override
        public void insertLabel(VkCommandBuffer commandBuffer, String name) {
        }
    }

    private static final class DebugImpl implements VulkanDebugBackend {
        @Override
        public void setObjectName(VkDevice device, int objectType, long handle, String label) {
            if (device == null || handle == 0 || handle == VK_NULL_HANDLE || device.getCapabilitiesInstance().vkSetDebugUtilsObjectNameEXT == 0L) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDebugUtilsObjectNameInfoEXT nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT)
                        .objectType(objectType)
                        .objectHandle(handle)
                        .pObjectName(stack.UTF8(sanitizeLabel(label)));
                EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, nameInfo);
            }
        }

        @Override
        public void beginLabel(VkCommandBuffer commandBuffer, String name) {
            if (commandBuffer == null || commandBuffer.getCapabilitiesInstance().vkCmdBeginDebugUtilsLabelEXT == 0L) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDebugUtilsLabelEXT label = createLabel(stack, name);
                EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(commandBuffer, label);
            }
        }

        @Override
        public void endLabel(VkCommandBuffer commandBuffer) {
            if (commandBuffer == null || commandBuffer.getCapabilitiesInstance().vkCmdEndDebugUtilsLabelEXT == 0L) {
                return;
            }
            EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        }

        @Override
        public void insertLabel(VkCommandBuffer commandBuffer, String name) {
            if (commandBuffer == null || commandBuffer.getCapabilitiesInstance().vkCmdInsertDebugUtilsLabelEXT == 0L) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDebugUtilsLabelEXT label = createLabel(stack, name);
                EXTDebugUtils.vkCmdInsertDebugUtilsLabelEXT(commandBuffer, label);
            }
        }

        private VkDebugUtilsLabelEXT createLabel(MemoryStack stack, String name) {
            VkDebugUtilsLabelEXT label = VkDebugUtilsLabelEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT)
                    .pLabelName(stack.UTF8(sanitizeLabel(name)));
            label.color(0, 0.2f);
            label.color(1, 0.6f);
            label.color(2, 1.0f);
            label.color(3, 1.0f);
            return label;
        }
    }
}
