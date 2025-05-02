package io.homo.superresolution.core.impl.texture;

import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;

public interface ITexture extends Destroyable, Resizable {
    int getTextureId();

    TextureFormat getTextureFormat();

    int getWidth();

    int getHeight();

    default String string() {
        return "ITexture{" +
                "id=" + getTextureId() +
                "format=" + getTextureFormat() +
                "width=" + getWidth() +
                "height=" + getHeight() +
                '}';
    }
}
