package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.*;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture1D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import org.lwjgl.opengl.*;

import static io.homo.superresolution.core.graphics.opengl.Gl.glBindImageTexture;
import static io.homo.superresolution.core.graphics.opengl.Gl.glBindTextureUnit;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL44.glClearTexImage;

public class GlRenderSystem implements IRenderSystem {
    private GlRenderState renderState;
    private GlDevice device;

    @Override
    public void initRenderSystem() {
        this.renderState = new GlRenderState();
        this.device = new GlDevice();
    }

    @Override
    public void destroyRenderSystem() {
        this.renderState = null;
    }

    @Override
    public GlDevice device() {
        return device;
    }


    @Override
    public void clearTextureRGBA(ITexture texture, float[] color) {
        if (texture == null) {
            throw new OpenGLException("clearTextureRGBA: 输入的纹理对象为null");
        }
        TextureFormat format = texture.getTextureFormat();
        if (format.isInteger()) {
            int[] intColor = new int[color.length];
            for (int i = 0; i < color.length; i++) intColor[i] = (int) (color[i] * 255);
            glClearTexImage(texture.handle(), 0, format.gl(), GL_UNSIGNED_INT, intColor);
        } else {
            glClearTexImage(texture.handle(), 0, format.gl(), GL_FLOAT, color);
        }
    }

    @Override
    public void clearTextureDepth(ITexture texture, float depth) {
        if (texture == null) {
            throw new OpenGLException("clearTextureDepth: 输入的纹理对象为null");
        }
        float[] clearDepth = new float[]{depth};
        glClearTexImage(texture.handle(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, clearDepth);
    }

    @Override
    public void clearTextureStencil(ITexture texture, int stencil) {
        if (texture == null) {
            throw new OpenGLException("clearTextureStencil: 输入的纹理对象为null");
        }
        int[] clearStencil = new int[]{stencil};
        glClearTexImage(texture.handle(), 0, GL_STENCIL_INDEX, GL_UNSIGNED_INT, clearStencil);
    }

    @Override
    public void copyTexture(
            ITexture src,
            ITexture dst,
            int srcX0,
            int srcY0,
            int srcX1,
            int srcY1,
            int srcLevel,
            int dstX0,
            int dstY0,
            int dstX1,
            int dstY1,
            int dstLevel
    ) {
        if (src == null || dst == null) {
            throw new OpenGLException("copyTexture: 源或目标纹理为null");
        }
        if (src.getTextureFormat() != dst.getTextureFormat()) {
            throw new OpenGLException("copyTexture: 源和目标纹理格式不一致，无法拷贝：" +
                    src.getTextureFormat() + " -> " + dst.getTextureFormat());
        }
        if (src.getTextureType() != dst.getTextureType()) {
            throw new OpenGLException("copyTexture: 源和目标纹理类型不一致，无法拷贝：" +
                    src.getTextureType() + " -> " + dst.getTextureType());
        }

        switch (src.getTextureType()) {
            case Texture1D:
                GL45.glCopyImageSubData(
                        src.handle(), GL11.GL_TEXTURE_1D, srcLevel, srcX0, 0, 0,
                        dst.handle(), GL11.GL_TEXTURE_1D, dstLevel, dstX0, 0, 0,
                        srcX1 - srcX0, 1, 1
                );
                break;
            case Texture2D:
                GL45.glCopyImageSubData(
                        src.handle(), GL11.GL_TEXTURE_2D, srcLevel, srcX0, srcY0, 0,
                        dst.handle(), GL11.GL_TEXTURE_2D, dstLevel, dstX0, dstY0, 0,
                        srcX1 - srcX0, srcY1 - srcY0, 1
                );
                break;
        }

    }

    @Override
    public void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z) {
        if (shaderProgram == null) {
            throw new OpenGLException("dispatchCompute: 着色器程序为null");
        }
        try (GlState state = new GlState(
                GlState.STATE_TEXTURE_2D |
                        GlState.STATE_ACTIVE_TEXTURE |
                        GlState.STATE_TEXTURES |
                        GlState.STATE_PROGRAM
        )) {
            setupShaderProgram((GlShaderProgram) shaderProgram);
            Gl.glDispatchCompute(x, y, z);
        }
    }

    @Override
    public void draw(IShaderProgram<?> shaderProgram, DrawObject drawObject, int firstVertex, int vertexCount) {
        if (shaderProgram == null) {
            throw new OpenGLException("draw: 着色器程序为null");
        }
        try (GlState ig = new GlState(
                GlState.STATE_TEXTURE_2D |
                        GlState.STATE_ACTIVE_TEXTURE |
                        GlState.STATE_TEXTURES |
                        GlState.STATE_PROGRAM |
                        GlState.STATE_VERTEX_OPERATIONS |
                        GlState.STATE_DRAW_FBO |
                        GlState.STATE_READ_FBO
        )) {
            setupShaderProgram((GlShaderProgram) shaderProgram);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, drawObject.getVertexBuffer().handle());
            GL45.glBindVertexArray(drawObject.getVertexArray().handle());
            int glPrimitive = switch (drawObject.getPrimitiveType()) {
                case TRIANGLES -> GL11.GL_TRIANGLES;
                case TRIANGLE_STRIP -> GL11.GL_TRIANGLE_STRIP;
                case LINES -> GL11.GL_LINES;
                case POINTS -> GL11.GL_POINTS;
            };
            GL11.glDrawArrays(glPrimitive, firstVertex, vertexCount);
        }
    }

    private void setupShaderProgram(GlShaderProgram shaderProgram) {
        Gl.glUseProgram(shaderProgram.handle());
        var uniformMap = shaderProgram.uniforms().getUniformMap();
        uniformMap.forEach((name, uniform) -> {
            if (uniform instanceof GlShaderUniformBuffer<?>) {
                if (((GlShaderUniformBuffer<?>) uniform).buffer() != null) {
                    Gl.DSA.bindBufferBase(GL45.GL_UNIFORM_BUFFER, uniform.binding(), ((GlShaderUniformBuffer<?>) uniform).buffer().handle());
                }
            }
            if (uniform instanceof GlShaderUniformStorageTexture) {
                if (((GlShaderUniformStorageTexture) uniform).texture() != null) {
                    glBindImageTexture(
                            uniform.binding(),
                            ((GlShaderUniformStorageTexture) uniform).texture().handle(),
                            0,
                            false,
                            0,
                            switch (uniform.access()) {
                                case Read -> GL_READ_ONLY;
                                case Write -> GL_WRITE_ONLY;
                                case Both -> GL_READ_WRITE;
                            },
                            ((GlShaderUniformStorageTexture) uniform).texture().getTextureFormat().gl()
                    );
                }
            }
            if (uniform instanceof GlShaderUniformSamplerTexture) {
                if (((GlShaderUniformSamplerTexture) uniform).texture() != null) {
                    glBindTextureUnit(uniform.binding(), ((GlShaderUniformSamplerTexture) uniform).texture().handle());
                }
            }
        });
    }

    @Override
    public IRenderState renderState() {
        return renderState;
    }

    @Override
    public void finish() {
        glFinish();
    }
}