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
import io.homo.superresolution.core.graphics.vulkan.texture.VulkanTexture;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class VulkanCommandDecoder implements ICommandDecoder {
    public VulkanCommandDecoder(VulkanDevice vulkanDevice) {
        this.vulkanDevice = vulkanDevice;
    }

    private VulkanDevice vulkanDevice;

    @Override
    public void clearTextureRGBA(ICommandBuffer commandBuffer, ITexture texture, float[] color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            clearColor.float32(0, color[0]);
            clearColor.float32(1, color[1]);
            clearColor.float32(2, color[2]);
            clearColor.float32(3, color[3]);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);
            vkCmdClearColorImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearColor,
                    range
            );
        }
    }

    @Override
    public void clearTextureDepth(ICommandBuffer commandBuffer, ITexture texture, float depth) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearDepthStencilValue clearDepth = VkClearDepthStencilValue.calloc(stack);
            clearDepth.depth(depth);
            clearDepth.stencil(0);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);

            vkCmdClearDepthStencilImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearDepth,
                    range
            );
        }
    }

    @Override
    public void clearTextureStencil(ICommandBuffer commandBuffer, ITexture texture, int stencil) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkClearDepthStencilValue clearStencil = VkClearDepthStencilValue.calloc(stack);
            clearStencil.depth(1.0f);
            clearStencil.stencil(stencil);
            VulkanTexture vulkanTexture = (VulkanTexture) texture;
            long imageHandle = vulkanTexture.handle();
            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();
            VkImageSubresourceRange range = VkImageSubresourceRange.calloc(stack);
            range.aspectMask(VK_IMAGE_ASPECT_STENCIL_BIT);
            range.baseMipLevel(0);
            range.levelCount(1);
            range.baseArrayLayer(0);
            range.layerCount(1);

            vkCmdClearDepthStencilImage(
                    commandBufferHandle,
                    imageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    clearStencil,
                    range
            );
        }
    }

    @Override
    public void copyTexture(ICommandBuffer commandBuffer, ITexture src, ITexture dst, int srcX0, int srcY0, int srcX1, int srcY1, int srcLevel, int dstX0, int dstY0, int dstX1, int dstY1, int dstLevel) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanTexture srcTexture = (VulkanTexture) src;
            VulkanTexture dstTexture = (VulkanTexture) dst;

            VkImageCopy.Buffer copyRegion = VkImageCopy.calloc(1, stack);
            copyRegion.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            copyRegion.srcSubresource().mipLevel(srcLevel);
            copyRegion.srcSubresource().baseArrayLayer(0);
            copyRegion.srcSubresource().layerCount(1);
            copyRegion.srcOffset().set(srcX0, srcY0, 0);
            copyRegion.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            copyRegion.dstSubresource().mipLevel(dstLevel);
            copyRegion.dstSubresource().baseArrayLayer(0);
            copyRegion.dstSubresource().layerCount(1);
            copyRegion.dstOffset().set(dstX0, dstY0, 0);

            VulkanCommandBuffer vulkanCommandBuffer = (VulkanCommandBuffer) commandBuffer;
            VkCommandBuffer commandBufferHandle = vulkanCommandBuffer.getNativeCommandBuffer();

            vkCmdCopyImage(
                    commandBufferHandle,
                    srcTexture.handle(),
                    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    dstTexture.handle(),
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    copyRegion
            );
        }
    }

    @Override
    public void copyBuffer(ICommandBuffer commandBuffer, IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size) {
    }

    @Override
    public void dispatchCompute(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, int x, int y, int z) {

    }


    @Override
    public void draw(ICommandBuffer commandBuffer, IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount) {

    }

    @Override
    public void applyRenderState(ICommandBuffer commandBuffer, IRenderState.StateSnapshot stateSnapshot) {

    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }
}
