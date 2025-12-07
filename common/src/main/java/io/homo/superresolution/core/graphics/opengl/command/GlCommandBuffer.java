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
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlDevice;

import java.util.ArrayList;
import java.util.List;

public class GlCommandBuffer implements ICommandBuffer {
    private final List<Runnable> glCalls = new ArrayList<>();
    private final GlDevice device;

    public GlCommandBuffer(GlDevice device) {
        this.device = device;
    }

    protected void _addGlCalls(Runnable glCalls) {
        this.glCalls.add(glCalls);
    }

    @Override
    public void destroy() {
        glCalls.clear();
    }

    @Override
    public void submit(IDevice device) {
        GlDebug.pushGroup(GlDebug.nextCommandBufferId(), "GlCommandBuffer");
        for (Runnable call : glCalls) {
            call.run();
        }
        GlDebug.popGroup();
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    @Override
    public ICommandDecoder getDecoder() {
        return device.commandDecoder();
    }
}
