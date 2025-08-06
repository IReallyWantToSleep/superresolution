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

    default TextureDescription getTextureDescription() {
        return TextureDescription.create()
                .filterMode(getTextureFilterMode())
                .format(getTextureFormat())
                .size(getWidth(), getHeight())
                .type(getTextureType())
                .wrapMode(getTextureWrapMode())
                .mipmapSettings(getMipmapSettings())
                .usages(getTextureUsages())
                .build();
    }


    int getWidth();

    int getHeight();

    default String string() {
        return "ITexture{" +
                "id=" + handle() +
                "format=" + getTextureFormat() +
                "width=" + getWidth() +
                "height=" + getHeight() +
                '}';
    }
}
