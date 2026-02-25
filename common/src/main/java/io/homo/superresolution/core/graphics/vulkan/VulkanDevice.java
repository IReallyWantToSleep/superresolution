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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandPool;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;

public class VulkanDevice implements IDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanDevice.class);
    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final VulkanQueue mainQueue;
    private final VulkanCommandPool commandManager;
    private final VulkanCommandPool defaultCommandPool;
    private final VulkanCommandDecoder commandDecoder;


    public VulkanDevice(VkPhysicalDevice physicalDevice, VkDevice device, int graphicsQueueFamilyIndex) {
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.mainQueue = new VulkanQueue(this, graphicsQueueFamilyIndex);
        this.commandManager = new VulkanCommandPool(this, java.util.EnumSet.of(CommandPoolFlags.Reset));
        this.defaultCommandPool = commandManager;
        this.commandDecoder = new VulkanCommandDecoder(this);
    }

    @Override
    public ITexture createTexture(TextureDescription description) {
        return new VulkanTexture(this, description);
    }

    @Override
    public IShaderProgram createShaderProgram(ShaderDescription description) {
        return null;
    }

    @Override
    public IVertexBuffer createVertexBuffer(VertexBufferDescription description) {
        return null;
    }

    @Override
    public IBuffer createBuffer(BufferDescription description) {
        return null;
    }

    @Override
    public RenderPass createRenderPass(RenderPass.Builder builder) {
        return null;
    }

    @Override
    public PipelineDescriptorSet createDescriptorSet(IShaderProgram shader) {
        return null;
    }

    @Override
    public ComputePipeline createComputePipeline(ComputePipeline.Builder builder) {
        return null;
    }

    @Override
    public GraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder) {
        return null;
    }

    @Override
    public VulkanCommandBuffer createCommandBuffer() {
        return defaultCommandPool.createCommandBuffer();
    }

    @Override
    public VulkanCommandPool createCommandPool(CommandPoolFlags... flags) {
        java.util.EnumSet<CommandPoolFlags> poolFlags = java.util.EnumSet.noneOf(CommandPoolFlags.class);
        if (flags != null) {
            java.util.Collections.addAll(poolFlags, flags);
        }
        VulkanCommandPool pool = new VulkanCommandPool(this, poolFlags);
        pool.init();
        return pool;
    }

    @Override
    public ICommandPool defaultCommandPool() {
        return defaultCommandPool;
    }

    @Override
    public ICommandDecoder commandDecoder() {
        return commandDecoder;
    }

    @Override
    public void submitCommandBuffer(ICommandBuffer commandBuffer) {
        VulkanCommandBuffer vkCommandBuffer = (VulkanCommandBuffer) commandBuffer;
        submitCommandBuffer(vkCommandBuffer, null, null, null);
    }

    public VulkanTexture createTextureExt(
            TextureDescription description,
            boolean isExternal,
            long memoryHandle,
            boolean exportable
    ) {
        return new VulkanTexture(
                this,
                description,
                isExternal,
                memoryHandle,
                exportable
        );
    }

    public VulkanTexture createTextureExportable(
            TextureDescription description
    ) {
        return createTextureExt(
                description,
                false,
                0,
                true
        );
    }

    public VulkanTexture createTextureExternal(
            TextureDescription description,
            long memoryHandle
    ) {
        return createTextureExt(
                description,
                true,
                memoryHandle,
                false
        );
    }

    public ITexture createTextureFromHandle(TextureDescription description, long memory) {
        return new VulkanTexture(this, description, memory);
    }

    public long submitCommandBuffer(
            VulkanCommandBuffer commandBuffer,
            long[] waitSemaphores,
            int[] waitDstStageMask,
            long[] signalSemaphores
    ) {
        if (waitSemaphores != null && waitDstStageMask != null && waitSemaphores.length != waitDstStageMask.length) {
            throw new IllegalArgumentException("waitSemaphores and waitDstStageMask length mismatch");
        }

        long fence = commandBuffer.prepareFenceForSubmit();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer.getNativeCommandBuffer().address()));

            if (waitSemaphores != null && waitSemaphores.length > 0) {
                submitInfo.waitSemaphoreCount(waitSemaphores.length);
                submitInfo.pWaitSemaphores(stack.longs(waitSemaphores));
                submitInfo.pWaitDstStageMask(stack.ints(waitDstStageMask));
            }
            if (signalSemaphores != null && signalSemaphores.length > 0) {
                submitInfo.pSignalSemaphores(stack.longs(signalSemaphores));
            }

            VK_CHECK(vkQueueSubmit(mainQueue.getQueue(), submitInfo, fence));
            commandBuffer.markSubmitted();
        }
        return fence;
    }

    public void submitCommandBuffer(VulkanCommandBuffer commandBuffer) {
        long fence = commandBuffer.prepareFenceForSubmit();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(
                            stack.pointers(
                                    commandBuffer
                                            .getNativeCommandBuffer()
                                            .address()
                            )
                    );
            VK_CHECK(vkQueueSubmit(mainQueue.getQueue(), submitInfo, fence));
            commandBuffer.markSubmitted();
        }
    }

    /**
     * 获取VulkanCommandManager实例
     *
     * @return VulkanCommandManager实例
     */
    public VulkanCommandPool getCommandManager() {
        return commandManager;
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (commandManager != null) {
            commandManager.destroy();
        }
        LOGGER.debug("VulkanDevice 资源已清理");
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getVkDevice() {
        return device;
    }

    public VulkanQueue getMainQueue() {
        return mainQueue;
    }
}