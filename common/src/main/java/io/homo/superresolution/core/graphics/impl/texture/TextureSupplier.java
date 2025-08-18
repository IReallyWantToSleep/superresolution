package io.homo.superresolution.core.graphics.impl.texture;

import java.util.function.Supplier;

public class TextureSupplier implements ITexture {
    private final Supplier<ITexture> supplier;

    TextureSupplier(Supplier<ITexture> supplier) {
        this.supplier = supplier;
    }

    public static TextureSupplier of(Supplier<ITexture> supplier) {
        return new TextureSupplier(supplier);
    }

    @Override
    public long handle() {
        return supplier.get().handle();
    }

    @Override
    public TextureFormat getTextureFormat() {
        return supplier.get().getTextureFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return supplier.get().getTextureUsages();
    }

    @Override
    public TextureType getTextureType() {
        return supplier.get().getTextureType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return supplier.get().getTextureFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return supplier.get().getTextureWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return supplier.get().getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return supplier.get().getTextureDescription();
    }

    @Override
    public int getWidth() {
        return supplier.get().getWidth();
    }

    @Override
    public int getHeight() {
        return supplier.get().getHeight();
    }

    @Override
    public void destroy() {
        supplier.get().destroy();
    }

    @Override
    public void resize(int width, int height) {
        supplier.get().resize(width, height);
    }
}
