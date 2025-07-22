package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

public class Fsr2ResourceCreateDescription {
    public Vector2f size;
    public TextureFormat format;
    public int dim;
    public String label;
    public int mipCount;

    public Fsr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label) {
        this(size, format, dim, label, -1);
    }

    public Fsr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label, int mipCount) {
        this.size = size;
        this.format = format;
        this.dim = dim;
        this.label = label;
        this.mipCount = mipCount;
    }
}
