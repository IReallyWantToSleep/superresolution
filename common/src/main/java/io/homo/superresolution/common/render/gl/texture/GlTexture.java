package io.homo.superresolution.common.render.gl.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
#if MC_VER > MC_1_20_1
import io.homo.superresolution.common.render.gl.buffer.VertexBuffer;
import io.homo.superresolution.common.render.gl.shader.BlitShader;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class GlTexture implements Destroyable, Resizable {
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

    public static void blitToScreen(int srcWidth, int srcHeight, int viewWidth, int viewHeight, int id) {
        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, viewWidth, viewHeight);
        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderInstance = minecraft.gameRenderer.blitShader;
        shaderInstance.setSampler("DiffuseSampler", id);
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float) srcWidth, (float) srcHeight, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        if (shaderInstance.MODEL_VIEW_MATRIX != null) {
            shaderInstance.MODEL_VIEW_MATRIX.set((new Matrix4f()).translation(0.0F, 0.0F, -2000.0F));
        }

        if (shaderInstance.PROJECTION_MATRIX != null) {
            shaderInstance.PROJECTION_MATRIX.set(matrix4f);
        }
        shaderInstance.apply();
        #if MC_VER > MC_1_20_1
        try (VertexArray vao = new VertexArray();
             VertexBuffer vbo = new VertexBuffer()) {
            float[] vertices = {
                    0, 0, 0,
                    1, 0, 0,
                    1, 1, 0,
                    0, 1, 0,
            };
            vao.bind();
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadData(vertices, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 3 * Float.BYTES, 0);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 3 * Float.BYTES, 2 * Float.BYTES);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }
        #else
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0, srcHeight, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(srcWidth, srcHeight, 0.0).uv(1, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(srcWidth, 0.0, 0.0).uv(1, 1).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, 1).color(255, 255, 255, 255).endVertex();
        BufferUploader.draw(bufferBuilder.end());
        #endif
        shaderInstance.clear();
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
}
