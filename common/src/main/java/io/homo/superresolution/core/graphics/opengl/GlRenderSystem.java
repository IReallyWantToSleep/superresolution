package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderBaseUniform;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import java.util.Map;

import static org.lwjgl.opengl.GL41.*;

public class GlRenderSystem implements IRenderSystem {
    private GlRenderState renderState;
    private GlDevice device;
    private boolean supportsARBClearTexture;

    @Override
    public void initRenderSystem() {
        this.renderState = new GlRenderState();
        this.device = new GlDevice();
        supportsARBClearTexture = GL.getCapabilities().GL_ARB_clear_texture || GL.getCapabilities().OpenGL44;
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
        if (texture == null) throw new OpenGLException("clearTextureRGBA: 输入的纹理对象为null");

        if (supportsARBClearTexture) {
            TextureFormat format = texture.getTextureFormat();
            if (format.isInteger()) {
                int[] intColor = new int[color.length];
                for (int i = 0; i < color.length; i++) intColor[i] = (int) (color[i] * 255);
                GL44.glClearTexImage(texture.handle(), 0, format.gl(), GL_UNSIGNED_INT, intColor);
            } else {
                GL44.glClearTexImage(texture.handle(), 0, format.gl(), GL_FLOAT, color);
            }
        } else {
            try (GlState state = new GlState(
                    GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
            )) {
                int fbo = glGenFramebuffers();
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.handle(), 0);

                int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    glDeleteFramebuffers(fbo);
                    throw new OpenGLException("clearTextureRGBA: FBO状态不完整, 状态码: " + status);
                }

                glViewport(0, 0, texture.getWidth(), texture.getHeight());
                glEnable(GL_SCISSOR_TEST);
                glScissor(0, 0, texture.getWidth(), texture.getHeight());

                glClearColor(color[0], color[1], color[2], color[3]);
                glClear(GL_COLOR_BUFFER_BIT);

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glDeleteFramebuffers(fbo);
            }
        }
    }

    @Override
    public void clearTextureDepth(ITexture texture, float depth) {
        if (texture == null) throw new OpenGLException("clearTextureDepth: 输入的纹理对象为null");

        if (supportsARBClearTexture) {
            float[] clearDepth = new float[]{depth};
            GL44.glClearTexImage(texture.handle(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, clearDepth);
        } else {
            try (GlState state = new GlState(
                    GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
            )) {
                int fbo = glGenFramebuffers();
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texture.handle(), 0);

                int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    glDeleteFramebuffers(fbo);
                    throw new OpenGLException("clearTextureDepth: FBO状态不完整, 状态码: " + status);
                }

                glViewport(0, 0, texture.getWidth(), texture.getHeight());
                glEnable(GL_SCISSOR_TEST);
                glScissor(0, 0, texture.getWidth(), texture.getHeight());

                glClearDepth(depth);
                glClear(GL_DEPTH_BUFFER_BIT);

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glDeleteFramebuffers(fbo);
            }
        }
    }

    @Override
    public void clearTextureStencil(ITexture texture, int stencil) {
        if (texture == null) throw new OpenGLException("clearTextureStencil: 输入的纹理对象为null");

        if (supportsARBClearTexture) {
            int[] clearStencil = new int[]{stencil};
            GL44.glClearTexImage(texture.handle(), 0, GL_STENCIL_INDEX, GL_UNSIGNED_INT, clearStencil);
        } else {
            try (GlState state = new GlState(
                    GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
            )) {
                int fbo = glGenFramebuffers();
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, texture.handle(), 0);

                int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    glDeleteFramebuffers(fbo);
                    throw new OpenGLException("clearTextureStencil: FBO状态不完整, 状态码: " + status);
                }

                glViewport(0, 0, texture.getWidth(), texture.getHeight());
                glEnable(GL_SCISSOR_TEST);
                glScissor(0, 0, texture.getWidth(), texture.getHeight());

                glClearStencil(stencil);
                glClear(GL_STENCIL_BUFFER_BIT);

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glDeleteFramebuffers(fbo);
            }
        }
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
                GL43.glCopyImageSubData(
                        src.handle(), GL_TEXTURE_1D, srcLevel, srcX0, 0, 0,
                        dst.handle(), GL_TEXTURE_1D, dstLevel, dstX0, 0, 0,
                        srcX1 - srcX0, 1, 1
                );
                break;
            case Texture2D:
                GL43.glCopyImageSubData(
                        src.handle(), GL_TEXTURE_2D, srcLevel, srcX0, srcY0, 0,
                        dst.handle(), GL_TEXTURE_2D, dstLevel, dstX0, dstY0, 0,
                        srcX1 - srcX0, srcY1 - srcY0, 1
                );
                break;
        }

    }

    @Override
    public void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
        glCopyBufferSubData(
                src.handle(),
                dst.handle(),
                srcOffset,
                dstOffset,
                size
        );
    }


    @Override
    public void draw(
            IShaderProgram<?> shaderProgram,
            IFrameBuffer frameBuffer,
            DrawObject drawObject,
            int firstVertex,
            int vertexCount
    ) {
        if (shaderProgram == null) {
            throw new OpenGLException("draw: 着色器程序为null");
        }
        try (GlState ig = new GlState(
                GlState.STATE_ALL
        )) {
            if (frameBuffer != null) {
                if (frameBuffer instanceof GlFrameBuffer) {
                    ((GlFrameBuffer) frameBuffer).bind(FrameBufferBindPoint.All, true);
                } else {
                    throw new OpenGLException("draw: 目标帧缓冲区不是由OpenGL创建的");
                }
            }
            setupShaderProgram((GlShaderProgram) shaderProgram);
            glBindBuffer(GL_ARRAY_BUFFER, drawObject.getVertexBuffer().handle());
            glBindVertexArray(drawObject.getVertexArray().handle());
            int glPrimitive = switch (drawObject.getPrimitiveType()) {
                case TRIANGLES -> GL_TRIANGLES;
                case TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
                case LINES -> GL_LINES;
                case POINTS -> GL_POINTS;
            };
            glDrawArrays(glPrimitive, firstVertex, vertexCount);
            if (drawObject.isOnce()) drawObject.destroy();
        }
    }

    @Override
    public void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z) {
        if (shaderProgram == null) {
            throw new OpenGLException("dispatchCompute: 着色器程序为null");
        }
        try (GlState state = new GlState(
                GlState.STATE_TEXTURE |
                        GlState.STATE_ACTIVE_TEXTURE |
                        GlState.STATE_TEXTURES |
                        GlState.STATE_PROGRAM
        )) {
            setupShaderProgram((GlShaderProgram) shaderProgram);
            GL43.glDispatchCompute(x, y, z);
        }
    }

    @Override
    public IRenderState renderState() {
        return renderState;
    }

    @Override
    public void finish() {
        glFinish();
    }

    private void setupShaderProgram(GlShaderProgram shaderProgram) {
        glUseProgram(shaderProgram.handle());

        Map<String, GlShaderBaseUniform<?, ?>> uniformMap = shaderProgram.uniforms().getUniformMap();
        uniformMap.forEach((name, uniform) -> {
            if (uniform instanceof GlShaderUniformBuffer) {
                if (((GlShaderUniformBuffer) uniform).buffer() != null) {
                    if (Gl.isLegacy()) {
                        int blockIndex = glGetUniformBlockIndex(
                                shaderProgram.handle(),
                                uniform.name()
                        );
                        if (blockIndex == GL_INVALID_INDEX) {
                            throw new RuntimeException("Uniform block '%s' not found".formatted(uniform.name()));
                        }
                        glUniformBlockBinding(shaderProgram.handle(), blockIndex, uniform.binding());
                        glBindBufferBase(GL_UNIFORM_BUFFER, uniform.binding(), ((GlShaderUniformBuffer) uniform).buffer().handle());
                    } else {
                        Gl.DSA.bindBufferBase(GL_UNIFORM_BUFFER, uniform.binding(), ((GlShaderUniformBuffer) uniform).buffer().handle());
                    }
                }
            }
            if (uniform instanceof GlShaderUniformStorageTexture) {
                if (((GlShaderUniformStorageTexture) uniform).texture() != null) {
                    Gl.DSA.bindImageTexture(
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
                    /* GlState无法保存用glBindTextureUnit绑定的纹理单元 （你别说，还顺带兼容OpenGL4.1了）
                       Gl.DSA.bindTextureUnit(
                               uniform.binding(),
                               ((GlShaderUniformSamplerTexture) uniform).texture().handle()
                       );
                    */
                    ITexture texture = ((GlShaderUniformSamplerTexture) uniform).texture();
                    glActiveTexture(GL_TEXTURE0 + uniform.binding());

                    if (texture.getTextureType() == TextureType.Texture1D) {
                        glBindTexture(GL_TEXTURE_1D, texture.handle());
                    } else if (texture.getTextureType() == TextureType.Texture2D) {
                        glBindTexture(GL_TEXTURE_2D, texture.handle());
                    } else {
                        glBindTexture(GL_TEXTURE_2D, texture.handle());
                    }
                    glUniform1i(glGetUniformLocation(shaderProgram.handle(), uniform.name()), uniform.binding());
                }
            }
        });
    }
}