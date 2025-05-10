package io.homo.superresolution.core.gl.texture;

import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.utils.GlBlitRenderer;
import io.homo.superresolution.core.impl.IDebuggableObject;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;

import static io.homo.superresolution.core.gl.Gl.glSafeObjectLabel;
import static org.lwjgl.opengl.GL43.*;

public class GlTexture implements ITexture, IDebuggableObject {
    public int id;
    public int format;
    public int width;
    public int height;
    private boolean mipmap = false;

    public GlTexture(int width, int height, int format, boolean mipmap) {
        this.id = 0;
        this.format = format;
        this.width = width;
        this.height = height;
        this.mipmap = mipmap;
        initializeTexture();
    }

    public static GlTexture create(int width, int height, TextureFormat format) {
        return create(width, height, format, false);
    }

    public static GlTexture create(int width, int height, TextureFormat format, boolean mipmap) {
        return new GlTexture(width, height, format.gl(), mipmap);
    }

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, int id) {
        GlBlitRenderer.blitToScreen(id, viewWidth, viewHeight);
    }

    private void initializeTexture() {
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("无效的尺寸: " + width + "x" + height);
        }
        glDeleteTextures(this.id);
        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.id);
        int minFilter = mipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        int maxLevel = 0;
        if (mipmap) {
            int maxSize = Math.max(this.width, this.height);
            maxLevel = (int) (Math.log(maxSize) / Math.log(2));
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
        glTexStorage2D(GL_TEXTURE_2D, maxLevel + 1, this.format, this.width, this.height);
        glBindTexture(GL_TEXTURE_2D, 0);
        updateDebugLabel(getDebugLabel());
    }

    @Override
    public void destroy() {
        glDeleteTextures(this.id);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        initializeTexture();
    }

    public void copyFromFBO(int srcFbo) {
        glBindFramebuffer(GL_FRAMEBUFFER, srcFbo);
        glBindTexture(GL_TEXTURE_2D, this.id);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void copyFromTex(int srcTex) {
        glCopyImageSubData(srcTex, GL_TEXTURE_2D, 0, 0, 0, 0,
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
        try (GlState ignored = new GlState()) {
            glBindTexture(GL_TEXTURE_2D, this.id);
            glGenerateMipmap(GL_TEXTURE_2D);
        }
    }
}