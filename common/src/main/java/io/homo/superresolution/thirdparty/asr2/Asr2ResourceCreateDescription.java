package io.homo.superresolution.thirdparty.asr2;

import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.math.Vector2f;

public class Asr2ResourceCreateDescription {
    public Vector2f size;
    public TextureFormat format;
    public int dim;
    public String label;
    public int mipCount;

    public Asr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label) {
        this(size, format, dim, label, -1);
    }

    public Asr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label, int mipCount) {
        this.size = size;
        this.format = format;
        this.dim = dim;
        this.label = label;
        this.mipCount = mipCount;
    }
}
