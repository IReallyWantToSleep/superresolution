package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.command.commands.GpuCommand;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.GlState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlCommandBuffer implements ICommandBuffer {
    private final List<Runnable> glCalls = new ArrayList<>();
    private final List<GpuCommand> gpuCommands = new ArrayList<>();
    private final GlDevice device;

    public GlCommandBuffer(GlDevice device) {
        this.device = device;
    }

    @Override
    public void addCommand(GlCommandDecoder decoder, GpuCommand command) {
        this.gpuCommands.add(command);
        decoder.decodeCommand(this, command);
    }

    protected void _addGlCalls(Runnable glCalls) {
        this.glCalls.add(glCalls);
    }

    @Override
    public Collection<GpuCommand> getCommands() {
        return gpuCommands;
    }

    @Override
    public void destroy() {
        glCalls.clear();
        gpuCommands.clear();
    }

    @Override
    public void submit(IDevice device) {
        try (GlState state = new GlState(GlState.STATE_ALL)) {
            GlDebug.pushGroup(GlDebug.nextCommandBufferId(), "GlCommandBuffer");
            for (Runnable call : glCalls) {
                call.run();
            }
            GlDebug.popGroup();
        }
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    @Override
    public ICommandDecoder getDecoder() {
        return device.commandDecoder();
    }

    @Override
    public ICommandEncoder getEncoder() {
        return device.commendEncoder();
    }
}
