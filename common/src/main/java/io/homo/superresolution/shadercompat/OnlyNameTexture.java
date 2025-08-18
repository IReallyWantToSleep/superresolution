package io.homo.superresolution.shadercompat;

import io.homo.superresolution.core.graphics.impl.texture.*;

import java.util.function.Supplier;

public class OnlyNameTexture implements ITexture {
    private final Supplier<TextureFormat> textureFormatSupplier;
    private final Supplier<Integer> widthSupplier;
    private final Supplier<Integer> heightSupplier;
    private final Supplier<Long> handleSupplier;

    public OnlyNameTexture(Supplier<TextureFormat> textureFormatSupplier, Supplier<Integer> widthSupplier, Supplier<Integer> heightSupplier, Supplier<Long> handleSupplier) {
        this.textureFormatSupplier = textureFormatSupplier;
        this.widthSupplier = widthSupplier;
        this.heightSupplier = heightSupplier;
        this.handleSupplier = handleSupplier;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return textureFormatSupplier.get();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return TextureUsages.create().sampler().storage();
    }

    @Override
    public TextureType getTextureType() {
        return TextureType.Texture2D;
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return TextureFilterMode.NEAREST;
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return TextureWrapMode.CLAMP_TO_EDGE;
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return TextureMipmapSettings.disabled();
    }

    @Override
    public TextureDescription getTextureDescription() {
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

    @Override
    public int getWidth() {
        return widthSupplier.get();
    }

    @Override
    public int getHeight() {
        return heightSupplier.get();
    }

    @Override
    public long handle() {
        return handleSupplier.get();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void resize(int width, int height) {

    }
}
