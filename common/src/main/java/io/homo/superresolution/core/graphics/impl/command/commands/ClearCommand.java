package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class ClearCommand {
    public boolean clearDepth = false;
    public float[] colorRGBA = new float[]{0, 0, 0, 0};
    public float depth = 0;
    public int stencil = 0;

    public ITexture target;

    public ClearCommand(ITexture target) {
        this.target = target;
    }
}
