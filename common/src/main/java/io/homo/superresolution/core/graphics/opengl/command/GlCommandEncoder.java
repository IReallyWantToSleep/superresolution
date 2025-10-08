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

package io.homo.superresolution.core.graphics.opengl.command;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.command.commands.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.GlRenderState;
import io.homo.superresolution.core.graphics.system.IRenderState;
import org.joml.Vector3i;
import org.joml.Vector4i;

public class GlCommandEncoder implements ICommandEncoder {
    private GlCommandBuffer commandBuffer;
    private final GlDevice device;
    private final GlRenderState renderState;

    public GlCommandEncoder(GlDevice device) {
        this.device = device;
        this.renderState = new GlRenderState();
    }

    @Override
    public ICommandEncoder begin() {
        if (this.commandBuffer != null) {
            SuperResolution.LOGGER.error("The GlCommandEncoder is recording commands, but it is calling the begin method.");
            //throw new IllegalStateException();
        }
        this.commandBuffer = new GlCommandBuffer((GlDevice) getDevice());
        return this;
    }

    @Override
    public void clearTextureRGBA(ITexture texture, float[] color) {
        clearTextureRGBA(commandBuffer, texture, color);
    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        ClearCommand command = new ClearCommand(texture);
        command.clearMode = 0;
        command.colorRGBA[0] = color[0];
        command.colorRGBA[1] = color[1];
        command.colorRGBA[2] = color[2];
        command.colorRGBA[3] = color[3];
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public void clearTextureDepth(ITexture texture, float depth) {
        clearTextureDepth(commandBuffer, texture, depth);
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        ClearCommand command = new ClearCommand(texture);
        command.clearMode = 1;
        command.depth = depth;
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);

    }

    @Override
    public void clearTextureStencil(ITexture texture, int stencil) {
        clearTextureStencil(commandBuffer, texture, stencil);
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        ClearCommand command = new ClearCommand(texture);
        command.clearMode = 2;
        command.stencil = stencil;
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public void copyTexture(ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        copyTexture(commandBuffer, src, dst, srcX0, srcY0, srcX1, srcY1, srcLevel, dstX0, dstY0, dstX1, dstY1, dstLevel);
    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        CopyTextureCommand command = new CopyTextureCommand(src, dst);
        command.sourceDimensions = new Vector4i(srcX0, srcY0, srcX1, srcY1);
        command.destinationDimensions = new Vector4i(dstX0, dstY0, dstX1, dstY1);
        command.sourceLevel = srcLevel;
        command.destinationLevel = dstLevel;
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
        copyBuffer(commandBuffer, src, dst, srcOffset, dstOffset, size);
    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
        CopyBufferCommand command = new CopyBufferCommand(src, dst);
        command.srcOffset = srcOffset;
        command.dstOffset = dstOffset;
        command.size = size;
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public void draw(IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {
        draw(commandBuffer, shaderProgram, frameBuffer, drawObject, firstVertex, vertexCount);
    }

    @Override
    public void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {
        DrawCommand command = new DrawCommand(shaderProgram);
        command.frameBuffer = frameBuffer;
        command.drawObject = drawObject;
        command.firstVertex = firstVertex;
        command.vertexCount = vertexCount;
        command.stateSnapshot = renderState.get();
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z) {
        dispatchCompute(commandBuffer, shaderProgram, x, y, z);
    }

    @Override
    public void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z) {
        ComputeCommand command = new ComputeCommand(shaderProgram, new Vector3i(x, y, z));
        commandBuffer.addCommand((GlCommandDecoder) getDevice().commandDecoder(), command);
    }

    @Override
    public ICommandBuffer end() {
        ICommandBuffer cmdBuf = commandBuffer;
        this.commandBuffer = null;
        return cmdBuf;
    }

    @Override
    public IRenderState renderState() {
        return renderState;
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    @Override
    public ICommandBuffer getCommandBuffer() {
        return commandBuffer;
    }
}
