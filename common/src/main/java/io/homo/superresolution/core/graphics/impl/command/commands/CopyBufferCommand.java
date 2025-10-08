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

package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import org.joml.Vector4i;

public class CopyBufferCommand extends GpuCommand {
    public long srcOffset = 0;
    public long dstOffset = 0;
    public long size = 0;
    public IBuffer source;
    public IBuffer destination;

    public CopyBufferCommand(IBuffer source, IBuffer destination) {
        this.source = source;
        this.destination = destination;
    }


    @Override
    public GpuCommandType getCommandType() {
        return GpuCommandType.CopyBuffer;
    }
}
