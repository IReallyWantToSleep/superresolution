package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class ClearCommand extends GpuCommand {
    public int clearMode = 0; // 0-RGBA 1-Depth 2-Stencil
    public float[] colorRGBA = new float[]{0, 0, 0, 0};
    public float depth = 0;
    public int stencil = 0;

    public ITexture target;

    public ClearCommand(ITexture target) {
        this.target = target;
    }


    @Override
    public GpuCommandType getCommandType() {
        return GpuCommandType.Clear;
    }
}
