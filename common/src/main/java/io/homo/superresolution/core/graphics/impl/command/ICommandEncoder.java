/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderState;

public interface ICommandEncoder {
    ICommandEncoder begin();

    void clearTextureRGBA(ITexture texture, float[] color);

    void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color);

    void clearTextureDepth(ITexture texture, float depth);

    void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth);

    void clearTextureStencil(ITexture texture, int stencil);

    void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil);

    void copyTexture(ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel);

    void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel);

    default void copyTexture(ITexture src, ITexture dst) {
        copyTexture(
                src,
                dst,
                0, 0, src.getWidth(), src.getHeight(), 0,
                0, 0, dst.getWidth(), dst.getHeight(), 0
        );
    }


    void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    void draw(IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount);

    void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount);


    void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z);

    void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z);


    ICommandBuffer end();

    IRenderState renderState();

    IDevice getDevice();

    ICommandBuffer getCommandBuffer();
}
