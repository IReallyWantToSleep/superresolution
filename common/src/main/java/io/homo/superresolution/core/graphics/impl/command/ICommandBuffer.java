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
import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

import java.nio.ByteBuffer;

public interface ICommandBuffer {
    void begin();

    void end();

    void reset();

    void destroy();

    void submit(IDevice device);

    IDevice getDevice();

    ICommandDecoder decoder();

    ICommandPool ownerPool();

    CommandBufferState state();

    default boolean isInFlight() {
        return state() == CommandBufferState.Pending;
    }

    default boolean isFenceSignaled() {
        return !isInFlight();
    }

    default void waitForFence() { /* noop */ }

    CommandBufferBehavior behavior();


    default void clearTextureRGBA(ITexture texture, float[] color) {
        decoder().clearTextureRGBA(this, texture, color);
    }

    default void clearTextureStencil(ITexture texture, int stencil){
        decoder().clearTextureStencil(this, texture, stencil);
    }


    default void clearTextureDepth(ITexture texture, float depth){
        decoder().clearTextureDepth(this, texture, depth);
    }


    default void copyTexture(ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        decoder().copyTexture(this, src, dst, srcX0, srcY0, srcX1, srcY1, srcLevel, dstX0, dstY0, dstX1, dstY1, dstLevel);
    }

    default void writeToBuffer(IBuffer dst, long dstOffset, ByteBuffer data){
        writeToBuffer(dst, dstOffset, data.remaining(), data);
    }

    default void writeToBuffer(IBuffer dst, long dstOffset, long size, ByteBuffer data){
        decoder().writeToBuffer(this, dst, dstOffset, size, data);
    }

    default void writeToBuffer(IBuffer dst, long dstOffset, IBufferData data){
        writeToBuffer(dst, dstOffset, data.asByteBuffer());
    }

    default void writeToBuffer(IBuffer dst, long dstOffset, long size, IBufferData data){
        writeToBuffer(dst, dstOffset, size, data.asByteBuffer());
    }

    default void setViewport(float x, float y, float width, float height){
        decoder().setViewport(this, x, y, width, height);
    }

    default void setScissor(int x, int y, int width, int height){
        decoder().setScissor(this, x, y, width, height);
    }

    default void setLineWidth(float width){
        decoder().setLineWidth(this, width);
    }

    default void setBlendConstants(float r, float g, float b, float a){
        decoder().setBlendConstants(this, r, g, b, a);
    }
    default void beginRenderPass(RenderPass renderPass){
        decoder().beginRenderPass(this, renderPass);
    }

    default void endRenderPass(){
        decoder().endRenderPass(this);
    }

    default void bindPipeline(GraphicsPipeline pipeline){
        decoder().bindPipeline(this, pipeline);
    }

    default void bindPipeline(ComputePipeline pipeline){
        decoder().bindPipeline(this, pipeline);
    }

    default void draw(IVertexBuffer vertexBuffer, int vertexCount, int firstVertex){
        decoder().draw(this, vertexBuffer, vertexCount, firstVertex);
    }

    default void dispatch(int groupCountX, int groupCountY, int groupCountZ){
        decoder().dispatch(this, groupCountX, groupCountY, groupCountZ);
    }

    default void memoryBarrier(MemoryBarrierType... barriers){
        decoder().memoryBarrier(this, barriers);
    }
}

