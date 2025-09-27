/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.opengl.framebuffer;

import io.homo.superresolution.core.graphics.impl.texture.*;

import java.util.function.Supplier;

public class GlOnlyNameTexture implements ITexture {
    private final Supplier<TextureFormat> textureFormatSupplier;
    private final Supplier<Integer> widthSupplier;
    private final Supplier<Integer> heightSupplier;
    private final Supplier<Long> handleSupplier;

    public GlOnlyNameTexture(Supplier<TextureFormat> textureFormatSupplier, Supplier<Integer> widthSupplier, Supplier<Integer> heightSupplier, Supplier<Long> handleSupplier) {
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
        return TextureFilterMode.Nearest;
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return TextureWrapMode.ClampToEdge;
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
