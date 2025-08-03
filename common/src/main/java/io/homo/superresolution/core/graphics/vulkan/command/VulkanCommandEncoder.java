package io.homo.superresolution.core.graphics.vulkan.command;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderState;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;

public class VulkanCommandEncoder implements ICommandEncoder {
    private VulkanCommandBuffer currentCommandBuffer;

    private VulkanDevice vulkanDevice;

    public VulkanCommandEncoder(VulkanDevice vulkanDevice) {
        this.vulkanDevice = vulkanDevice;
    }

    @Override
    public ICommandEncoder begin() {
        currentCommandBuffer = new VulkanCommandBuffer((VulkanDevice) getDevice());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VK_CHECK(VK10.vkBeginCommandBuffer(currentCommandBuffer.getNativeCommandBuffer(), beginInfo));
        }
        return this;
    }

    @Override
    public void clearTextureRGBA(ITexture texture, float[] color) {

    }

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {

    }

    @Override
    public void clearTextureDepth(ITexture texture, float depth) {

    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {

    }

    @Override
    public void clearTextureStencil(ITexture texture, int stencil) {

    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {

    }

    @Override
    public void copyTexture(ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {

    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {

    }

    @Override
    public void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {

    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {

    }

    @Override
    public void draw(IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {

    }

    @Override
    public void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {

    }

    @Override
    public void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z) {

    }

    @Override
    public void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z) {

    }

    @Override
    public ICommandBuffer end() {
        VulkanCommandBuffer cmdBuf = currentCommandBuffer;
        this.currentCommandBuffer = null;
        VK_CHECK(VK10.vkEndCommandBuffer(cmdBuf.getNativeCommandBuffer()));
        return cmdBuf;
    }

    @Override
    public IRenderState renderState() {
        return null;
    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }

    @Override
    public ICommandBuffer getCommandBuffer() {
        return currentCommandBuffer;
    }
}
