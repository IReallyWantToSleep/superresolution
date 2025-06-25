package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.utils.GlBlitRenderer;
import io.homo.superresolution.core.graphics.impl.IDebuggableObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.homo.superresolution.core.graphics.opengl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL43.*;

public class GlTexture2D implements ITexture, IDebuggableObject {
    public static final int AUTO_MIPMAP_LEVEL = 0;
    private static final int MAX_MIPMAP_LEVELS = 16;
    private static final int DEFAULT_ALIGNMENT = 4;
    private final TextureFormat format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private TextureUsages usages = null;
    private TextureType type = null;
    private TextureFilterMode filterMode = null;
    private TextureWrapMode wrapMode = null;
    private int id;
    private int width;
    private int height;
    private int mipmapLevel;
    private boolean mipmapEnabled;

    protected GlTexture2D(TextureDescription description) {
        validateDimensions(description.getWidth(), description.getHeight());
        this.id = Gl.DSA.createTexture2D();
        this.format = description.getFormat();
        this.width = description.getWidth();
        this.height = description.getHeight();
        this.usages = description.getUsages();
        this.type = description.getType();
        this.filterMode = description.getFilterMode();
        this.wrapMode = description.getWrapMode();
        configureMipmap(description.getMipmapSettings().getLevels());
        initializeTexture();
    }

    public static GlTexture2D create(TextureDescription description) {
        return new GlTexture2D(description);
    }

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, ITexture texture) {
        GlBlitRenderer.blitToScreen(texture, viewWidth, viewHeight);
    }

    public int getMipmapLevel() {
        return mipmapLevel;
    }

    public GlTextureView getMipView(int level) {
        return mipViews.computeIfAbsent(level, this::createMipView);
    }

    private GlTextureView createMipView(int level) {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE_2D | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
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
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
    }

    private void allocateTextureStorage() {
        int levels = mipmapEnabled ? (mipmapLevel + 1) : 1;
        Gl.DSA.textureStorage2D(this.id, levels, format.gl(), width, height);
    }

    private void initializeTexture() {
        try (GlState ignored = new GlState(GlState.STATE_TEXTURE_2D | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES)) {
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
                width, height, 1
        );
    }

    @Override
    public int handle() {
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
        glSafeObjectLabel(GL_TEXTURE, handle(), getDebugLabel());
    }

    public void generateMipmap() {
        Gl.DSA.generateTextureMipmap(this.id);
    }

    @Override
    public void destroy() {
        mipViews.values().forEach(GlTextureView::destroy);
        mipViews.clear();
        Gl.DSA.deleteTexture(this.id);
        this.id = -1;
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        initializeTexture();
    }
}