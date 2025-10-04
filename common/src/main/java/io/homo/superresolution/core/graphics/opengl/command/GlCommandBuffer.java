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
        //try (GlState state = new GlState(
        //        GlState.STATE_ALL
        //)) {
        GlDebug.pushGroup(GlDebug.nextCommandBufferId(), "GlCommandBuffer");
        for (Runnable call : glCalls) {
            call.run();
        }
        GlDebug.popGroup();
        //}
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
        return device.commandEncoder();
    }
}
