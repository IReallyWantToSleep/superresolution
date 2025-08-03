package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandDecoder;
import io.homo.superresolution.core.graphics.vulkan.command.VulkanCommandEncoder;
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
    private final VulkanCommandEncoder commandEncoder;
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
        this.commandEncoder = new VulkanCommandEncoder(this);
        this.commandDecoder = new VulkanCommandDecoder(this);

    }

    @Override
    public ITexture createTexture(TextureDescription description) {
        return new VulkanTexture(this, description);
    }

    public ITexture createTextureFromHandle(TextureDescription description, long memory) {
        return new VulkanTexture(this, description, memory);
    }

    @Override
    public IShaderProgram<?> createShaderProgram(ShaderDescription description) {
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
    public ICommandEncoder commendEncoder() {
        return commandEncoder;
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