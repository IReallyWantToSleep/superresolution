package io.homo.superresolution.core.graphics.vulkan.command;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;

public class VulkanCommandDecoder implements ICommandDecoder {
    public VulkanCommandDecoder(VulkanDevice vulkanDevice) {
        this.vulkanDevice = vulkanDevice;
    }

    private VulkanDevice vulkanDevice;

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {

    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {

    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {

    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {

    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {

    }

    @Override
    public void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {

    }

    @Override
    public void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z) {

    }

    @Override
    public void applyRenderState(ICommandBuffer commandBuffer, IRenderState.StateSnapshot stateSnapshot) {

    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }
}
