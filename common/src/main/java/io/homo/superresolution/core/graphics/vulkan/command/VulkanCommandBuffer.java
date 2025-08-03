package io.homo.superresolution.core.graphics.vulkan.command;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.command.commands.GpuCommand;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandDecoder;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VulkanCommandBuffer implements ICommandBuffer {
    private final List<GpuCommand> gpuCommands = new ArrayList<>();
    private VulkanDevice vulkanDevice;

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
        VK10.vkEndCommandBuffer(nativeCommandBuffer);
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
        return vulkanDevice.commendEncoder();
    }
}
