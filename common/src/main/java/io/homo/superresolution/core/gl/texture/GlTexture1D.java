package io.homo.superresolution.core.gl.texture;

import io.homo.superresolution.core.gl.Gl;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.impl.IDebuggableObject;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.gl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL45.*;


public class GlTexture1D implements ITexture, IDebuggableObject {
    public static final int AUTO_MIPMAP_LEVEL = 0;
    private static final int MAX_MIPMAP_LEVELS = 16;
    private static final int DEFAULT_ALIGNMENT = 4;

    private final int id;
    private final int format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private int width;
    private int mipmapLevel;
    private boolean mipmapEnabled;

    public GlTexture1D(int width, int format, int mipmapLevel) {
        validateDimensions(width);
        this.id = Gl.DSA.createTexture1D();
        this.format = format;
        this.width = width;
        configureMipmap(mipmapLevel);
        initializeTexture();
    }

    public static GlTexture1D create(int width, TextureFormat format) {
        return create(width, format, AUTO_MIPMAP_LEVEL);
    }

    public static GlTexture1D create(int width, TextureFormat format, int mipmapLevel) {
        return new GlTexture1D(width, format.gl(), mipmapLevel);
    }

    public GlTextureView getMipView(int level) {
        return mipViews.computeIfAbsent(level, k -> createMipView(k));
    }

    private GlTextureView createMipView(int level) {
        try (GlState ignored = new GlState()) {
            if (level < 0 || level > this.mipmapLevel) {
                throw new IllegalArgumentException("Invalid mip level: " + level);
            }

            return GlTextureView.create(
                    this,
                    GL_TEXTURE_1D,
                    level,
                    1,
                    0,
                    1
            );
        }
    }

    private void configureTextureParameters() {
        if (format != TextureFormat.R32UI.gl()) {
            Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER,
                    mipmapEnabled ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST);
        } else {
            Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER,
                    mipmapEnabled ? GL_NEAREST_MIPMAP_NEAREST : GL_NEAREST);
        }
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
    }

    private void allocateTextureStorage() {
        int levels = mipmapEnabled ? (mipmapLevel + 1) : 1;
        Gl.DSA.textureStorage1D(this.id, levels, format, width);
    }

    private void initializeTexture() {
        try (GlState ignored = new GlState()) {
            configureTextureParameters();
            allocateTextureStorage();
            updateDebugLabel(getDebugLabel());
        }
    }

    public void uploadData(int mipLevel, int xoffset, int width,
                           int format, int type, ByteBuffer data, int alignment) {
        try (GlState ignored = new GlState()) {
            glBindTexture(GL_TEXTURE_1D, this.id);
            glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
            glTexSubImage1D(
                    GL_TEXTURE_1D,
                    mipLevel,
                    xoffset,
                    width,
                    format,
                    type,
                    data
            );
        }
    }

    public void uploadData(int format, int type, ByteBuffer data) {
        uploadData(0, 0, width, format, type, data, DEFAULT_ALIGNMENT);
    }

    private void validateDimensions(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Invalid texture width: " + width);
        }
    }

    public void copyFromTex(int srcTex) {
        glCopyImageSubData(
                srcTex, GL_TEXTURE_1D, 0, 0, 0, 0,
                this.id, GL_TEXTURE_1D, 0, 0, 0, 0,
                width, 1, 1
        );
    }

    @Override
    public int getTextureId() {
        return id;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return TextureFormat.fromGl(format);
    }

    public void configureMipmap(int requestedLevel) {
        this.mipmapEnabled = requestedLevel >= 0;
        this.mipmapLevel = calculateActualMipLevel(requestedLevel);
    }

    private int calculateActualMipLevel(int requestedLevel) {
        if (!mipmapEnabled) return 0;

        if (requestedLevel == AUTO_MIPMAP_LEVEL) {
            return calculateMaxMipLevel();
        }

        return Math.min(requestedLevel, MAX_MIPMAP_LEVELS);
    }

    private int calculateMaxMipLevel() {
        return (int) (Math.log(width) / Math.log(2));
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public String getDebugLabel() {
        return string();
    }

    @Override
    public void updateDebugLabel(String newLabel) {
        glSafeObjectLabel(GL_TEXTURE, getTextureId(), getDebugLabel());
    }

    public void generateMipmap() {
        try (GlState ignored = new GlState()) {
            glBindTexture(GL_TEXTURE_1D, this.id);
            glGenerateMipmap(GL_TEXTURE_1D);
        }
    }

    @Override
    public void destroy() {
        mipViews.values().forEach(GlTextureView::destroy);
        mipViews.clear();
        Gl.DSA.deleteTexture(this.id);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        initializeTexture();
    }
}