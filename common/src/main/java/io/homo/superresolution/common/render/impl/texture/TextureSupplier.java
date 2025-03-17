package io.homo.superresolution.common.render.impl.texture;

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
    public int getTextureId() {
        return supplier.get().getTextureId();
    }

    @Override
    public TextureFormat getTextureFormat() {
        return supplier.get().getTextureFormat();
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
