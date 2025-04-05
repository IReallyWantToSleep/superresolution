package io.homo.superresolution.common.render.gl.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.utils.BlitRenderer;
import io.homo.superresolution.common.render.impl.IDebuggableObject;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.glGenTextures;

public class GlTexture implements ITexture, IDebuggableObject {
    public int id;
    public int format;
    public int width;
    public int height;
    private boolean mipmap = false;

    public GlTexture(int width, int height, int format, boolean mipmap) {
        this.id = glGenTextures();
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
        RenderSystem.assertOnRenderThread();
        BlitRenderer.blitToScreen(id, viewWidth, viewHeight);
    }

    private void initializeTexture() {
        glBindTexture(GL_TEXTURE_2D, this.id);
        int minFilter = mipmap ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        int maxLevel = mipmap ? 8 : 0;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
        glTexStorage2D(GL_TEXTURE_2D, maxLevel + 1, this.format, this.width, this.height);
        glBindTexture(GL_TEXTURE_2D, 0);
        updateDebugLabel(getDebugLabel());
    }

    @Override
    public void destroy() {
        RenderSystem.assertOnRenderThread();
        glDeleteTextures(this.id);
    }

    @Override
    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        glDeleteTextures(this.id);
        this.id = glGenTextures();
        this.width = width;
        this.height = height;
        initializeTexture();
    }

    public void copyFromFBO(int srcFbo) {
        RenderSystem.assertOnRenderThread();
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