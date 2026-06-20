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

import io.homo.superresolution.core.graphics.impl.command.*;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandBuffer implements ICommandBuffer {
    private final VulkanDevice vulkanDevice;
    private final VulkanCommandPool ownerPool;
    private final CommandBufferBehavior behavior;
    private final List<Destroyable> transientResources = new ArrayList<>();
    private CommandBufferState state = CommandBufferState.Executable;
    private long reusableFence = VK_NULL_HANDLE;
    private boolean inFlight = false;
    private VkCommandBuffer nativeCommandBuffer;
    private VulkanRenderPass activeRenderPass;
    private VulkanGraphicsPipeline boundGraphicsPipeline;
    private VulkanComputePipeline boundComputePipeline;
    private boolean renderPassActive;
    private long boundGraphicsPipelineHandle = VK_NULL_HANDLE;
    private long boundComputePipelineHandle = VK_NULL_HANDLE;
    private final Map<DescriptorSetKey, DescriptorSetState> pushedDescriptorSets = new HashMap<>();
    private long pipelineBindCount;
    private long pushDescriptorCount;
    private long pushDescriptorSkippedCount;
    private long layoutCompatibleReuseCount;

    public VulkanCommandBuffer(VulkanDevice vulkanDevice, VulkanCommandPool ownerPool, CommandBufferBehavior behavior) {
        this.vulkanDevice = vulkanDevice;
        this.ownerPool = ownerPool;
        this.behavior = behavior;
        nativeCommandBuffer = ownerPool.createNativeCommandBuffer();
        vulkanDevice.setDebugName(VK_OBJECT_TYPE_COMMAND_BUFFER, nativeCommandBuffer.address(), "CommandBuffer:" + behavior);
    }

    public VkCommandBuffer getNativeCommandBuffer() {
        return nativeCommandBuffer;
    }

    @Override
    public void begin() {
        ensureNotDestroyed();
        if (state == CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is already recording");
        }
        if (inFlight && !isFenceSignaled()) {
            throw new IllegalStateException("Command buffer is still in-flight and cannot begin");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VK_CHECK(vkBeginCommandBuffer(nativeCommandBuffer, beginInfo));
        }
        vulkanDevice.beginDebugLabel(nativeCommandBuffer, "Command Buffer");
        clearRenderPassState();
        clearBindingState();
        state = CommandBufferState.Recording;
    }

    @Override
    public void end() {
        ensureNotDestroyed();
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is not in recording state");
        }
        if (renderPassActive) {
            throw new IllegalStateException("Command buffer still has an active render pass; call endRenderPass first");
        }
        vulkanDevice.endDebugLabel(nativeCommandBuffer);
        VK_CHECK(vkEndCommandBuffer(nativeCommandBuffer));
        state = CommandBufferState.Executable;
    }

    @Override
    public void reset() {
        ensureNotDestroyed();
        if (behavior == CommandBufferBehavior.OneTimeSubmit) {
            throw new IllegalStateException("Cannot reset a one-time submit command buffer");
        }
        if (!ownerPool.flags().contains(io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags.Reset)) {
            throw new IllegalStateException("Command pool does not allow command buffer reset");
        }
        ensureNotInFlight();
        VK_CHECK(vkResetCommandBuffer(nativeCommandBuffer, 0));
        clearRenderPassState();
        clearBindingState();
        state = CommandBufferState.Executable;
    }

    @Override
    public void destroy() {
        if (state == CommandBufferState.Destroyed) {
            return;
        }
        ensureNotInFlight();
        destroyTransientResources();
        if (reusableFence != VK_NULL_HANDLE) {
            ownerPool.getFencePool().destroyFence(reusableFence);
            reusableFence = VK_NULL_HANDLE;
        }
        ownerPool.freeCommandBuffer(nativeCommandBuffer);
        ownerPool.onCommandBufferDestroyed(this);
        nativeCommandBuffer = null;
        clearRenderPassState();
        clearBindingState();
        state = CommandBufferState.Destroyed;
    }

    @Override
    public void submit(IDevice device) {
        ensureNotDestroyed();
        if (state != CommandBufferState.Executable) {
            throw new IllegalStateException("Command buffer must be executable before submit");
        }
        device.submitCommandBuffer(this);
        if (behavior == CommandBufferBehavior.OneTimeSubmit) {
            waitForFence();
            destroy();
        }
    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }

    @Override
    public ICommandDecoder decoder() {
        return vulkanDevice.commandDecoder();
    }

    @Override
    public ICommandPool ownerPool() {
        return ownerPool;
    }

    @Override
    public CommandBufferState state() {
        if (state == CommandBufferState.Destroyed) {
            return CommandBufferState.Destroyed;
        }
        if (inFlight && !isFenceSignaled()) {
            return CommandBufferState.Pending;
        }
        return state;
    }

    @Override
    public boolean isInFlight() {
        return state() == CommandBufferState.Pending;
    }

    @Override
    public boolean isFenceSignaled() {
        if (reusableFence == VK_NULL_HANDLE) {
            inFlight = false;
            destroyTransientResourcesIfComplete();
            return true;
        }
        int status = vkGetFenceStatus(vulkanDevice.getVkDevice(), reusableFence);
        if (status == VK_SUCCESS) {
            inFlight = false;
            destroyTransientResourcesIfComplete();
            return true;
        }
        if (status == VK_NOT_READY) {
            return false;
        }
        VK_CHECK(status);
        return false;
    }

    @Override
    public void waitForFence() {
        if (reusableFence == VK_NULL_HANDLE) {
            inFlight = false;
            destroyTransientResourcesIfComplete();
            return;
        }
        VK_CHECK(vkWaitForFences(vulkanDevice.getVkDevice(), reusableFence, true, Long.MAX_VALUE));
        inFlight = false;
        destroyTransientResourcesIfComplete();
    }

    @Override
    public CommandBufferBehavior behavior() {
        return behavior;
    }

    long prepareFenceForSubmit() {
        ensureNotDestroyed();
        if (reusableFence == VK_NULL_HANDLE) {
            reusableFence = ownerPool.getFencePool().createFence();
        }
        if (inFlight && !isFenceSignaled()) {
            throw new IllegalStateException("Command buffer is still in-flight and cannot be submitted again");
        }
        VK_CHECK(vkResetFences(vulkanDevice.getVkDevice(), reusableFence));
        inFlight = true;
        return reusableFence;
    }

    void markSubmitted() {
        inFlight = true;
    }

    public void markExternalSubmitted() {
        ensureNotDestroyed();
        if (state != CommandBufferState.Executable) {
            throw new IllegalStateException("Command buffer must be executable before external submit");
        }
        inFlight = true;
    }

    public boolean isExternallyComplete() {
        return inFlight;
    }

    public void markExternalComplete() {
        inFlight = false;
        destroyTransientResourcesIfComplete();
    }

    void _beginRenderPass(VulkanRenderPass renderPass) {
        ensureNotDestroyed();
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is not in recording state");
        }
        if (renderPassActive) {
            throw new IllegalStateException("Render pass is already active");
        }
        this.activeRenderPass = renderPass;
        this.boundGraphicsPipeline = null;
        this.renderPassActive = true;
    }

    void _endRenderPass() {
        if (!renderPassActive) {
            throw new IllegalStateException("No active render pass to end");
        }
        boundGraphicsPipeline = null;
        clearRenderPassState();
    }

    void bindGraphicsPipeline(VulkanGraphicsPipeline pipeline) {
        this.boundGraphicsPipeline = pipeline;
    }

    void bindComputePipeline(VulkanComputePipeline pipeline) {
        this.boundComputePipeline = pipeline;
    }

    boolean isNativePipelineBound(int bindPoint, long pipelineHandle) {
        return switch (bindPoint) {
            case VK_PIPELINE_BIND_POINT_GRAPHICS -> boundGraphicsPipelineHandle == pipelineHandle;
            case VK_PIPELINE_BIND_POINT_COMPUTE -> boundComputePipelineHandle == pipelineHandle;
            default -> false;
        };
    }

    void recordNativePipelineBind(int bindPoint, long pipelineHandle) {
        switch (bindPoint) {
            case VK_PIPELINE_BIND_POINT_GRAPHICS -> boundGraphicsPipelineHandle = pipelineHandle;
            case VK_PIPELINE_BIND_POINT_COMPUTE -> boundComputePipelineHandle = pipelineHandle;
            default -> {
            }
        }
        pipelineBindCount++;
    }

    List<VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> collectDescriptorUpdates(
            int bindPoint,
            int setIndex,
            VulkanPipelineDescriptorSet.DescriptorLayoutKey layoutKey,
            List<VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> requestedBindings
    ) {
        DescriptorSetKey key = new DescriptorSetKey(bindPoint, setIndex);
        DescriptorSetState current = pushedDescriptorSets.get(key);
        if (current == null || !current.layoutKey().equals(layoutKey)) {
            return requestedBindings;
        }

        List<VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> updates = new ArrayList<>();
        for (VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey requested : requestedBindings) {
            VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey previous =
                    current.bindings().get(requested.binding());
            if (!requested.equals(previous)) {
                updates.add(requested);
            } else {
                layoutCompatibleReuseCount++;
            }
        }

        if (updates.isEmpty()) {
            pushDescriptorSkippedCount++;
        }
        return updates;
    }

    void recordDescriptorPush(
            int bindPoint,
            int setIndex,
            VulkanPipelineDescriptorSet.DescriptorLayoutKey layoutKey,
            List<VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> pushedBindings
    ) {
        DescriptorSetKey key = new DescriptorSetKey(bindPoint, setIndex);
        DescriptorSetState state = pushedDescriptorSets.get(key);
        if (state == null || !state.layoutKey().equals(layoutKey)) {
            state = new DescriptorSetState(layoutKey);
            pushedDescriptorSets.put(key, state);
        }
        for (VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey binding : pushedBindings) {
            state.bindings().put(binding.binding(), binding);
        }
        pushDescriptorCount++;
    }

    public long getPipelineBindCount() {
        return pipelineBindCount;
    }

    public long getPushDescriptorCount() {
        return pushDescriptorCount;
    }

    public long getPushDescriptorSkippedCount() {
        return pushDescriptorSkippedCount;
    }

    public long getLayoutCompatibleReuseCount() {
        return layoutCompatibleReuseCount;
    }

    VulkanGraphicsPipeline getBoundGraphicsPipeline() {
        return boundGraphicsPipeline;
    }

    VulkanComputePipeline getBoundComputePipeline() {
        return boundComputePipeline;
    }

    boolean isRenderPassActive() {
        return renderPassActive;
    }

    VulkanRenderPass getActiveRenderPass() {
        return activeRenderPass;
    }

    void addTransientResource(Destroyable destroyable) {
        if (destroyable == null) {
            return;
        }
        transientResources.add(destroyable);
    }

    void destroyTransientResourcesIfComplete() {
        if (!transientResources.isEmpty() && !isInFlight()) {
            destroyTransientResources();
        }
    }

    private void ensureNotDestroyed() {
        if (state == CommandBufferState.Destroyed || nativeCommandBuffer == null) {
            throw new IllegalStateException("Command buffer is destroyed");
        }
    }

    private void ensureNotInFlight() {
        if (!inFlight) {
            return;
        }
        if (!isFenceSignaled()) {
            throw new IllegalStateException("Command buffer is still in-flight");
        }
    }

    private void clearRenderPassState() {
        activeRenderPass = null;
        boundGraphicsPipeline = null;
        boundComputePipeline = null;
        renderPassActive = false;
    }

    private void clearBindingState() {
        boundGraphicsPipelineHandle = VK_NULL_HANDLE;
        boundComputePipelineHandle = VK_NULL_HANDLE;
        pushedDescriptorSets.clear();
    }

    private void destroyTransientResources() {
        if (transientResources.isEmpty()) {
            return;
        }
        List<Destroyable> resourcesToDestroy = new ArrayList<>(transientResources);
        transientResources.clear();
        for (Destroyable destroyable : resourcesToDestroy) {
            destroyable.destroy();
        }
    }

    private record DescriptorSetKey(int bindPoint, int setIndex) {
    }

    private static final class DescriptorSetState {
        private final VulkanPipelineDescriptorSet.DescriptorLayoutKey layoutKey;
        private final Map<Integer, VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> bindings = new HashMap<>();

        private DescriptorSetState(VulkanPipelineDescriptorSet.DescriptorLayoutKey layoutKey) {
            this.layoutKey = layoutKey;
        }

        private VulkanPipelineDescriptorSet.DescriptorLayoutKey layoutKey() {
            return layoutKey;
        }

        private Map<Integer, VulkanPipelineDescriptorSet.DescriptorBindingSnapshotKey> bindings() {
            return bindings;
        }
    }
}
