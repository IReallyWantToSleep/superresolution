package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.graphics.opengl.GlDebug.objectLabel;
import static org.lwjgl.opengl.GL45.*;

public class GlTexture1D implements ITexture, IDebuggableObject {
    private static final int DEFAULT_ALIGNMENT = 4;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private int id;
    private final TextureDescription description;
    private int width;
    private int currentMipmapLevel;


    protected GlTexture1D(TextureDescription description) {
        validateDimensions(description.getWidth());
        this.id = Gl.DSA.createTexture1D();
        this.description = description;
        this.width = description.getWidth();
        if (description.getType() != TextureType.Texture1D) {
            throw new RuntimeException();
        }
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
        return description.getMipmapSettings();
    }

    private void configureTextureParameters() {
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, description.getMipmapSettings().isEnabled() ? description.getFilterMode() == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : description.getFilterMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, description.getMipmapSettings().isEnabled() ? description.getFilterMode() == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : description.getFilterMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, description.getWrapMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, description.getWrapMode().gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, currentMipmapLevel);
        Gl.DSA.textureParameterf(this.id, GL_TEXTURE_LOD_BIAS, description.getMipmapSettings().getBias());
    }

    private void allocateTextureStorage() {
        int levels = description.getMipmapSettings().isEnabled() ? (currentMipmapLevel + 1) : 1;
        Gl.DSA.textureStorage1D(this.id, levels, description.getFormat().gl(), width);
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

    public void configureMipmap() {
        if (description.getMipmapSettings().isAutoGenerate()) {
            this.currentMipmapLevel = calculateMaxMipLevel();
            return;
        }
        this.currentMipmapLevel = Math.min(description.getMipmapSettings().getLevels(), calculateMaxMipLevel());
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
        objectLabel(GL_TEXTURE, (int) handle(), getDebugLabel());
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
    
    public TextureDescription getTextureDescription() {
        return description;
    }
}