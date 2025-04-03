package io.homo.superresolution.common.render.impl.texture;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.impl.IDebuggableObject;

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
