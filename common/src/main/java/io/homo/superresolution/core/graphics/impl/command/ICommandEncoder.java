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
