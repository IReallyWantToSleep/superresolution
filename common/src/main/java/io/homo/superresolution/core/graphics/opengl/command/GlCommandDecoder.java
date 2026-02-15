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
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttributeFormat;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.OpenGLException;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import org.lwjgl.opengl.GL44;

import static io.homo.superresolution.core.graphics.opengl.GlDebug.*;
import static org.lwjgl.opengl.GL43.*;

public class GlCommandDecoder implements ICommandDecoder {
    private final GlDevice device;

    public GlCommandDecoder(GlDevice device) {
        this.device = device;
    }

    private void putGlCommand(ICommandBuffer commandBuffer, Runnable glCalls) {
        requireGlCommandBuffer(commandBuffer, "putGlCommand")._addGlCalls(glCalls);
    }

    private GlCommandBuffer requireGlCommandBuffer(ICommandBuffer commandBuffer, String action) {
        if (commandBuffer == null) {
            throw new IllegalArgumentException(action + ": commandBuffer为null");
        }
        if (commandBuffer instanceof GlCommandBuffer glCommandBuffer) {
            return glCommandBuffer;
        }
        throw new IllegalArgumentException(action + ": commandBuffer类型错误: " + commandBuffer.getClass().getName());
    }

    private void requireTexture(ITexture texture, String action) {
        if (texture == null) {
            throw new IllegalArgumentException(action + ": 输入的纹理对象为null");
        }
    }

    private void requireBuffer(IBuffer buffer, String action) {
        if (buffer == null) {
            throw new IllegalArgumentException(action + ": 输入的缓冲对象为null");
        }
    }

    private void requireRangeInclusive(float value, float min, float max, String action, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(action + ": " + name + "超出范围[" + min + "," + max + "]");
        }
    }

    private void requireRangeInclusive(int value, int min, int max, String action, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(action + ": " + name + "超出范围[" + min + "," + max + "]");
        }
    }

    private void requirePositive(int value, String action, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(action + ": " + name + "必须为正数");
        }
    }

    private void requireNonNegative(int value, String action, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(action + ": " + name + "不能为负数");
        }
    }

    private void requireNonNegative(long value, String action, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(action + ": " + name + "不能为负数");
        }
    }

    private int mipSize(int baseSize, int level) {
        int size = baseSize >> level;
        return Math.max(1, size);
    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        requireGlCommandBuffer(commandBuffer, "clearTextureRGBA");
        requireTexture(texture, "clearTextureRGBA");
        if (color == null || color.length == 0) {
            throw new IllegalArgumentException("clearTextureRGBA: 颜色数组为空");
        }

        TextureFormat format = texture.getTextureFormat();
        if (format.isDepth() || format.isStencil()) {
            throw new IllegalArgumentException("clearTextureRGBA: 纹理格式不支持颜色清除: " + format);
        }
        if (color.length != format.getChannelCount()) {
            throw new IllegalArgumentException("clearTextureRGBA: 颜色分量数与纹理通道数不匹配");
        }
        for (float component : color) {
            requireRangeInclusive(component, 0.0f, 1.0f, "clearTextureRGBA", "颜色分量");
        }

        final int debugId = nextClearId();
        final String debugName = "Clear Texture (RGBA)";

        if (RenderSystems.opengl().supportsARBClearTexture) {
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
                try (
                        GlState state = new GlState(
                                GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                        )
                ) {
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
        requireGlCommandBuffer(commandBuffer, "clearTextureDepth");
        requireTexture(texture, "clearTextureDepth");
        requireRangeInclusive(depth, 0.0f, 1.0f, "clearTextureDepth", "深度值");

        TextureFormat format = texture.getTextureFormat();
        if (!format.isDepth()) {
            throw new IllegalArgumentException("clearTextureDepth: 纹理格式不支持深度清除: " + format);
        }

        final int debugId = nextClearId();
        final String debugName = "Clear Texture Depth";

        if (RenderSystems.opengl().supportsARBClearTexture && !format.isStencil()) {
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
                try (
                        GlState state = new GlState(
                                GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                        )
                ) {
                    int fbo = glGenFramebuffers();

                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                    // 明确告诉驱动不需要颜色输出
                    glDrawBuffer(GL_NONE);
                    glReadBuffer(GL_NONE);

                    int attachment = format.isStencil() ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, attachment, GL_TEXTURE_2D, (int) texture.handle(), 0);

                    int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                    if (status != GL_FRAMEBUFFER_COMPLETE && format.isStencil()) {
                        // fallback: 尝试只附加深度
                        glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
                        glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, (int) texture.handle(), 0);
                        int fallbackStatus = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                        if (fallbackStatus != GL_FRAMEBUFFER_COMPLETE) {
                            glDeleteFramebuffers(fbo);
                            throw new OpenGLException("clearTextureDepth: FBO状态不完整, 状态码: " + status + ", fallback: " + fallbackStatus);
                        }
                    } else if (status != GL_FRAMEBUFFER_COMPLETE) {
                        glDeleteFramebuffers(fbo);
                        throw new OpenGLException("clearTextureDepth: FBO状态不完整, 状态码: " + status);
                    }

                    glViewport(0, 0, texture.getWidth(), texture.getHeight());
                    if (format.isStencil()) {
                        // 仅清深度不动模板：禁止模板写入
                        glDepthMask(true);
                        glStencilMask(0);
                        glClearDepth(depth);
                        glClear(GL_DEPTH_BUFFER_BIT);
                        glStencilMask(0xFF);
                    } else {
                        glClearDepth(depth);
                        glClear(GL_DEPTH_BUFFER_BIT);
                    }
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
        requireGlCommandBuffer(commandBuffer, "clearTextureStencil");
        requireTexture(texture, "clearTextureStencil");
        requireRangeInclusive(stencil, 0, 255, "clearTextureStencil", "模板值");
        TextureFormat format = texture.getTextureFormat();
        if (!format.isDepthStencil()) {
            if (format.isDepth()) {
                throw new IllegalArgumentException("clearTextureStencil: 深度纹理不支持模板清除: " + format);
            }
            throw new IllegalArgumentException("clearTextureStencil: 纹理格式不支持模板清除: " + format);
        }

        final int debugId = nextClearId();
        final String debugName = "Clear Texture Stencil";

        putGlCommand(commandBuffer, () -> {
            pushGroup(debugId, debugName);
            try (
                    GlState state = new GlState(
                            GlState.STATE_DRAW_FBO | GlState.STATE_VIEWPORT | GlState.STATE_SCISSOR_TEST
                    )
            ) {
                int fbo = glGenFramebuffers();

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
                // 明确告诉驱动不需要颜色输出
                glDrawBuffer(GL_NONE);
                glReadBuffer(GL_NONE);

                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, (int) texture.handle(), 0);

                int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    // fallback: 尝试只附加模板
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
                    glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_TEXTURE_2D, (int) texture.handle(), 0);
                    int fallbackStatus = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                    if (fallbackStatus != GL_FRAMEBUFFER_COMPLETE) {
                        glDeleteFramebuffers(fbo);
                        throw new OpenGLException("clearTextureStencil: FBO状态不完整, 状态码: " + status + ", fallback: " + fallbackStatus);
                    }
                }

                glViewport(0, 0, texture.getWidth(), texture.getHeight());
                glEnable(GL_SCISSOR_TEST);
                glScissor(0, 0, texture.getWidth(), texture.getHeight());

                // 仅清模板不动深度：禁止深度写入
                glDepthMask(false);
                glStencilMask(0xFF);
                glClearStencil(stencil);
                glClear(GL_STENCIL_BUFFER_BIT);
                glDepthMask(true);

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glDeleteFramebuffers(fbo);
            } finally {
                popGroup();
            }
        });
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
        requireGlCommandBuffer(commandBuffer, "copyTexture");
        requireTexture(src, "copyTexture");
        requireTexture(dst, "copyTexture");
        if (src.getTextureFormat() != dst.getTextureFormat()) {
            throw new IllegalArgumentException("copyTexture: 源和目标纹理格式不一致，无法拷贝：" +
                    src.getTextureFormat() + " -> " + dst.getTextureFormat());
        }
        if (src.getTextureType() != dst.getTextureType()) {
            throw new IllegalArgumentException("copyTexture: 源和目标纹理类型不一致，无法拷贝：" +
                    src.getTextureType() + " -> " + dst.getTextureType());
        }
        requireNonNegative(srcLevel, "copyTexture", "srcLevel");
        requireNonNegative(dstLevel, "copyTexture", "dstLevel");
        int srcLevels = src.getMipmapSettings().resolveLevels(src.getWidth(), src.getHeight());
        int dstLevels = dst.getMipmapSettings().resolveLevels(dst.getWidth(), dst.getHeight());
        if (srcLevel >= srcLevels || dstLevel >= dstLevels) {
            throw new IllegalArgumentException("copyTexture: mipmap等级超出范围");
        }
        requireNonNegative(srcX0, "copyTexture", "srcX0");
        requireNonNegative(srcY0, "copyTexture", "srcY0");
        requireNonNegative(srcX1, "copyTexture", "srcX1");
        requireNonNegative(srcY1, "copyTexture", "srcY1");
        requireNonNegative(dstX0, "copyTexture", "dstX0");
        requireNonNegative(dstY0, "copyTexture", "dstY0");
        requireNonNegative(dstX1, "copyTexture", "dstX1");
        requireNonNegative(dstY1, "copyTexture", "dstY1");

        int srcWidth = mipSize(src.getWidth(), srcLevel);
        int srcHeight = mipSize(src.getHeight(), srcLevel);
        int dstWidth = mipSize(dst.getWidth(), dstLevel);
        int dstHeight = mipSize(dst.getHeight(), dstLevel);
        if (srcX1 <= srcX0 || dstX1 <= dstX0) {
            throw new IllegalArgumentException("copyTexture: X范围无效");
        }
        if (src.getTextureType() == TextureType.Texture1D) {
            if (srcY0 != 0 || srcY1 != 1 || dstY0 != 0 || dstY1 != 1) {
                throw new IllegalArgumentException("copyTexture: 1D纹理Y范围必须为[0,1]");
            }
            if (srcX1 > srcWidth || dstX1 > dstWidth) {
                throw new IllegalArgumentException("copyTexture: X范围超出纹理尺寸");
            }
        } else {
            if (srcY1 <= srcY0 || dstY1 <= dstY0) {
                throw new IllegalArgumentException("copyTexture: Y范围无效");
            }
            if (srcX1 > srcWidth || srcY1 > srcHeight || dstX1 > dstWidth || dstY1 > dstHeight) {
                throw new IllegalArgumentException("copyTexture: 范围超出纹理尺寸");
            }
        }

        final int debugId = nextCopyId();
        final String debugName = "Copy Texture";

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
        requireGlCommandBuffer(commandBuffer, "copyBuffer");
        requireBuffer(src, "copyBuffer");
        requireBuffer(dst, "copyBuffer");
        requireNonNegative(srcOffset, "copyBuffer", "srcOffset");
        requireNonNegative(dstOffset, "copyBuffer", "dstOffset");
        if (size <= 0) {
            throw new IllegalArgumentException("copyBuffer: size必须为正数");
        }
        if (srcOffset + size > src.getSize() || dstOffset + size > dst.getSize()) {
            throw new IllegalArgumentException("copyBuffer: 拷贝范围超出缓冲大小");
        }

        final int debugId = nextCopyId();
        final String debugName = "Copy Buffer";

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
        requireGlCommandBuffer(commandBuffer, "setViewport");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("setViewport: width/height必须为正数");
        }
        putGlCommand(commandBuffer, () -> {
            glViewport((int) x, (int) y, (int) width, (int) height);
        });
    }

    @Override
    public void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height) {
        requireGlCommandBuffer(commandBuffer, "setScissor");
        requireNonNegative(x, "setScissor", "x");
        requireNonNegative(y, "setScissor", "y");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("setScissor: width/height不能为负数");
        }
        putGlCommand(commandBuffer, () -> {
            glScissor(x, y, width, height);
        });
    }

    @Override
    public void setLineWidth(ICommandBuffer commandBuffer, float width) {
        requireGlCommandBuffer(commandBuffer, "setLineWidth");
        if (width <= 0) {
            throw new IllegalArgumentException("setLineWidth: width必须为正数");
        }
        putGlCommand(commandBuffer, () -> {
            glLineWidth(width);
        });
    }

    @Override
    public void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a) {
        requireGlCommandBuffer(commandBuffer, "setBlendConstants");
        requireRangeInclusive(r, 0.0f, 1.0f, "setBlendConstants", "r");
        requireRangeInclusive(g, 0.0f, 1.0f, "setBlendConstants", "g");
        requireRangeInclusive(b, 0.0f, 1.0f, "setBlendConstants", "b");
        requireRangeInclusive(a, 0.0f, 1.0f, "setBlendConstants", "a");
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
        GlCommandBuffer glCommandBuffer = requireGlCommandBuffer(commandBuffer, "draw");
        if (renderPass == null) {
            throw new IllegalArgumentException("draw: renderPass为null");
        }
        if (!(renderPass instanceof GlRenderPass glRenderPass)) {
            throw new IllegalArgumentException("draw: renderPass类型错误: " + renderPass.getClass().getName());
        }
        if (primitiveType == null) {
            throw new IllegalArgumentException("draw: primitiveType为null");
        }
        if (vertexBuffer == null) {
            throw new IllegalArgumentException("draw: vertexBuffer为null");
        }
        if (!(vertexBuffer instanceof GlVertexBuffer)) {
            throw new IllegalArgumentException("draw: vertexBuffer类型错误: " + vertexBuffer.getClass().getName());
        }
        requirePositive(vertexCount, "draw", "vertexCount");
        requireNonNegative(firstVertex, "draw", "firstVertex");
        if (firstVertex + vertexCount > vertexBuffer.getVertexCount()) {
            throw new IllegalArgumentException("draw: 顶点范围超出vertexBuffer大小");
        }
        if (!(renderPass.pipeline() instanceof GlGraphicsPipeline)) {
            throw new IllegalArgumentException("draw: pipeline类型错误: " + renderPass.pipeline().getClass().getName());
        }
        putGlCommand(commandBuffer, () -> {
            GlDebug.pushGroup(0x7170001, "Render Pass");
        });
        putGlCommand(commandBuffer, () -> {
            GlDebug.pushGroup(0x7170002, "Begin Render Pass");
            glRenderPass.bind();
        });
        //这里会把pass开始时的清理操作命令全放在commandBuffer
        glRenderPass.begin(glCommandBuffer);
        putGlCommand(commandBuffer, () -> {
            {
                GlDebug.pushGroup(0x7170003, "Setup Render Pipeline");
                GraphicsPipeline pipeline = renderPass.pipeline();
                if (pipeline instanceof GlGraphicsPipeline glPipeline) {
                    glUseProgram((int) glPipeline.shader().handle());
                    glPipeline.setupRenderStates();
                    glPipeline.applyDynamicStates(commandBuffer);
                    glPipeline.descriptorSet().update();
                    glPipeline.descriptorSet().apply();
                }
                GlDebug.popGroup();
            }

            {
                GlDebug.pushGroup(0x7170004, "Setup Vertex Buffer");
                if (vertexBuffer instanceof GlVertexBuffer glVertexBuffer) {
                    glVertexBuffer.getVao().bind();
                    glBindBuffer(GL_ARRAY_BUFFER, (int) vertexBuffer.handle());
                }
                GlDebug.popGroup();
            }
            GlDebug.popGroup(); // Begin Render Pass
        });
        putGlCommand(commandBuffer, () -> {
            glDrawArrays(switch (primitiveType) {
                case Lines -> GL_LINES;
                case Triangle -> GL_TRIANGLES;
                case TriangleStrip -> GL_TRIANGLE_STRIP;
                case Points -> GL_POINTS;
            }, firstVertex, vertexCount);
            if (vertexBuffer instanceof GlVertexBuffer glVertexBuffer) {
                glVertexBuffer.getVao().unbind();
            }
        });
        putGlCommand(commandBuffer, () -> {
            GlDebug.pushGroup(0x7170005, "End Render Pass");
        });
        glRenderPass.end((GlCommandBuffer) commandBuffer);
        putGlCommand(commandBuffer, () -> {
            GlDebug.popGroup();
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
        requireGlCommandBuffer(commandBuffer, "dispatch");
        if (computePipeline == null) {
            throw new IllegalArgumentException("dispatch: computePipeline为null");
        }
        if (!(computePipeline instanceof GlComputePipeline)) {
            throw new IllegalArgumentException("dispatch: computePipeline类型错误: " + computePipeline.getClass().getName());
        }
        requirePositive(groupCountX, "dispatch", "groupCountX");
        requirePositive(groupCountY, "dispatch", "groupCountY");
        requirePositive(groupCountZ, "dispatch", "groupCountZ");
        putGlCommand(commandBuffer, () -> {
            GlDebug.pushGroup(0x7160001, "Compute");
            GlDebug.pushGroup(0x7160000, "Setup Compute Pipeline");
            if (computePipeline instanceof GlComputePipeline glPipeline) {
                glUseProgram((int) glPipeline.shader().handle());
                glPipeline.descriptorSet().update();
                glPipeline.descriptorSet().apply();
            }
            GlDebug.popGroup();

            glDispatchCompute(groupCountX, groupCountY, groupCountZ);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            GlDebug.popGroup();
        });
    }

    @Override
    public IDevice getDevice() {
        return device;
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
}
