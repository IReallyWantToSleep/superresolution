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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandBuffer implements ICommandBuffer {
    private final VulkanDevice vulkanDevice;
    private final VulkanCommandPool ownerPool;
    private final CommandBufferBehavior behavior;
    private CommandBufferState state = CommandBufferState.Executable;
    private long reusableFence = VK_NULL_HANDLE;
    private boolean inFlight = false;
    private VkCommandBuffer nativeCommandBuffer;

    public VulkanCommandBuffer(VulkanDevice vulkanDevice, VulkanCommandPool ownerPool, CommandBufferBehavior behavior) {
        this.vulkanDevice = vulkanDevice;
        this.ownerPool = ownerPool;
        this.behavior = behavior;
        nativeCommandBuffer = ownerPool.createNativeCommandBuffer();
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
        state = CommandBufferState.Recording;
    }

    @Override
    public void end() {
        ensureNotDestroyed();
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("Command buffer is not in recording state");
        }
        VK_CHECK(vkEndCommandBuffer(nativeCommandBuffer));
        state = CommandBufferState.Executable;
    }

    @Override
    public void reset() {
        ensureNotDestroyed();
        if (!ownerPool.flags().contains(io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags.Reset)) {
            throw new IllegalStateException("Command pool does not allow command buffer reset");
        }
        ensureNotInFlight();
        VK_CHECK(vkResetCommandBuffer(nativeCommandBuffer, 0));
        state = CommandBufferState.Executable;
    }

    @Override
    public void destroy() {
        if (state == CommandBufferState.Destroyed) {
            return;
        }
        ensureNotInFlight();
        if (reusableFence != VK_NULL_HANDLE) {
            ownerPool.getFencePool().destroyFence(reusableFence);
            reusableFence = VK_NULL_HANDLE;
        }
        ownerPool.freeCommandBuffer(nativeCommandBuffer);
        ownerPool.onCommandBufferDestroyed(this);
        nativeCommandBuffer = null;
        state = CommandBufferState.Destroyed;
    }

    @Override
    public void submit(IDevice device) {
        ensureNotDestroyed();
        if (state != CommandBufferState.Executable) {
            throw new IllegalStateException("Command buffer must be executable before submit");
        }
        device.submitCommandBuffer(this);
    }

    @Override
    public IDevice getDevice() {
        return vulkanDevice;
    }

    @Override
    public ICommandDecoder getDecoder() {
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
            return true;
        }
        int status = vkGetFenceStatus(vulkanDevice.getVkDevice(), reusableFence);
        if (status == VK_SUCCESS) {
            inFlight = false;
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
            return;
        }
        VK_CHECK(vkWaitForFences(vulkanDevice.getVkDevice(), reusableFence, true, Long.MAX_VALUE));
        inFlight = false;
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
}
