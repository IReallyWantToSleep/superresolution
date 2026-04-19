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
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

public interface ICommandDecoder {
    ResourceStateTracker getStateTracker();

    void declareExternalResource(ITexture texture, ResourceAccessType currentState);

    void restoreExternalResource(ICommandBuffer commandBuffer, ITexture texture, ResourceAccessType targetState);

    void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color);

    void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth);

    void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil);

    void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel);

    void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    void setViewport(ICommandBuffer commandBuffer, float x, float y, float width, float height);

    void setScissor(ICommandBuffer commandBuffer, int x, int y, int width, int height);

    void setLineWidth(ICommandBuffer commandBuffer, float width);

    void setBlendConstants(ICommandBuffer commandBuffer, float r, float g, float b, float a);

    void beginRenderPass(ICommandBuffer commandBuffer, RenderPass renderPass);

    void endRenderPass(ICommandBuffer commandBuffer);

    void bindPipeline(ICommandBuffer commandBuffer, GraphicsPipeline pipeline);

    void bindPipeline(ICommandBuffer commandBuffer, ComputePipeline pipeline);

    void draw(ICommandBuffer commandBuffer, IVertexBuffer vertexBuffer, int vertexCount, int firstVertex);

    void dispatch(ICommandBuffer commandBuffer, int groupCountX, int groupCountY, int groupCountZ);

    void memoryBarrier(ICommandBuffer commandBuffer, MemoryBarrierType... barriers);

    IDevice getDevice();
}
