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

package io.homo.superresolution.core.graphics.impl.command;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;

public interface ICommandDecoder {
    void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color);

    void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth);

    void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil);

    void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel);

    void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    void setViewport(ICommandBuffer commandBuffer, float x, float y, float width, float height);

    void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height);

    void setLineWidth(ICommandBuffer commandBuffer, float width);

    void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a);

    void draw(ICommandBuffer commandBuffer, RenderPass renderPass, PrimitiveType primitiveType, IVertexBuffer vertexBuffer, int vertexCount, int firstVertex);

    void dispatch(ICommandBuffer commandBuffer, ComputePipeline computePipeline, int groupCountX, int groupCountY, int groupCountZ);


    default void clearTextureRGBA(ITexture texture, float[] color) {
        clearTextureRGBA(currentCommandBuffer(), texture, color);
    }

    default void clearTextureDepth(ITexture texture, float depth){
        clearTextureDepth(currentCommandBuffer(), texture, depth);
    }

    default void clearTextureStencil(ITexture texture, int stencil) {
        clearTextureStencil(currentCommandBuffer(), texture, stencil);
    }

    default void copyTexture(ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        copyTexture(currentCommandBuffer(), src, dst, srcX0, srcY0, srcX1, srcY1, srcLevel, dstX0, dstY0, dstX1, dstY1, dstLevel);
    }

    default void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
        copyBuffer(currentCommandBuffer(), src, dst, srcOffset, dstOffset, size);
    }

    default void setViewport(float x, float y, float width, float height) {
        setViewport(currentCommandBuffer(), x, y, width, height);
    }

    default void setScissor(int x, int y, int width, int height) {
        setScissor(currentCommandBuffer(), x, y, width, height);
    }

    default void setLineWidth(float width) {
        setLineWidth(currentCommandBuffer(), width);
    }

    default void setBlendConstants(float r, float g, float b, float a) {
        setBlendConstants(currentCommandBuffer(), r, g, b, a);
    }

    default void draw(RenderPass renderPass, PrimitiveType primitiveType, IVertexBuffer vertexBuffer, int vertexCount, int firstVertex) {
        draw(currentCommandBuffer(), renderPass, primitiveType, vertexBuffer, vertexCount, firstVertex);
    }

    default void dispatch(ComputePipeline computePipeline, int groupCountX, int groupCountY, int groupCountZ) {
        dispatch(currentCommandBuffer(), computePipeline, groupCountX, groupCountY, groupCountZ);
    }

    ICommandBuffer beginCommandBuffer();

    ICommandBuffer endCommandBuffer();

    ICommandBuffer endAndSubmitCommandBuffer();

    ICommandBuffer currentCommandBuffer();

    IDevice getDevice();
}
