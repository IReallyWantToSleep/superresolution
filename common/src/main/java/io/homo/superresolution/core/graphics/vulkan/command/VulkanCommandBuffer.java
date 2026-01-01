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

package io.homo.superresolution.core.graphics.vulkan.command;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VkCommandBuffer;


public class VulkanCommandBuffer implements ICommandBuffer {
    private final VulkanDevice vulkanDevice;

    public VkCommandBuffer getNativeCommandBuffer() {
        return nativeCommandBuffer;
    }

    private VkCommandBuffer nativeCommandBuffer;

    public VulkanCommandBuffer(VulkanDevice vulkanDevice) {
        this.vulkanDevice = vulkanDevice;
        nativeCommandBuffer = vulkanDevice.getCommandManager().createCommandBuffer();
    }

    @Override
    public void destroy() {
        vulkanDevice.getCommandManager().freeCommandBuffer(nativeCommandBuffer);
        nativeCommandBuffer = null;
    }

    @Override
    public void submit(IDevice device) {
        device.submitCommandBuffer(this);
    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }

    @Override
    public ICommandDecoder getDecoder() {
        return vulkanDevice.commandDecoder();
    }
}
