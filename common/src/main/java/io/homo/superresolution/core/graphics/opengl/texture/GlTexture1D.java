package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.graphics.opengl.Gl.setGlObjectLabel;
import static org.lwjgl.opengl.GL45.*;

public class GlTexture1D implements ITexture, IDebuggableObject {
    private static final int DEFAULT_ALIGNMENT = 4;
    private final TextureFormat format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private int id;
    private TextureUsages usages = null;
    private TextureType type = null;
    private TextureFilterMode filterMode = null;
    private TextureWrapMode wrapMode = null;
    private TextureMipmapSettings mipmapSettings;

    private int width;
    private int currentMipmapLevel;


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
        this.mipmapSettings = description.getMipmapSettings();
        configureMipmap();
        initializeTexture();
    }

    public static GlTexture1D create(TextureDescription description) {
        return new GlTexture1D(description);
    }

    public GlTextureView getMipView(int level) {
        return mipViews.computeIfAbsent(level, this::createMipView);
    }

    private GlTextureView createMipView(int level) {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
            if (level < 0 || level > this.currentMipmapLevel) {
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

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return mipmapSettings;
    }

    private void configureTextureParameters() {
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, mipmapSettings.isEnabled() ? filterMode == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, mipmapSettings.isEnabled() ? filterMode == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, currentMipmapLevel);
        Gl.DSA.textureParameterf(this.id, GL_TEXTURE_LOD_BIAS, mipmapSettings.getBias());
    }

    private void allocateTextureStorage() {
        int levels = mipmapSettings.isEnabled() ? (currentMipmapLevel + 1) : 1;
        Gl.DSA.textureStorage1D(this.id, levels, format.gl(), width);
    }

    private void initializeTexture() {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
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
    public long handle() {
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

    public void configureMipmap() {
        if (mipmapSettings.isAutoGenerate()) {
            this.currentMipmapLevel = calculateMaxMipLevel();
            return;
        }
        this.currentMipmapLevel = Math.min(mipmapSettings.getLevels(), calculateMaxMipLevel());
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
        setGlObjectLabel(GL_TEXTURE, (int) handle(), getDebugLabel());
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
        Gl.DSA.deleteTexture(this.id);
        this.id = Gl.DSA.createTexture1D();
        configureMipmap();
        initializeTexture();
    }
}