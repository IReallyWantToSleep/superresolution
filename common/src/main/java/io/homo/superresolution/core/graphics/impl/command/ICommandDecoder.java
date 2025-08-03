package io.homo.superresolution.core.graphics.impl.command;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.commands.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderState;

public interface ICommandDecoder {
    void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color);

    void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth);

    void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil);

    void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel);

    void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount);

    void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z);


    default void decodeClearTexture(ICommandBuffer commandBuffer, ClearCommand command) {
        switch (command.clearMode) {
            case 0:
                clearTextureRGBA(commandBuffer, command.target, command.colorRGBA);
            case 1:
                clearTextureDepth(commandBuffer, command.target, command.depth);
            case 2:
                clearTextureStencil(commandBuffer, command.target, command.stencil);
        }
    }


    default void decodeCopyTexture(ICommandBuffer commandBuffer, CopyTextureCommand command) {
        copyTexture(
                commandBuffer,
                command.source,
                command.destination,
                command.sourceDimensions.x,
                command.sourceDimensions.y,
                command.sourceDimensions.z,
                command.sourceDimensions.w,
                command.sourceLevel,
                command.destinationDimensions.x,
                command.destinationDimensions.y,
                command.destinationDimensions.z,
                command.destinationDimensions.w,
                command.destinationLevel

        );
    }

    default void decodeCopyBuffer(ICommandBuffer commandBuffer, CopyBufferCommand command) {
        copyBuffer(
                commandBuffer,
                command.source,
                command.destination,
                command.srcOffset,
                command.dstOffset,
                command.size
        );
    }

    default void decodeDraw(ICommandBuffer commandBuffer, DrawCommand command) {
        applyRenderState(commandBuffer, command.stateSnapshot);
        draw(
                commandBuffer,
                command.program,
                command.frameBuffer,
                command.drawObject,
                command.firstVertex,
                command.vertexCount
        );
    }

    default void decodeDispatchCompute(ICommandBuffer commandBuffer, ComputeCommand command) {
        dispatchCompute(
                commandBuffer,
                command.shaderProgram,
                command.workGroupSize.x,
                command.workGroupSize.y,
                command.workGroupSize.z
        );
    }

    /**
     * 输入渲染状态快照，CommandDecoder会应用此渲染状态
     *
     * @param stateSnapshot 渲染状态快照
     */
    void applyRenderState(ICommandBuffer commandBuffer, IRenderState.StateSnapshot stateSnapshot);

    default void decodeCommand(ICommandBuffer commandBuffer, GpuCommand command) {
        switch (command.getCommandType()) {
            case Draw -> decodeDraw(commandBuffer, (DrawCommand) command);
            case Clear -> decodeClearTexture(commandBuffer, (ClearCommand) command);
            case Compute -> decodeDispatchCompute(commandBuffer, (ComputeCommand) command);
            case CopyBuffer -> decodeCopyBuffer(commandBuffer, (CopyBufferCommand) command);
            case CopyTexture -> decodeCopyTexture(commandBuffer, (CopyTextureCommand) command);

        }
    }

    IDevice getDevice();
}
