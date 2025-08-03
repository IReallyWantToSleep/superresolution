package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector4i;

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
