package io.homo.superresolution.common.render.gl.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.render.gl.buffer.VertexBuffer;
import io.homo.superresolution.common.render.gl.shader.BlitShader;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;
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
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, viewWidth, viewHeight);
        BlitShader blitShader = BlitShader.getShader();
        blitShader.use();
        blitShader.bindTexture(id);
        try (VertexArray vao = new VertexArray();
             VertexBuffer vbo = new VertexBuffer()) {
            float[] vertices = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, 1f, 0f, 1f
            };
            vao.bind();
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadData(vertices, GL_STATIC_DRAW);
            int stride = 4 * Float.BYTES;
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0); // 位置属性
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES); // 纹理坐标属性
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }
        blitShader.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
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
