package io.homo.superresolution.core.graphics.impl.command;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.command.commands.GpuCommand;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandDecoder;

import java.util.Collection;

public interface ICommandBuffer {
    void addCommand(GlCommandDecoder decoder, GpuCommand command);

    Collection<GpuCommand> getCommands();

    void destroy();

    void submit(IDevice device);

    IDevice getDevice();

    ICommandDecoder getDecoder();

    ICommandEncoder getEncoder();

}
