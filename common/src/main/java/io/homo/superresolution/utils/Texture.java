package io.homo.superresolution.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class Texture implements CanDestroy, CanResize {
    public int id;
    public int format;
    public int width;
    public int height;
    public Texture(int width,int height,int format){
        this.id = TextureUtil.generateTextureId();
        this.format = format;
        this.width = width;
        this.height = height;

        glBindTexture(GL_TEXTURE_2D, this.id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void destroy() {
        glDeleteTextures(this.id);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        glBindTexture(GL_TEXTURE_2D, this.id);
        glTexImage2D(GL_TEXTURE_2D, 0, this.format, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void blitToScreen(){
        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, width, height);
        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderInstance = minecraft.gameRenderer.blitShader;
        shaderInstance.setSampler("DiffuseSampler", this.id);
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        if (shaderInstance.MODEL_VIEW_MATRIX != null) {
            shaderInstance.MODEL_VIEW_MATRIX.set((new Matrix4f()).translation(0.0F, 0.0F, -2000.0F));
        }
        if (shaderInstance.PROJECTION_MATRIX != null) {
            shaderInstance.PROJECTION_MATRIX.set(matrix4f);
        }
        shaderInstance.apply();
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0, height, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(width, height, 0.0).uv(1f, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(width, 0.0, 0.0).uv(1f, 1f).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, 1f).color(255, 255, 255, 255).endVertex();
        BufferUploader.draw(bufferBuilder.end());
        shaderInstance.clear();
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
    }
    public static Texture loadTexture(String filePath) {
        int width;
        int height;
        ByteBuffer buf;
        // 加载纹理文件
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            buf = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (buf == null) {
                throw new Exception(STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Texture tex = new Texture(width,height,GL_RGBA8);
        glBindTexture(GL_TEXTURE_2D, tex.id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, tex.format, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }
}
