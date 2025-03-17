package io.homo.superresolution.common.render.gl.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.render.gl.utils.BlitRenderer;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class GlTexture implements ITexture {
    public int id;
    public int format;
    public int width;
    public int height;

    public GlTexture(int width, int height, int format) {
        this.id = TextureUtil.generateTextureId();
        this.format = format;
        this.width = width;
        this.height = height;
        initializeTexture();
    }

    public static GlTexture create(int width, int height, TextureFormat format) {
        return new GlTexture(width, height, format.gl());
    }

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, int id) {
        RenderSystem.assertOnRenderThread();
        BlitRenderer.blitToScreen(id, viewWidth, viewHeight);
    }

    private void initializeTexture() {
        glBindTexture(GL_TEXTURE_2D, this.id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        glTexStorage2D(GL_TEXTURE_2D, 1, this.format, this.width, this.height);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void destroy() {
        RenderSystem.assertOnRenderThread();
        glDeleteTextures(this.id);
    }

    @Override
    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        TextureUtil.releaseTextureId(this.id);
        this.id = TextureUtil.generateTextureId();
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
}
