package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.graphics.opengl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL45.*;

public class GlTexture1D implements ITexture, IDebuggableObject {
    public static final int AUTO_MIPMAP_LEVEL = 0;
    private static final int MAX_MIPMAP_LEVELS = 16;
    private static final int DEFAULT_ALIGNMENT = 4;
    private final int id;
    private final TextureFormat format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private TextureUsages usages = null;
    private TextureType type = null;
    private TextureFilterMode filterMode = null;
    private TextureWrapMode wrapMode = null;
    private int width;
    private int mipmapLevel;
    private boolean mipmapEnabled;

    protected GlTexture1D(TextureDescription description) {
        validateDimensions(description.getWidth());
        this.id = Gl.DSA.createTexture1D();
        this.format = description.getFormat();
        this.width = description.getWidth();
        this.usages = description.getUsages();
        this.type = description.getType();
        this.filterMode = description.getFilterMode();
        this.wrapMode = description.getWrapMode();
        if (type != TextureType.Texture1D) {
            throw new RuntimeException();
        }

        configureMipmap(description.getMipmapSettings().getLevels());
        initializeTexture();
    }

    public static GlTexture1D create(TextureDescription description) {
        return new GlTexture1D(description);
    }

    public GlTextureView getMipView(int level) {
        return mipViews.computeIfAbsent(level, this::createMipView);
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
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
    }

    private void allocateTextureStorage() {
        int levels = mipmapEnabled ? (mipmapLevel + 1) : 1;
        Gl.DSA.textureStorage1D(this.id, levels, format.gl(), width);
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
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
        Gl.DSA.textureSubImage1D(
                this.id,
                mipLevel,
                xoffset,
                width,
                format,
                type,
                MemoryUtil.memAddress(data)
        );
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
        return format;
    }

    @Override
    public TextureUsages getTextureUsages() {
        return usages;
    }

    @Override
    public TextureType getTextureType() {
        return type;
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return filterMode;
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return wrapMode;
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
        Gl.DSA.generateTextureMipmap(this.id);
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