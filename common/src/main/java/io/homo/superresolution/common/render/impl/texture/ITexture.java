package io.homo.superresolution.common.render.impl.texture;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;

public interface ITexture extends Destroyable, Resizable {
    int getTextureId();

    TextureFormat getTextureFormat();

    int getWidth();

    int getHeight();

}
