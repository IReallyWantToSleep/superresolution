/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.command.commands.GpuCommand;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandDecoder;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VulkanCommandBuffer implements ICommandBuffer {
    private final List<GpuCommand> gpuCommands = new ArrayList<>();
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
    public void addCommand(GlCommandDecoder decoder, GpuCommand command) {
        gpuCommands.add(command);
        vulkanDevice.commandDecoder().decodeCommand(this, command);
    }

    @Override
    public Collection<GpuCommand> getCommands() {
        return gpuCommands;
    }

    @Override
    public void destroy() {
        gpuCommands.clear();
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

    @Override
    public ICommandEncoder getEncoder() {
        return vulkanDevice.commandEncoder();
    }
}
