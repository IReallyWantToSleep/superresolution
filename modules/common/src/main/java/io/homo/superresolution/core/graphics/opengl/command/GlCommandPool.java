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

package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandPool;
import io.homo.superresolution.core.graphics.opengl.GlDevice;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GlCommandPool implements ICommandPool {
    private final GlDevice device;
    private final EnumSet<CommandPoolFlags> flags;
    private final List<GlCommandBuffer> buffers = new ArrayList<>();

    public GlCommandPool(GlDevice device, EnumSet<CommandPoolFlags> flags) {
        this.device = device;
        this.flags = flags.clone();
    }

    @Override
    public GlCommandBuffer createCommandBuffer() {
        return createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
    }

    @Override
    public GlCommandBuffer createCommandBuffer(CommandBufferBehavior behavior) {
        GlCommandBuffer commandBuffer = new GlCommandBuffer(device, this, behavior);
        if (behavior != CommandBufferBehavior.OneTimeSubmit) {
            buffers.add(commandBuffer);
        }
        return commandBuffer;
    }

    @Override
    public EnumSet<CommandPoolFlags> flags() {
        return flags.clone();
    }

    @Override
    public void destroy() {
        for (GlCommandBuffer buffer : buffers) {
            buffer.destroy();
        }
        buffers.clear();
    }

    @Override
    public void reset() {
        for (GlCommandBuffer buffer : buffers) {
            buffer.reset();
        }
    }
}
