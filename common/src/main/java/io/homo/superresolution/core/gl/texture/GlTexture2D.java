package io.homo.superresolution.core.gl.texture;

import io.homo.superresolution.core.gl.Gl;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.utils.GlBlitRenderer;
import io.homo.superresolution.core.impl.IDebuggableObject;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.gl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL43.*;

public class GlTexture2D implements ITexture, IDebuggableObject {
    public static final int AUTO_MIPMAP_LEVEL = 0;
    private static final int MAX_MIPMAP_LEVELS = 16;
    private static final int DEFAULT_ALIGNMENT = 4;
    private final int format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private int id;
    private int width;
    private int height;
    private int mipmapLevel;
    private boolean mipmapEnabled;

    public GlTexture2D(int width, int height, int format, int mipmapLevel) {
        validateDimensions(width, height);
        this.format = format;
        this.width = width;
        this.height = height;
        configureMipmap(mipmapLevel);
        initializeTexture();
    }

    public static GlTexture2D create(int width, int height, TextureFormat format) {
        return create(width, height, format, -1);
    }

    public static GlTexture2D create(int width, int height, TextureFormat format, int mipmapLevel) {
        return new GlTexture2D(width, height, format.gl(), mipmapLevel);
    }

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, int id) {
        GlBlitRenderer.blitToScreen(id, viewWidth, viewHeight);
    }

    public int getMipmapLevel() {
        return mipmapLevel;
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
                    GL_TEXTURE_2D,
                    level,    // minLevel
                    1,        // numLevels
                    0,        // minLayer
                    1         // numLayers
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
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        if (mipmapEnabled) Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
    }

    private void allocateTextureStorage() {
        int levels = mipmapEnabled ? (mipmapLevel + 1) : 1;
        Gl.DSA.textureStorage2D(this.id, levels, format, width, height);
    }

    private void initializeTexture() {
        try (GlState ignored = new GlState()) {
            if (this.id >= 0) {
                Gl.DSA.deleteTexture(this.id);
            }
            this.id = Gl.DSA.createTexture2D();
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

    public void copyFromFBO(int srcFbo) {
        glBindFramebuffer(GL_FRAMEBUFFER, srcFbo);
        glBindTexture(GL_TEXTURE_2D, this.id);
        Gl.DSA.copyTextureSubImage2D(
                this.id,
                0,
                0,
                0,
                0,
                0,
                width,
                height
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void copyFromTex(int srcTex) {
        glCopyImageSubData(
                srcTex, GL_TEXTURE_2D, 0, 0, 0, 0,
                this.id, GL_TEXTURE_2D, 0, 0, 0, 0,
                width, height, 1);
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
        int maxDimension = Math.max(width, height);
        return (int) (Math.log(maxDimension) / Math.log(2));
    }


    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
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
        this.height = height;
        initializeTexture();
    }
}