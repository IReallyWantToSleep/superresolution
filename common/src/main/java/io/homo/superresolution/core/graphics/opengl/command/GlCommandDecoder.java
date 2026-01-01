/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.opengl.*;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttributeFormat;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import org.lwjgl.opengl.GL44;

import static io.homo.superresolution.core.graphics.opengl.GlDebug.*;
import static org.lwjgl.opengl.GL43.*;

public class GlCommandDecoder implements ICommandDecoder {
    private final GlDevice device;
    private GlCommandBuffer currentCommandBuffer;

    public GlCommandDecoder(GlDevice device) {
        this.device = device;
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
    public void setViewport(ICommandBuffer commandBuffer, float x, float y, float width, float height) {
        putGlCommand(commandBuffer, () -> {
            glViewport((int) x, (int) y, (int) width, (int) height);
        });
    }

    @Override
    public void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height) {
        putGlCommand(commandBuffer, () -> {
            glScissor(x, y, width, height);
        });
    }

    @Override
    public void setLineWidth(ICommandBuffer commandBuffer, float width) {
        putGlCommand(commandBuffer, () -> {
            glLineWidth(width);
        });
    }

    @Override
    public void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a) {
        putGlCommand(commandBuffer, () -> {
            glBlendColor(r, g, b, a);
        });
    }

    @Override
    public void draw(
            ICommandBuffer commandBuffer,
            RenderPass renderPass,
            PrimitiveType primitiveType,
            IVertexBuffer vertexBuffer,
            int vertexCount,
            int firstVertex
    ) {
        putGlCommand(commandBuffer, () -> {
            GlRenderPass glRenderPass = (GlRenderPass) renderPass;
            glRenderPass.bind();
            glRenderPass.begin((GlCommandBuffer) commandBuffer);

            GraphicsPipeline pipeline = renderPass.pipeline();
            if (pipeline instanceof GlGraphicsPipeline glPipeline) {
                glUseProgram((int) glPipeline.shader().handle());
                glPipeline.setupRenderStates();
                glPipeline.descriptorSet().update();
                glPipeline.descriptorSet().apply();
            }

            if (vertexBuffer instanceof GlVertexBuffer glVertexBuffer) {
                glVertexBuffer.getVao().bind();
                glBindBuffer(GL_ARRAY_BUFFER, (int) vertexBuffer.handle());
            }

            glDrawArrays(switch (primitiveType) {
                case Lines -> GL_LINES;
                case Triangle -> GL_TRIANGLES;
                case TriangleStrip -> GL_TRIANGLE_STRIP;
                case Points -> GL_POINTS;
            }, firstVertex, vertexCount);

            if (vertexBuffer instanceof GlVertexBuffer glVertexBuffer) {
                glVertexBuffer.getVao().unbind();
            }
            glRenderPass.end((GlCommandBuffer) commandBuffer);
        });
    }

    @Override
    public void dispatch(
            ICommandBuffer commandBuffer,
            ComputePipeline computePipeline,
            int groupCountX,
            int groupCountY,
            int groupCountZ
    ) {
        putGlCommand(commandBuffer, () -> {
            if (computePipeline instanceof GlComputePipeline glPipeline) {
                glUseProgram((int) glPipeline.shader().handle());
                glPipeline.descriptorSet().update();
                glPipeline.descriptorSet().apply();
            }

            glDispatchCompute(groupCountX, groupCountY, groupCountZ);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        });
    }

    @Override
    public ICommandBuffer beginCommandBuffer() {
        currentCommandBuffer = (GlCommandBuffer) device.createCommandBuffer();
        return currentCommandBuffer;
    }

    @Override
    public ICommandBuffer endCommandBuffer() {
        return currentCommandBuffer;
    }

    @Override
    public ICommandBuffer endAndSubmitCommandBuffer() {
        currentCommandBuffer.submit(device);
        return currentCommandBuffer;
    }

    @Override
    public ICommandBuffer currentCommandBuffer() {
        return currentCommandBuffer;
    }

    private int getGlType(VertexAttributeFormat format) {
        return switch (format) {
            case FLOAT, FLOAT2, FLOAT3,
                 FLOAT4 -> GL_FLOAT;
            case INT, INT2, INT3, INT4 -> GL_INT;
            case UINT, UINT2, UINT3, UINT4 -> GL_UNSIGNED_INT;
            case BYTE4_NORMALIZED -> GL_BYTE;
            case UBYTE4_NORMALIZED -> GL_UNSIGNED_BYTE;
            case SHORT2, SHORT4 -> GL_SHORT;
            case USHORT2, USHORT4 -> GL_UNSIGNED_SHORT;
        };
    }

    private boolean isNormalized(VertexAttributeFormat format) {
        return switch (format) {
            case BYTE4_NORMALIZED, UBYTE4_NORMALIZED -> true;
            default -> false;
        };
    }

    private boolean isIntegerType(VertexAttributeFormat format) {
        return switch (format) {
            case INT, INT2, INT3, INT4, UINT, UINT2, UINT3, UINT4 -> true;
            default -> false;
        };
    }

    @Override
    public IDevice getDevice() {
        return device;
    }
}
