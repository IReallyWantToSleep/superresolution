package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.commands.DrawCommand;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.opengl.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderBaseUniform;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.opengl.shader.uniform.GlShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import org.lwjgl.opengl.GL44;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.homo.superresolution.core.graphics.opengl.GlDebug.*;
import static org.lwjgl.opengl.GL43.*;

public class GlCommandDecoder implements ICommandDecoder {
    private final GlDevice device;
    private final GlCommandEncoder commandEncoder;


    public GlCommandDecoder(GlDevice device, GlCommandEncoder commandEncoder) {
        this.device = device;
        this.commandEncoder = commandEncoder;
    }

    private void putGlCommand(ICommandBuffer commandBuffer, Runnable glCalls) {
        if (commandBuffer instanceof GlCommandBuffer) {
            ((GlCommandBuffer) commandBuffer)._addGlCalls(
                    glCalls
            );
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        if (texture == null) throw new OpenGLException("clearTextureRGBA: 输入的纹理对象为null");

        final int debugId = nextClearId();
        final String debugName = "clearTextureRGBA: " + texture.getTextureFormat();

        if (RenderSystems.opengl().supportsARBClearTexture) {
            TextureFormat format = texture.getTextureFormat();
            if (format.isInteger()) {
                int[] intColor = new int[color.length];
                for (int i = 0; i < color.length; i++) intColor[i] = (int) (color[i] * 255);
                putGlCommand(commandBuffer, () -> {
                    pushGroup(debugId, debugName);
                    try {
                        GL44.glClearTexImage((int) texture.handle(), 0, format.gl(), GL_UNSIGNED_INT, intColor);

                    } finally {
                        popGroup();
                    }
                });
            } else {
                putGlCommand(commandBuffer, () -> {
                    pushGroup(debugId, debugName);
                    try {
                        GL44.glClearTexImage((int) texture.handle(), 0, format.gl(), GL_FLOAT, color);

                    } finally {
                        popGroup();
                    }
                });
            }
        } else {
            putGlCommand(commandBuffer, () -> {
                pushGroup(debugId, debugName);
                try (GlState state = new GlState(
                        GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                )) {
                    int fbo = glGenFramebuffers();


                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, (int) texture.handle(), 0);

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
                } finally {
                    popGroup();
                }
            });
        }
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        if (texture == null) throw new OpenGLException("clearTextureDepth: 输入的纹理对象为null");

        final int debugId = nextClearId();
        final String debugName = "clearTextureDepth";

        if (RenderSystems.opengl().supportsARBClearTexture) {
            putGlCommand(commandBuffer, () -> {
                pushGroup(debugId, debugName);
                try {
                    float[] clearDepth = new float[]{depth};
                    GL44.glClearTexImage((int) texture.handle(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, clearDepth);

                } finally {
                    popGroup();
                }
            });
        } else {
            putGlCommand(commandBuffer, () -> {
                pushGroup(debugId, debugName);
                try (GlState state = new GlState(
                        GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                )) {
                    int fbo = glGenFramebuffers();


                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, (int) texture.handle(), 0);

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
                } finally {
                    popGroup();
                }
            });
        }
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        if (texture == null) throw new OpenGLException("clearTextureStencil: 输入的纹理对象为null");

        final int debugId = nextClearId();
        final String debugName = "clearTextureStencil";

        if (RenderSystems.opengl().supportsARBClearTexture) {
            putGlCommand(commandBuffer, () -> {
                pushGroup(debugId, debugName);
                try {
                    int[] clearStencil = new int[]{stencil};
                    GL44.glClearTexImage((int) texture.handle(), 0, GL_STENCIL_INDEX, GL_UNSIGNED_INT, clearStencil);

                } finally {
                    popGroup();
                }
            });
        } else {
            putGlCommand(commandBuffer, () -> {
                pushGroup(debugId, debugName);
                try (GlState state = new GlState(
                        GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                )) {
                    int fbo = glGenFramebuffers();


                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, (int) texture.handle(), 0);

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
                } finally {
                    popGroup();
                }
            });
        }
    }

    @Override
    public void copyTexture(
            ICommandBuffer commandBuffer,
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

        final int debugId = nextCopyId();
        final String debugName = String.format("copyTexture: %s -> %s",
                src.getTextureFormat(), dst.getTextureFormat());

        switch (src.getTextureType()) {
            case Texture1D:
                putGlCommand(commandBuffer, () -> {
                    pushGroup(debugId, debugName);
                    try {
                        glCopyImageSubData(
                                (int) src.handle(), GL_TEXTURE_1D, srcLevel, srcX0, 0, 0,
                                (int) dst.handle(), GL_TEXTURE_1D, dstLevel, dstX0, 0, 0,
                                srcX1 - srcX0, 1, 1
                        );

                    } finally {
                        popGroup();
                    }
                });
                break;
            case Texture2D:
                putGlCommand(commandBuffer, () -> {
                    pushGroup(debugId, debugName);
                    try {
                        glCopyImageSubData(
                                (int) src.handle(), GL_TEXTURE_2D, srcLevel, srcX0, srcY0, 0,
                                (int) dst.handle(), GL_TEXTURE_2D, dstLevel, dstX0, dstY0, 0,
                                srcX1 - srcX0, srcY1 - srcY0, 1
                        );

                    } finally {
                        popGroup();
                    }
                });
                break;
            default:
                throw new UnsupportedOperationException("Unsupported texture type: " + src.getTextureType());
        }
    }

    @Override
    public void copyBuffer(
            ICommandBuffer commandBuffer,
            IBuffer src,
            IBuffer dst,
            long srcOffset,
            long dstOffset,
            long size
    ) {
        final int debugId = nextCopyId();
        final String debugName = String.format("copyBuffer: %d bytes", size);

        putGlCommand(commandBuffer, () -> {
            pushGroup(debugId, debugName);
            try {
                glCopyBufferSubData(
                        (int) src.handle(),
                        (int) dst.handle(),
                        srcOffset,
                        dstOffset,
                        size
                );

            } finally {
                popGroup();
            }
        });
    }

    @Override
    public void decodeDraw(ICommandBuffer commandBuffer, DrawCommand command) {
        IRenderState.StateSnapshot stateSnapshot = commandEncoder.renderState().get();
        ICommandDecoder.super.decodeDraw(commandBuffer, command);
        commandEncoder.renderState().apply(stateSnapshot);
    }

    @Override
    public void draw(
            ICommandBuffer commandBuffer,
            IShaderProgram<?> shaderProgram,
            IFrameBuffer frameBuffer,
            DrawObject drawObject,
            int firstVertex,
            int vertexCount
    ) {
        if (shaderProgram == null) {
            throw new OpenGLException("draw: 着色器程序为null");
        }

        final int debugId = nextDrawId();
        final String debugName = String.format("draw: %s (%d verts)",
                drawObject.getPrimitiveType(), vertexCount);

        List<ResourcesBindingSnapshot> resourcesSnapshot = createResourcesBindingSnapshot((GlShaderProgram) shaderProgram);

        putGlCommand(commandBuffer, () -> {
            pushGroup(debugId, debugName);
            try (GlState ig = new GlState(
                    GlState.STATE_DRAW_FBO |
                            GlState.STATE_ACTIVE_TEXTURE |
                            GlState.STATE_TEXTURES |
                            GlState.STATE_PROGRAM |
                            GlState.STATE_VERTEX_OPERATIONS
            )) {

                if (frameBuffer != null) {
                    if (frameBuffer instanceof GlFrameBuffer) {
                        ((GlFrameBuffer) frameBuffer).bind(FrameBufferBindPoint.All, true);
                    } else {
                        throw new OpenGLException("draw: 目标帧缓冲区不是由OpenGL创建的");
                    }
                }
                setupShaderProgram(shaderProgram, resourcesSnapshot);
                glBindBuffer(GL_ARRAY_BUFFER, (int) drawObject.getVertexBuffer().handle());
                glBindVertexArray((int) drawObject.getVertexArray().handle());

                int glPrimitive = switch (drawObject.getPrimitiveType()) {
                    case TRIANGLES -> GL_TRIANGLES;
                    case TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
                    case LINES -> GL_LINES;
                    case POINTS -> GL_POINTS;
                };
                glDrawArrays(glPrimitive, firstVertex, vertexCount);
                if (drawObject.isOnce()) {
                    drawObject.destroy();
                }
            } finally {
                popGroup();
            }
        });
    }

    private List<ResourcesBindingSnapshot> createResourcesBindingSnapshot(GlShaderProgram shaderProgram) {
        List<ResourcesBindingSnapshot> resourcesBindingSnapshot = new ArrayList<>();
        Map<String, GlShaderBaseUniform<?, ?>> uniformMap = shaderProgram.uniforms().getUniformMap();
        uniformMap.forEach((name, uniform) -> {
            if (uniform instanceof GlShaderUniformBuffer) {
                if (((GlShaderUniformBuffer) uniform).buffer() != null) {
                    resourcesBindingSnapshot.add(new ResourcesBindingSnapshot(
                            uniform.name(),
                            uniform.binding(),
                            uniform.type(),
                            uniform.access(),
                            ((GlShaderUniformBuffer) uniform).buffer()
                    ));
                }
            } else if (uniform instanceof GlShaderUniformStorageTexture) {
                if (((GlShaderUniformStorageTexture) uniform).texture() != null) {
                    resourcesBindingSnapshot.add(new ResourcesBindingSnapshot(
                            uniform.name(),
                            uniform.binding(),
                            uniform.type(),
                            uniform.access(),
                            ((GlShaderUniformStorageTexture) uniform).texture()
                    ));
                }
            } else if (uniform instanceof GlShaderUniformSamplerTexture) {
                if (((GlShaderUniformSamplerTexture) uniform).texture() != null) {
                    resourcesBindingSnapshot.add(new ResourcesBindingSnapshot(
                            uniform.name(),
                            uniform.binding(),
                            uniform.type(),
                            uniform.access(),
                            ((GlShaderUniformSamplerTexture) uniform).texture()
                    ));
                }
            }
        });
        return resourcesBindingSnapshot;
    }

    private void setupShaderProgram(IShaderProgram<?> shaderProgram, List<ResourcesBindingSnapshot> resourcesBindingSnapshots) {
        glUseProgram((int) shaderProgram.handle());
        resourcesBindingSnapshots.forEach((uniform) -> {
            switch (uniform.resourcesType()) {
                case UniformBuffer -> {
                    if (Gl.isLegacy()) {
                        int blockIndex = glGetUniformBlockIndex(
                                (int) shaderProgram.handle(),
                                uniform.name()
                        );
                        if (blockIndex == GL_INVALID_INDEX) {
                            throw new RuntimeException("Uniform block '%s' not found".formatted(uniform.name()));
                        }
                        glUniformBlockBinding((int) shaderProgram.handle(), blockIndex, uniform.binding());
                        glBindBufferBase(GL_UNIFORM_BUFFER, uniform.binding(), (int) uniform.object().handle());
                    } else {
                        Gl.DSA.bindBufferBase(GL_UNIFORM_BUFFER, uniform.binding(), (int) uniform.object().handle());
                    }
                }
                case StorageTexture -> Gl.DSA.bindImageTexture(
                        uniform.binding(),
                        (int) uniform.object().handle(),
                        0,
                        false,
                        0,
                        switch (uniform.access()) {
                            case Read -> GL_READ_ONLY;
                            case Write -> GL_WRITE_ONLY;
                            case Both -> GL_READ_WRITE;
                        },
                        ((ITexture) uniform.object()).getTextureFormat().gl()
                );
                case SamplerTexture -> {
                    ITexture texture = (ITexture) uniform.object();
                    glActiveTexture(GL_TEXTURE0 + uniform.binding());

                    if (texture.getTextureType() == TextureType.Texture1D) {
                        glBindTexture(GL_TEXTURE_1D, (int) texture.handle());
                    } else if (texture.getTextureType() == TextureType.Texture2D) {
                        glBindTexture(GL_TEXTURE_2D, (int) texture.handle());
                    } else {
                        glBindTexture(GL_TEXTURE_2D, (int) texture.handle());
                    }
                    glUniform1i(glGetUniformLocation((int) shaderProgram.handle(), uniform.name()), uniform.binding());
                }

            }
        });
    }

    @Override
    public void dispatchCompute(
            ICommandBuffer commandBuffer,
            IShaderProgram<?> shaderProgram,
            int x,
            int y,
            int z
    ) {
        if (shaderProgram == null) {
            throw new OpenGLException("dispatchCompute: 着色器程序为null");
        }

        final int debugId = nextComputeId();
        final String debugName = String.format("dispatchCompute: %dx%dx%d", x, y, z);

        List<ResourcesBindingSnapshot> resourcesSnapshot = createResourcesBindingSnapshot((GlShaderProgram) shaderProgram);

        putGlCommand(commandBuffer, () -> {
            pushGroup(debugId, debugName);
            try (GlState state = new GlState(
                    GlState.STATE_TEXTURE |
                            GlState.STATE_ACTIVE_TEXTURE |
                            GlState.STATE_TEXTURES |
                            GlState.STATE_PROGRAM
            )) {
                setupShaderProgram(shaderProgram, resourcesSnapshot);
                glDispatchCompute(x, y, z);
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            } finally {
                popGroup();
            }
        });
    }

    @Override
    public void applyRenderState(ICommandBuffer commandBuffer, IRenderState.StateSnapshot stateSnapshot) {
        final int debugId = nextStateId();
        final String debugName = "applyRenderState";
        putGlCommand(commandBuffer, () -> {
            pushGroup(debugId, debugName);
            commandEncoder.renderState().apply(stateSnapshot);
            popGroup();
        });
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    private record ResourcesBindingSnapshot(
            String name,
            int binding,
            ShaderUniformType resourcesType,
            ShaderUniformAccess access,
            GpuObject object
    ) {

    }
}
