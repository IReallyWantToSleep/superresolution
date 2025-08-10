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
    private final TextureFormat format;
    private final Map<Integer, GlTextureView> mipViews = new ConcurrentHashMap<>();
    private TextureUsages usages = null;
    private TextureType type = null;
    private TextureFilterMode filterMode = null;
    private TextureWrapMode wrapMode = null;

    public TextureMipmapSettings getMipmapSettings() {
        return mipmapSettings;
    }

    private TextureMipmapSettings mipmapSettings;

    private int id;
    private int width;
    private int height;
    private int currentMipmapLevel;

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
        this.mipmapSettings = description.getMipmapSettings();

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
                    GL_TEXTURE_2D,
                    level,    // minLevel
                    1,        // numLevels
                    0,        // minLayer
                    1         // numLayers
            );
        }
    }

    protected void configureTextureParameters() {
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MIN_FILTER, mipmapSettings.isEnabled() ? filterMode == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAG_FILTER, mipmapSettings.isEnabled() ? filterMode == TextureFilterMode.LINEAR ? GL_LINEAR_MIPMAP_NEAREST : GL_NEAREST_MIPMAP_NEAREST : filterMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_S, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_WRAP_T, wrapMode.gl());
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_BASE_LEVEL, 0);
        Gl.DSA.textureParameteri(this.id, GL_TEXTURE_MAX_LEVEL, currentMipmapLevel);
        Gl.DSA.textureParameterf(this.id, GL_TEXTURE_LOD_BIAS, mipmapSettings.getBias());
    }

    protected void allocateTextureStorage() {
        int levels = mipmapSettings.isEnabled() ? (currentMipmapLevel + 1) : 1;
        Gl.DSA.textureStorage2D(this.id, levels, format.gl(), width, height);
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
        this.id = -1;
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        Gl.DSA.deleteTexture(this.id);
        this.id = Gl.DSA.createTexture2D();
        configureMipmap();
        initializeTexture();
    }
}