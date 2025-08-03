package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector4i;

public class CopyTextureCommand extends GpuCommand {
    public Vector4i sourceDimensions = null;
    public Vector4i destinationDimensions = null;
    public int sourceLevel = 0;
    public int destinationLevel = 0;

    public ITexture source;
    public ITexture destination;

    public CopyTextureCommand(ITexture source, ITexture destination) {
        this.source = source;
        this.destination = destination;
    }


    @Override
    public GpuCommandType getCommandType() {
        return GpuCommandType.CopyTexture;
    }
}
