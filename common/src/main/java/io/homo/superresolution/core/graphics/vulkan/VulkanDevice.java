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
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.ITextureView;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureViewDescription;
import io.homo.superresolution.core.graphics.impl.validation.ValidatedCommandDecoder;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;

public class VulkanDevice implements IDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanDevice.class);
    private final VkInstance instance;
    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final VulkanQueue mainQueue;
    private final VulkanCommandPool defaultCommandPool;
    private final VulkanCommandDecoder commandDecoder;
    private final ValidatedCommandDecoder validatedCommandDecoder;
    private final VulkanMemoryAllocator memoryAllocator;
    private final List<DeferredDestroy> deferredDestroys = new ArrayList<>();
    private final boolean ownsVkDevice;
    private boolean drainingDeferredDestroys;


    public VulkanDevice(VkInstance instance, VkPhysicalDevice physicalDevice, VkDevice device, int graphicsQueueFamilyIndex) {
        this(instance, physicalDevice, device, graphicsQueueFamilyIndex, true);
    }

    public VulkanDevice(VkInstance instance, VkPhysicalDevice physicalDevice, VkDevice device, int graphicsQueueFamilyIndex, boolean ownsVkDevice) {
        this.instance = instance;
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.ownsVkDevice = ownsVkDevice;
        this.mainQueue = new VulkanQueue(this, graphicsQueueFamilyIndex);
        this.defaultCommandPool= new VulkanCommandPool(this, EnumSet.of(CommandPoolFlags.Reset));
        this.commandDecoder = new VulkanCommandDecoder(this);
        this.validatedCommandDecoder = new ValidatedCommandDecoder(commandDecoder);
        this.memoryAllocator = new VulkanMemoryAllocator(this);
        defaultCommandPool.init();
    }

    @Override
    public ITexture createTexture(TextureDescription description) {
        return new VulkanTexture(this, description);
    }

    @Override
    public ISampler createSampler(SamplerDescription description) {
        return new VulkanSampler(this, description);
    }

    @Override
    public ITextureView createTextureView(TextureViewDescription description) {
        return new VulkanTextureView(this, description);
    }

    @Override
    public IFrameBuffer createFramebuffer(FramebufferDescription description) {
        return new VulkanFramebuffer(this, description);
    }

    @Override
    public IShaderProgram createShaderProgram(ShaderDescription description) {
        VulkanShaderProgram program = new VulkanShaderProgram(this, description);
        program.compile();
        return program;
    }

    @Override
    public IVertexBuffer createVertexBuffer(VertexBufferDescription description) {
        return new VulkanVertexBuffer(this, description);
    }

    @Override
    public IBuffer createBuffer(BufferDescription description) {
        return new VulkanBuffer(this, description);
    }

    @Override
    public RenderPass createRenderPass(RenderPass.Builder builder) {
        return new VulkanRenderPass(
                this,
                builder.getFrameBuffer(),
                builder.getClearState()
        );
    }

    @Override
    public PipelineDescriptorSet createDescriptorSet(IShaderProgram shader) {
        return new VulkanPipelineDescriptorSet(this, shader);
    }

    @Override
    public ComputePipeline createComputePipeline(ComputePipeline.Builder builder) {
        PipelineDescriptorSet descriptorSet = createDescriptorSet(builder.shader());
        return new VulkanComputePipeline(this, builder.shader(), descriptorSet);
    }

    @Override
    public GraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder) {
        PipelineDescriptorSet descriptorSet = createDescriptorSet(builder.shader());
        return new VulkanGraphicsPipeline(
                this,
                builder.shader(),
                builder.renderPass(),
                builder.rasterization(),
                builder.depthStencil(),
                builder.colorBlend(),
                builder.dynamicStates(),
                builder.primitiveType(),
                builder.vertexFormat(),
                descriptorSet
        );
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
        return validatedCommandDecoder;
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
        reapCompletedTransientResources();
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
        reapCompletedTransientResources();
    }

    public void destroy() {
        waitForAllCommandBuffers();
        reapCompletedTransientResources();
        flushDeferredDestroys();
        if (defaultCommandPool != null) {
            defaultCommandPool.destroy();
        }
        if (memoryAllocator != null) {
            memoryAllocator.destroy();
        }
        LOGGER.debug("VulkanDevice 资源已清理");
    }

    public boolean ownsVkDevice() {
        return ownsVkDevice;
    }

    public VkInstance getVkInstance() {
        return instance;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getVkDevice() {
        return device;
    }

    public void setDebugName(int objectType, long handle, String label) {
        VulkanDebug.setObjectName(device, objectType, handle, label);
    }

    public void beginDebugLabel(VkCommandBuffer commandBuffer, String label) {
        VulkanDebug.beginLabel(commandBuffer, label);
    }

    public void endDebugLabel(VkCommandBuffer commandBuffer) {
        VulkanDebug.endLabel(commandBuffer);
    }

    public void insertDebugLabel(VkCommandBuffer commandBuffer, String label) {
        VulkanDebug.insertLabel(commandBuffer, label);
    }

    public VulkanQueue getMainQueue() {
        return mainQueue;
    }

    public VulkanMemoryAllocator getMemoryAllocator() {
        return memoryAllocator;
    }

    void queueForDestroy(Runnable destroyAction) {
        if (destroyAction == null) {
            return;
        }
        List<VulkanCommandBuffer> blockers = new ArrayList<>();
        for (VulkanCommandBuffer buffer : new ArrayList<>(defaultCommandPool.getAllocatedBuffers())) {
            if (buffer.isInFlight()) {
                blockers.add(buffer);
            }
        }
        if (blockers.isEmpty() && !drainingDeferredDestroys) {
            destroyAction.run();
            return;
        }
        deferredDestroys.add(new DeferredDestroy(destroyAction, blockers));
    }

    private void reapCompletedTransientResources() {
        for (VulkanCommandBuffer buffer : new ArrayList<>(defaultCommandPool.getAllocatedBuffers())) {
            buffer.destroyTransientResourcesIfComplete();
        }
        drainingDeferredDestroys = true;
        try {
            boolean destroyedAny;
            do {
                destroyedAny = false;
                List<DeferredDestroy> snapshot = new ArrayList<>(deferredDestroys);
                for (DeferredDestroy deferredDestroy : snapshot) {
                    if (deferredDestroy.isReady() && deferredDestroys.remove(deferredDestroy)) {
                        deferredDestroy.destroyAction().run();
                        destroyedAny = true;
                    }
                }
            } while (destroyedAny);
        } finally {
            drainingDeferredDestroys = false;
        }
    }

    private void waitForAllCommandBuffers() {
        for (VulkanCommandBuffer buffer : new ArrayList<>(defaultCommandPool.getAllocatedBuffers())) {
            buffer.waitForFence();
        }
    }

    private void flushDeferredDestroys() {
        drainingDeferredDestroys = true;
        try {
            while (!deferredDestroys.isEmpty()) {
                DeferredDestroy deferredDestroy = deferredDestroys.remove(0);
                for (VulkanCommandBuffer blocker : deferredDestroy.blockers()) {
                    blocker.waitForFence();
                }
                deferredDestroy.destroyAction().run();
            }
        } finally {
            drainingDeferredDestroys = false;
        }
    }

    private record DeferredDestroy(Runnable destroyAction, List<VulkanCommandBuffer> blockers) {
        private boolean isReady() {
            for (VulkanCommandBuffer blocker : blockers) {
                if (!blocker.isFenceSignaled()) {
                    return false;
                }
            }
            return true;
        }
    }
}
