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

import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.utils.GlBlitRenderer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.graphics.opengl.GlDebug.objectLabel;
import static org.lwjgl.opengl.GL43.*;

public class GlTexture2D implements ITexture, IDebuggableObject {
    public static final int AUTO_MIPMAP_LEVEL = 0;
    private static final int MAX_MIPMAP_LEVELS = 16;
    private static final int DEFAULT_ALIGNMENT = 4;
    private final Map<Integer, ITextureView> mipViews = new ConcurrentHashMap<>();
    private final TextureDescription description;
    private int id;
    private int width;
    private int height;
    private int currentMipmapLevel;

    protected GlTexture2D(TextureDescription description) {
        validateDimensions(description.getWidth(), description.getHeight());
        if (description.getType() != TextureType.Texture2D) {
            throw new RuntimeException();
        }
        this.id = Gl.DSA.createTexture2D();
        this.description = description;
        this.width = description.getWidth();
        this.height = description.getHeight();

    }

    public static GlTexture2D create(TextureDescription description) {
        GlTexture2D texture2D = new GlTexture2D(description);
        texture2D.configureMipmap();
        texture2D.initializeTexture();
        return texture2D;
    }

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, ITexture texture) {
        GlBlitRenderer.blitToScreen(texture, viewWidth, viewHeight);
    }

    public int getMipmapLevel() {
        return currentMipmapLevel;
    }

    public ITextureView getMipView(int level) {
        return mipViews.computeIfAbsent(level, this::createMipView);
    }

    private ITextureView createMipView(int level) {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
            if (level < 0 || level > this.currentMipmapLevel) {
                throw new IllegalArgumentException("Invalid mip level: " + level);
            }

            TextureViewDescription desc = TextureViewDescription.create(this)
                    .baseMipLevel(level)
                    .mipLevelCount(1)
                    .build();
            return GlTextureView.create(desc);
        }
    }

    protected void configureTextureParameters() {
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, description.getMipmapSettings().isEnabled() ? description.getFilterMode() == TextureFilterMode.Linear ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : description.getFilterMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, description.getMipmapSettings().isEnabled() ? description.getFilterMode() == TextureFilterMode.Linear ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : description.getFilterMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, description.getWrapMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, description.getWrapMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, currentMipmapLevel);
        Gl.DSA.textureParameterf(this.id, GL_TEXTURE_LOD_BIAS, description.getMipmapSettings().getBias());
    }

    protected void allocateTextureStorage() {
        int levels = description.getMipmapSettings().isEnabled() ? (currentMipmapLevel + 1) : 1;
        Gl.DSA.textureStorage2D(this.id, levels, description.getFormat().gl(), width, height);
    }

    protected void initializeTexture() {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
            configureTextureParameters();
            allocateTextureStorage();
            updateDebugLabel(getDebugLabel());
        }
    }

    public void uploadData(int mipLevel, int xoffset, int yoffset, int width, int height,
                           int format, int type, ByteBuffer data, int alignment) {
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);

        Gl.DSA.textureSubImage2D(
                this.id,
                mipLevel,
                xoffset,
                yoffset,
                width,
                height,
                format,
                type,
                MemoryUtil.memAddress(data)
        );
    }

    public void uploadData(int format, int type, ByteBuffer data) {
        uploadData(0, 0, 0, width, height, format, type, data, DEFAULT_ALIGNMENT);
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid texture dimensions: " + width + "x" + height);
        }
    }

    public void copyFromTex(int srcTex) {
        glCopyImageSubData(
                srcTex, GL_TEXTURE_2D, 0, 0, 0, 0,
                this.id, GL_TEXTURE_2D, 0, 0, 0, 0,
                width, height, 1
        );
    }

    @Override
    public long handle() {
        return id;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return description.getFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return description.getUsages();
    }

    @Override
    public TextureType getTextureType() {
        return description.getType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return description.getFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return description.getWrapMode();
    }

    public TextureMipmapSettings getMipmapSettings() {
        return description.getMipmapSettings();
    }

    public TextureDescription getTextureDescription() {
        return description;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void configureMipmap() {
        if (description.getMipmapSettings().isAutoGenerate()) {
            this.currentMipmapLevel = calculateMaxMipLevel();
            return;
        }
        this.currentMipmapLevel = Math.min(description.getMipmapSettings().getLevels(), calculateMaxMipLevel());
    }

    private int calculateMaxMipLevel() {
        int maxDimension = Math.max(width, height);
        return (int) (Math.log(maxDimension) / Math.log(2));
    }

    @Override
    public String getDebugLabel() {
        return string();
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        objectLabel(GL_TEXTURE, (int) handle(), getDebugLabel());
    }

    public void generateMipmap() {
        Gl.DSA.generateTextureMipmap(this.id);
    }

    @Override
    public void destroy() {
        mipViews.values().forEach(ITextureView::destroy);
        mipViews.clear();
        Gl.DSA.deleteTexture(this.id);
        this.id = -1;
    }
}