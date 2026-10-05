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

package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class GlTextureView implements ITextureView {
    private final ITexture parent;
    private final TextureViewDescription viewDescription;
    private int id;

    private GlTextureView(ITexture parent, TextureViewDescription viewDescription, int id) {
        this.parent = parent;
        this.viewDescription = viewDescription;
        this.id = id;
    }

    public static GlTextureView create(TextureViewDescription description) {
        ITexture parent = description.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Parent texture cannot be null");
        }
        if (parent.handle() == 0) {
            throw new IllegalStateException("Parent texture is not initialized");
        }

        int glTarget = switch (parent.getTextureType()) {
            case Texture1D -> GL_TEXTURE_1D;
            case Texture2D -> GL_TEXTURE_2D;
        };

        int viewId = Gl.DSA.createTextureView(
                (int) parent.handle(),
                glTarget,
                parent.getTextureFormat().gl(),
                description.getBaseMipLevel(),
                description.getMipLevelCount(),
                0,   // baseArrayLayer
                1    // layerCount
        );

        return new GlTextureView(parent, description, viewId);
    }

    @Deprecated
    public static GlTextureView create(ITexture parent, int type,
                                       int minLevel, int numLevels,
                                       int minLayer, int numLayers) {
        TextureViewDescription desc = TextureViewDescription.create(parent)
                .baseMipLevel(minLevel)
                .mipLevelCount(numLevels)
                .build();
        return create(desc);
    }

    @Override
    public ITexture getParent() {
        return parent;
    }

    @Override
    public TextureViewDescription getViewDescription() {
        return viewDescription;
    }

    @Override
    public long handle() {
        return id;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return parent.getTextureFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return parent.getTextureUsages();
    }

    @Override
    public TextureType getTextureType() {
        return parent.getTextureType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return parent.getTextureFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return parent.getTextureWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return parent.getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return parent.getTextureDescription();
    }

    @Override
    public int getWidth() {
        return viewDescription.getWidth();
    }

    @Override
    public int getHeight() {
        return viewDescription.getHeight();
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteTexture(this.id);
        this.id = -1;
    }
}
