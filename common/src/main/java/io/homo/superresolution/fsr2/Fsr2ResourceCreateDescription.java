package io.homo.superresolution.fsr2;

import io.homo.superresolution.core.impl.Vec2;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

public class Fsr2ResourceCreateDescription {
    public Vec2 size;
    public TextureFormat format;
    public int dim;
    public String label;
    public int mipCount;

    public Fsr2ResourceCreateDescription(Vec2 size, TextureFormat format, int dim, String label) {
        this(size, format, dim, label, -1);
    }

    public Fsr2ResourceCreateDescription(Vec2 size, TextureFormat format, int dim, String label, int mipCount) {
        this.size = size;
        this.format = format;
        this.dim = dim;
        this.label = label;
        this.mipCount = mipCount;
    }
}
