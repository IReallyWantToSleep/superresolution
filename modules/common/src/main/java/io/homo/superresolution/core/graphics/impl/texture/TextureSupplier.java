/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
}
