package io.homo.superresolution.core.graphics.impl.texture;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;

public interface ITexture extends Destroyable, Resizable, GpuObject {
    TextureFormat getTextureFormat();

    TextureUsages getTextureUsages();

    TextureType getTextureType();

    TextureFilterMode getTextureFilterMode();

    TextureWrapMode getTextureWrapMode();

    TextureMipmapSettings getMipmapSettings();

    TextureDescription getTextureDescription();


    int getWidth();

    int getHeight();

    default String string() {
        return getTextureDescription().getLabel() != null ? getTextureDescription().getLabel() : "ITexture{" +
                "id=" + handle() +
                "format=" + getTextureFormat() +
                "width=" + getWidth() +
                "height=" + getHeight() +
                '}';
    }
}
