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
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
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
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandDecoder;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandManager;
import io.homo.superresolution.core.graphics.vulkan.texture.VulkanTexture;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanDevice implements IDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanDevice.class);

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final VkQueue graphicsQueue;
    private final int graphicsQueueFamilyIndex;
    private final VulkanCommandManager commandManager;
    private final VulkanCommandDecoder commandDecoder;


    public VulkanDevice(VkPhysicalDevice physicalDevice, VkDevice device, int graphicsQueueFamilyIndex) {
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.graphicsQueueFamilyIndex = graphicsQueueFamilyIndex;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQueueFamilyIndex, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
        }
        this.commandManager = new VulkanCommandManager(this);
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

    public ITexture createTextureFromHandle(TextureDescription description, long memory) {
        return new VulkanTexture(this, description, memory);
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
    public ICommandBuffer createCommandBuffer() {
        return new VulkanCommandBuffer(this);
    }

    @Override
    public ICommandDecoder commandDecoder() {
        return commandDecoder;
    }

    @Override
    public void submitCommandBuffer(ICommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(
                            stack.pointers(
                                    ((VulkanCommandBuffer) commandBuffer)
                                            .getNativeCommandBuffer()
                                            .address()
                            )
                    );
            vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(graphicsQueue);
        }
    }

    /**
     * 获取VulkanCommandManager实例
     *
     * @return VulkanCommandManager实例
     */
    public VulkanCommandManager getCommandManager() {
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

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public int getGraphicsQueueFamilyIndex() {
        return graphicsQueueFamilyIndex;
    }
}