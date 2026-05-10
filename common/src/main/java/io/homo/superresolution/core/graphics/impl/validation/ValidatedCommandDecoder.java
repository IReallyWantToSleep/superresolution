/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.impl.validation;

import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.ColorAttachment;
import io.homo.superresolution.core.graphics.impl.framebuffer.DepthStencilAttachment;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public class ValidatedCommandDecoder implements ICommandDecoder {
    protected ICommandDecoder rawCommandDecoder;

    private RenderPass activeRenderPass = null;
    private GraphicsPipeline boundGraphicsPipeline = null;
    private ComputePipeline boundComputePipeline = null;

    public ValidatedCommandDecoder(ICommandDecoder rawCommandDecoder) {
        this.rawCommandDecoder = Objects.requireNonNull(rawCommandDecoder, "rawCommandDecoder cannot be null");
    }

    private static void requireRecording(ICommandBuffer commandBuffer, String action) {
        if (commandBuffer.state() != CommandBufferState.Recording) {
            throw new IllegalStateException(action + ": command buffer is not in Recording state (current: " + commandBuffer.state() + ")");
        }
    }

    private static void requireNonNull(Object obj, String action, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(action + ": " + name + " cannot be null");
        }
    }

    private static void requireNonNullTexture(ITexture texture, String action) {
        if (texture == null) {
            throw new IllegalArgumentException(action + ": texture cannot be null");
        }
    }

    private static void requireNonNullBuffer(IBuffer buffer, String action) {
        if (buffer == null) {
            throw new IllegalArgumentException(action + ": buffer cannot be null");
        }
    }

    private static void requireNonNegative(int value, String action, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(action + ": " + name + " cannot be negative");
        }
    }

    private static void requireNonNegative(long value, String action, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(action + ": " + name + " cannot be negative");
        }
    }

    private static void requirePositive(int value, String action, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(action + ": " + name + " must be positive");
        }
    }

    private static void requirePositive(long value, String action, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(action + ": " + name + " must be positive");
        }
    }

    private static void requireRangeInclusive(float value, float min, float max, String action, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(action + ": " + name + " out of range [" + min + "," + max + "]");
        }
    }

    private static void requireRangeInclusive(int value, int min, int max, String action, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(action + ": " + name + " out of range [" + min + "," + max + "]");
        }
    }

    private static void requireSameTextureType(ITexture a, ITexture b, String action) {
        if (a.getTextureType() != b.getTextureType()) {
            throw new IllegalArgumentException(action + ": source and destination texture types differ: "
                    + a.getTextureType() + " vs " + b.getTextureType());
        }
    }

    private static void requireSameTextureFormat(ITexture a, ITexture b, String action) {
        if (a.getTextureFormat() != b.getTextureFormat()) {
            throw new IllegalArgumentException(action + ": source and destination texture formats differ: "
                    + a.getTextureFormat() + " vs " + b.getTextureFormat());
        }
    }

    private static void requireValidMipLevel(ITexture tex, int level, String action, String levelName) {
        int maxLevels = tex.getMipmapSettings().resolveLevels(tex.getWidth(), tex.getHeight());
        if (level < 0 || level >= maxLevels) {
            throw new IllegalArgumentException(action + ": " + levelName + " out of range [0," + (maxLevels - 1) + "]");
        }
    }

    private static void requireTextureUsage(ITexture texture, TextureUsage requiredUsage, String action) {
        if (!texture.getTextureUsages().getUsages().contains(requiredUsage)) {
            throw new IllegalArgumentException(action + ": texture missing required usage " + requiredUsage);
        }
    }

    private static void requireBufferUsage(IBuffer buffer, BufferUsage requiredUsage, String action) {
        if (!buffer.getUsages().has(requiredUsage)) {
            throw new IllegalArgumentException(action + ": buffer missing required usage " + requiredUsage);
        }
    }

    private static int mipSize(int baseSize, int level) {
        int size = baseSize >> level;
        return Math.max(1, size);
    }

    @Override
    public ResourceStateTracker getStateTracker() {
        return rawCommandDecoder.getStateTracker();
    }

    @Override
    public void declareExternalResource(ITexture texture, ResourceAccessType currentState) {
        requireNonNullTexture(texture, "declareExternalResource");
        requireNonNull(currentState, "declareExternalResource", "currentState");
        rawCommandDecoder.declareExternalResource(texture, currentState);
    }

    @Override
    public void restoreExternalResource(ICommandBuffer commandBuffer, ITexture texture, ResourceAccessType targetState) {
        requireNonNull(commandBuffer, "restoreExternalResource", "commandBuffer");
        requireNonNullTexture(texture, "restoreExternalResource");
        requireNonNull(targetState, "restoreExternalResource", "targetState");
        requireRecording(commandBuffer, "restoreExternalResource");
        rawCommandDecoder.restoreExternalResource(commandBuffer, texture, targetState);
    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        requireNonNull(commandBuffer, "clearTextureRGBA", "commandBuffer");
        requireNonNullTexture(texture, "clearTextureRGBA");
        requireRecording(commandBuffer, "clearTextureRGBA");
        if (color == null || color.length == 0) {
            throw new IllegalArgumentException("clearTextureRGBA: color array is null or empty");
        }

        TextureFormat format = texture.getTextureFormat();
        if (format.isDepth() || format.isStencil()) {
            throw new IllegalArgumentException("clearTextureRGBA: texture format does not support color clear: " + format);
        }
        if (color.length != format.getChannelCount()) {
            throw new IllegalArgumentException("clearTextureRGBA: color component count (" + color.length
                    + ") does not match texture channel count (" + format.getChannelCount() + ")");
        }
        for (float comp : color) {
            requireRangeInclusive(comp, 0.0f, 1.0f, "clearTextureRGBA", "color component");
        }

        requireTextureUsage(texture, TextureUsage.TransferDestination, "clearTextureRGBA");

        rawCommandDecoder.clearTextureRGBA(commandBuffer, texture, color);
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        requireNonNull(commandBuffer, "clearTextureDepth", "commandBuffer");
        requireNonNullTexture(texture, "clearTextureDepth");
        requireRecording(commandBuffer, "clearTextureDepth");
        requireRangeInclusive(depth, 0.0f, 1.0f, "clearTextureDepth", "depth");

        if (!texture.getTextureFormat().isDepth()) {
            throw new IllegalArgumentException("clearTextureDepth: texture format does not support depth clear: "
                    + texture.getTextureFormat());
        }

        requireTextureUsage(texture, TextureUsage.TransferDestination, "clearTextureDepth");
        rawCommandDecoder.clearTextureDepth(commandBuffer, texture, depth);
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        requireNonNull(commandBuffer, "clearTextureStencil", "commandBuffer");
        requireNonNullTexture(texture, "clearTextureStencil");
        requireRecording(commandBuffer, "clearTextureStencil");
        requireRangeInclusive(stencil, 0, 255, "clearTextureStencil", "stencil");

        TextureFormat format = texture.getTextureFormat();
        if (!format.isStencil()) {
            throw new IllegalArgumentException("clearTextureStencil: texture format does not support stencil clear: " + format);
        }

        requireTextureUsage(texture, TextureUsage.TransferDestination, "clearTextureStencil");

        rawCommandDecoder.clearTextureStencil(commandBuffer, texture, stencil);
    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst,
                            int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel,
                            int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        requireNonNull(commandBuffer, "copyTexture", "commandBuffer");
        requireNonNullTexture(src, "copyTexture");
        requireNonNullTexture(dst, "copyTexture");
        requireRecording(commandBuffer, "copyTexture");

        requireSameTextureFormat(src, dst, "copyTexture");
        requireSameTextureType(src, dst, "copyTexture");

        requireValidMipLevel(src, srcLevel, "copyTexture", "srcLevel");
        requireValidMipLevel(dst, dstLevel, "copyTexture", "dstLevel");

        requireNonNegative(srcX0, "copyTexture", "srcX0");
        requireNonNegative(srcY0, "copyTexture", "srcY0");
        requireNonNegative(srcX1, "copyTexture", "srcX1");
        requireNonNegative(srcY1, "copyTexture", "srcY1");
        requireNonNegative(dstX0, "copyTexture", "dstX0");
        requireNonNegative(dstY0, "copyTexture", "dstY0");
        requireNonNegative(dstX1, "copyTexture", "dstX1");
        requireNonNegative(dstY1, "copyTexture", "dstY1");

        if (srcX1 <= srcX0 || dstX1 <= dstX0) {
            throw new IllegalArgumentException("copyTexture: invalid X range (srcX1 <= srcX0 or dstX1 <= dstX0)");
        }
        if (srcY1 <= srcY0 || dstY1 <= dstY0) {
            throw new IllegalArgumentException("copyTexture: invalid Y range (srcY1 <= srcY0 or dstY1 <= dstY0)");
        }

        int srcMaxX = mipSize(src.getWidth(), srcLevel);
        int srcMaxY = mipSize(src.getHeight(), srcLevel);
        int dstMaxX = mipSize(dst.getWidth(), dstLevel);
        int dstMaxY = mipSize(dst.getHeight(), dstLevel);

        if (srcX1 > srcMaxX || dstX1 > dstMaxX) {
            throw new IllegalArgumentException("copyTexture: X range exceeds texture size");
        }
        if (srcY1 > srcMaxY || dstY1 > dstMaxY) {
            throw new IllegalArgumentException("copyTexture: Y range exceeds texture size");
        }

        requireTextureUsage(src, TextureUsage.TransferSource, "copyTexture");
        requireTextureUsage(dst, TextureUsage.TransferDestination, "copyTexture");

        rawCommandDecoder.copyTexture(commandBuffer, src, dst,
                srcX0, srcY0, srcX1, srcY1, srcLevel,
                dstX0, dstY0, dstX1, dstY1, dstLevel);
    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst,
                           long srcOffset, long dstOffset, long size) {
        requireNonNull(commandBuffer, "copyBuffer", "commandBuffer");
        requireNonNullBuffer(src, "copyBuffer");
        requireNonNullBuffer(dst, "copyBuffer");
        requireRecording(commandBuffer, "copyBuffer");
        requireNonNegative(srcOffset, "copyBuffer", "srcOffset");
        requireNonNegative(dstOffset, "copyBuffer", "dstOffset");
        requirePositive(size, "copyBuffer", "size");

        if (srcOffset + size > src.getSize()) {
            throw new IllegalArgumentException("copyBuffer: source range exceeds buffer size");
        }
        if (dstOffset + size > dst.getSize()) {
            throw new IllegalArgumentException("copyBuffer: destination range exceeds buffer size");
        }

        requireBufferUsage(src, BufferUsage.TransferSrc, "copyBuffer");
        requireBufferUsage(dst, BufferUsage.TransferDst, "copyBuffer");

        rawCommandDecoder.copyBuffer(commandBuffer, src, dst, srcOffset, dstOffset, size);
    }

    @Override
    public void writeToBuffer(ICommandBuffer commandBuffer, IBuffer dst, long dstOffset, long size, ByteBuffer data) {
        requireNonNull(commandBuffer, "writeToBuffer", "commandBuffer");
        requireNonNullBuffer(dst, "writeToBuffer");
        requireRecording(commandBuffer, "writeToBuffer");
        requireNonNegative(dstOffset, "writeToBuffer", "dstOffset");
        if (data == null) {
            throw new IllegalArgumentException("writeToBuffer: data cannot be null");
        }
        if (size <= 0) {
            return;
        }
        if (dstOffset + size > dst.getSize()) {
            throw new IllegalArgumentException("writeToBuffer: write range exceeds buffer size");
        }

        if (!dst.getUsages().has(BufferUsage.TransferDst)) {
            throw new IllegalArgumentException("writeToBuffer: destination buffer must have TransferDst usage");
        }

        rawCommandDecoder.writeToBuffer(commandBuffer, dst, dstOffset, size, data);
    }

    @Override
    public void writeToTexture(ICommandBuffer commandBuffer,ITexture texture, ByteBuffer data, int x, int y, int width, int height, int mipLevel) {
        requireNonNull(commandBuffer, "writeToTexture", "commandBuffer");
        requireNonNullTexture(texture, "writeToTexture");
        if (data == null) {
            throw new IllegalArgumentException("writeToTexture: data cannot be null");
        }
        requireValidMipLevel(texture, mipLevel, "writeToTexture", "mipLevel");
        requireNonNegative(x, "writeToTexture", "x");
        requireNonNegative(y, "writeToTexture", "y");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("writeToTexture: width and height must be positive");
        }

        int maxX = mipSize(texture.getWidth(), mipLevel);
        int maxY = mipSize(texture.getHeight(), mipLevel);
        if (x + width > maxX || y + height > maxY) {
            throw new IllegalArgumentException("writeToTexture: write region exceeds texture dimensions");
        }

        requireTextureUsage(texture, TextureUsage.TransferDestination, "writeToTexture");

        rawCommandDecoder.writeToTexture(commandBuffer,texture, data, x, y, width, height, mipLevel);
    }

    @Override
    public void setViewport(ICommandBuffer commandBuffer, float x, float y, float width, float height) {
        requireNonNull(commandBuffer, "setViewport", "commandBuffer");
        requireRecording(commandBuffer, "setViewport");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("setViewport: width and height must be positive");
        }
        rawCommandDecoder.setViewport(commandBuffer, x, y, width, height);
    }

    @Override
    public void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height) {
        requireNonNull(commandBuffer, "setScissor", "commandBuffer");
        requireRecording(commandBuffer, "setScissor");
        requireNonNegative(x, "setScissor", "x");
        requireNonNegative(y, "setScissor", "y");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("setScissor: width/height cannot be negative");
        }
        rawCommandDecoder.setScissor(commandBuffer, x, y, width, height);
    }

    @Override
    public void setLineWidth(ICommandBuffer commandBuffer, float width) {
        requireNonNull(commandBuffer, "setLineWidth", "commandBuffer");
        requireRecording(commandBuffer, "setLineWidth");
        if (width <= 0) {
            throw new IllegalArgumentException("setLineWidth: width must be positive");
        }
        rawCommandDecoder.setLineWidth(commandBuffer, width);
    }

    @Override
    public void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a) {
        requireNonNull(commandBuffer, "setBlendConstants", "commandBuffer");
        requireRecording(commandBuffer, "setBlendConstants");
        requireRangeInclusive(r, 0.0f, 1.0f, "setBlendConstants", "r");
        requireRangeInclusive(g, 0.0f, 1.0f, "setBlendConstants", "g");
        requireRangeInclusive(b, 0.0f, 1.0f, "setBlendConstants", "b");
        requireRangeInclusive(a, 0.0f, 1.0f, "setBlendConstants", "a");
        rawCommandDecoder.setBlendConstants(commandBuffer, r, g, b, a);
    }
    @Override
    public void beginRenderPass(ICommandBuffer commandBuffer, RenderPass renderPass) {
        requireNonNull(commandBuffer, "beginRenderPass", "commandBuffer");
        requireNonNull(renderPass, "beginRenderPass", "renderPass");
        requireRecording(commandBuffer, "beginRenderPass");

        if (activeRenderPass != null) {
            throw new IllegalStateException("beginRenderPass: a render pass is already active. Call endRenderPass() first.");
        }

        IFrameBuffer fb = renderPass.frameBuffer();
        if (fb != null) {
            for (ColorAttachment attachment : fb.getColorAttachments()) {
                ITexture texture = attachment.texture();
                requireTextureUsage(texture, TextureUsage.AttachmentColor, "beginRenderPass");
            }
            DepthStencilAttachment dsAttachment = fb.getDepthStencilAttachment();
            if (dsAttachment != null) {
                ITexture texture = dsAttachment.texture();
                requireTextureUsage(texture, TextureUsage.AttachmentDepth, "beginRenderPass");
            }
        }

        activeRenderPass = renderPass;
        boundGraphicsPipeline = null;
        boundComputePipeline = null;

        rawCommandDecoder.beginRenderPass(commandBuffer, renderPass);
    }

    @Override
    public void endRenderPass(ICommandBuffer commandBuffer) {
        requireNonNull(commandBuffer, "endRenderPass", "commandBuffer");
        requireRecording(commandBuffer, "endRenderPass");

        if (activeRenderPass == null) {
            throw new IllegalStateException("endRenderPass: no active render pass to end.");
        }

        activeRenderPass = null;
        boundGraphicsPipeline = null;

        rawCommandDecoder.endRenderPass(commandBuffer);
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, GraphicsPipeline pipeline) {
        requireNonNull(commandBuffer, "bindPipeline(graphics)", "commandBuffer");
        requireNonNull(pipeline, "bindPipeline(graphics)", "pipeline");
        requireRecording(commandBuffer, "bindPipeline(graphics)");

        if (activeRenderPass == null) {
            throw new IllegalStateException("bindPipeline(graphics): no active render pass. Call beginRenderPass() first.");
        }
        if (pipeline.renderPass() != activeRenderPass) {
            throw new IllegalStateException("bindPipeline(graphics): pipeline's render pass does not match the active render pass.");
        }

        validateDescriptorBindings(commandBuffer, pipeline.descriptorSet(), pipeline);

        boundGraphicsPipeline = pipeline;
        boundComputePipeline = null;

        rawCommandDecoder.bindPipeline(commandBuffer, pipeline);
    }

    @Override
    public void bindPipeline(ICommandBuffer commandBuffer, ComputePipeline pipeline) {
        requireNonNull(commandBuffer, "bindPipeline(compute)", "commandBuffer");
        requireNonNull(pipeline, "bindPipeline(compute)", "pipeline");
        requireRecording(commandBuffer, "bindPipeline(compute)");

        if (activeRenderPass != null) {
            throw new IllegalStateException("bindPipeline(compute): cannot bind compute pipeline inside a render pass.");
        }

        validateDescriptorBindings(commandBuffer, pipeline.descriptorSet(), pipeline);

        boundComputePipeline = pipeline;
        boundGraphicsPipeline = null;

        rawCommandDecoder.bindPipeline(commandBuffer, pipeline);
    }

    @Override
    public void draw(ICommandBuffer commandBuffer, IVertexBuffer vertexBuffer, int vertexCount, int firstVertex) {
        requireNonNull(commandBuffer, "draw", "commandBuffer");
        requireNonNull(vertexBuffer, "draw", "vertexBuffer");
        requireRecording(commandBuffer, "draw");
        requirePositive(vertexCount, "draw", "vertexCount");
        requireNonNegative(firstVertex, "draw", "firstVertex");

        if (activeRenderPass == null) {
            throw new IllegalStateException("draw: no active render pass. Call beginRenderPass() first.");
        }
        if (boundGraphicsPipeline == null) {
            throw new IllegalStateException("draw: no graphics pipeline bound. Call bindPipeline(graphics) first.");
        }

        if (firstVertex + vertexCount > vertexBuffer.getVertexCount()) {
            throw new IllegalArgumentException("draw: vertex range exceeds vertex buffer size");
        }


        rawCommandDecoder.draw(commandBuffer, vertexBuffer, vertexCount, firstVertex);
    }

    @Override
    public void dispatch(ICommandBuffer commandBuffer, int groupCountX, int groupCountY, int groupCountZ) {
        requireNonNull(commandBuffer, "dispatch", "commandBuffer");
        requireRecording(commandBuffer, "dispatch");
        requirePositive(groupCountX, "dispatch", "groupCountX");
        requirePositive(groupCountY, "dispatch", "groupCountY");
        requirePositive(groupCountZ, "dispatch", "groupCountZ");

        if (activeRenderPass != null) {
            throw new IllegalStateException("dispatch: cannot dispatch compute work inside a render pass.");
        }
        if (boundComputePipeline == null) {
            throw new IllegalStateException("dispatch: no compute pipeline bound. Call bindPipeline(compute) first.");
        }

        rawCommandDecoder.dispatch(commandBuffer, groupCountX, groupCountY, groupCountZ);
    }

    @Override
    public void memoryBarrier(ICommandBuffer commandBuffer, MemoryBarrierType... barriers) {
        requireNonNull(commandBuffer, "memoryBarrier", "commandBuffer");
        requireRecording(commandBuffer, "memoryBarrier");
        if (barriers == null || barriers.length == 0) {
            throw new IllegalArgumentException("memoryBarrier: at least one barrier must be specified");
        }
        rawCommandDecoder.memoryBarrier(commandBuffer, barriers);
    }

    @Override
    public IDevice getDevice() {
        return rawCommandDecoder.getDevice();
    }

    private void validateDescriptorBindings(ICommandBuffer commandBuffer,
                                            PipelineDescriptorSet descriptorSet,
                                            Object pipeline) {
        Map<String, PipelineDescriptorSet.ResourceBinding> bindings = descriptorSet.getBindings();
        if (bindings.isEmpty()) return;

        for (Map.Entry<String, PipelineDescriptorSet.ResourceBinding> entry : bindings.entrySet()) {
            String name = entry.getKey();
            PipelineDescriptorSet.ResourceBinding binding = entry.getValue();
            if (binding.resource() instanceof ITexture texture) {
                if (binding.type() == PipelineDescriptorSet.ResourceType.SAMPLER_TEXTURE) {
                    requireTextureUsage(texture, TextureUsage.Sampler, "bindPipeline");
                } else if (binding.type() == PipelineDescriptorSet.ResourceType.STORAGE_IMAGE) {
                    requireTextureUsage(texture, TextureUsage.Storage, "bindPipeline");
                }
            } else if (binding.resource() instanceof IBuffer buffer) {
                if (binding.type() == PipelineDescriptorSet.ResourceType.UNIFORM_BUFFER) {
                    requireBufferUsage(buffer, BufferUsage.Ubo, "bindPipeline");
                }
            }
        }
    }
}