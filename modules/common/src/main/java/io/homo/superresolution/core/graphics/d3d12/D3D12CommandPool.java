/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandPool;
import io.homo.superresolution.core.utils.ThrowableUtil;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;

public final class D3D12CommandPool implements ICommandPool, AutoCloseable {
    private final D3D12Device device;
    private final EnumSet<CommandPoolFlags> flags;
    private final ArrayList<D3D12CommandBuffer> commandBuffers = new ArrayList<>();
    private boolean destroyed;

    D3D12CommandPool(D3D12Device device, EnumSet<CommandPoolFlags> flags) {
        this.device = device;
        device.assertLifecycleLockHeld();
        this.flags = flags.clone();
    }

    public D3D12Device device() {
        return device;
    }

    @Override
    public D3D12CommandBuffer createCommandBuffer() {
        return createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
    }

    @Override
    public D3D12CommandBuffer createCommandBuffer(CommandBufferBehavior behavior) {
        return device.withLifecycleLock(() -> createCommandBufferLocked(behavior));
    }

    D3D12CommandBuffer createCommandBufferLocked(CommandBufferBehavior behavior) {
        ensureOpenLocked();
        if (behavior == null) {
            throw new IllegalArgumentException("behavior cannot be null");
        }
        if (behavior == CommandBufferBehavior.ReusableSequential &&
                !flags.contains(CommandPoolFlags.Reset)) {
            throw new IllegalStateException(
                    "Reusable D3D12 command buffers require a command pool with Reset");
        }
        retryUninitializedCommandBuffersLocked();
        commandBuffers.ensureCapacity(Math.addExact(commandBuffers.size(), 1));
        D3D12CommandBuffer commandBuffer = new D3D12CommandBuffer(device, this, behavior);
        commandBuffers.add(commandBuffer);
        try {
            commandBuffer.initializeLocked();
            return commandBuffer;
        } catch (Throwable throwable) {
            try {
                commandBuffer.destroyLocked();
            } catch (Throwable destroyFailure) {
                if (throwable != destroyFailure) {
                    throwable.addSuppressed(destroyFailure);
                }
            }
            ThrowableUtil.rethrowError(throwable);
            throw throwable;
        }
    }

    @Override
    public EnumSet<CommandPoolFlags> flags() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return flags.clone();
        });
    }

    @Override
    public void reset() {
        device.withLifecycleLock(this::resetLocked);
    }

    private void resetLocked() {
        ensureOpenLocked();
        retryUninitializedCommandBuffersLocked();
        if (!flags.contains(CommandPoolFlags.Reset)) {
            throw new IllegalStateException("D3D12 command pool does not allow reset");
        }
        List<D3D12CommandBuffer> reusableBuffers = commandBuffers.stream()
                .filter(commandBuffer ->
                        commandBuffer.behaviorLocked() == CommandBufferBehavior.ReusableSequential)
                .toList();
        for (D3D12CommandBuffer commandBuffer : reusableBuffers) {
            commandBuffer.validatePoolResetLocked();
        }
        for (D3D12CommandBuffer commandBuffer : reusableBuffers) {
            commandBuffer.resetLocked();
        }
    }

    void validateDestroyAfterDeviceIdleLocked() {
        device.assertLifecycleLockHeld();
        if (destroyed) {
            return;
        }
        for (D3D12CommandBuffer commandBuffer : List.copyOf(commandBuffers)) {
            commandBuffer.validatePoolDestroyLocked(true);
        }
    }

    void destroyAfterDeviceIdleLocked() {
        device.assertLifecycleLockHeld();
        if (destroyed) {
            return;
        }
        List<D3D12CommandBuffer> buffers = List.copyOf(commandBuffers);
        for (D3D12CommandBuffer commandBuffer : buffers) {
            commandBuffer.validatePoolDestroyLocked(true);
        }
        Throwable failure = null;
        for (D3D12CommandBuffer commandBuffer : buffers) {
            try {
                commandBuffer.destroyAfterDeviceIdleLocked();
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }
        if (commandBuffers.isEmpty()) {
            destroyed = true;
            device.onCommandPoolDestroyedLocked(this);
        }
        rethrowFailure(failure);
    }

    private static Throwable appendFailure(Throwable failure, Throwable addition) {
        if (failure == null) {
            return addition;
        }
        if (failure != addition) {
            failure.addSuppressed(addition);
        }
        return failure;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new D3D12Exception("Failed to destroy D3D12 command pool: " + failure);
    }

    private void validateOrdinaryDestroy() {
        for (D3D12CommandBuffer commandBuffer : List.copyOf(commandBuffers)) {
            commandBuffer.validatePoolDestroyLocked(false);
        }
    }

    private void destroyOrdinaryBuffers() {
        for (D3D12CommandBuffer commandBuffer : List.copyOf(commandBuffers)) {
            commandBuffer.destroyLocked();
        }
    }

    private void retryUninitializedCommandBuffersLocked() {
        device.assertLifecycleLockHeld();
        for (int index = commandBuffers.size() - 1; index >= 0; --index) {
            D3D12CommandBuffer commandBuffer = commandBuffers.get(index);
            if (!commandBuffer.isInitializedLocked()) {
                commandBuffer.destroyLocked();
            }
        }
    }

    void onCommandBufferDestroyedLocked(D3D12CommandBuffer commandBuffer) {
        device.assertLifecycleLockHeld();
        commandBuffers.remove(commandBuffer);
    }

    private void ensureOpenLocked() {
        device.assertLifecycleLockHeld();
        if (destroyed) {
            throw new IllegalStateException("D3D12 command pool is destroyed");
        }
        device.ensureOpenLocked();
    }

    @Override
    public void destroy() {
        device.withLifecycleLock(this::destroyLocked);
    }

    void destroyLocked() {
        device.assertLifecycleLockHeld();
        if (destroyed) {
            return;
        }
        validateOrdinaryDestroy();
        destroyOrdinaryBuffers();
        if (!commandBuffers.isEmpty()) {
            throw new IllegalStateException(
                    "D3D12 command pool retained command-buffer owners after destruction");
        }
        destroyed = true;
        device.onCommandPoolDestroyedLocked(this);
    }

    @Override
    public void close() {
        destroy();
    }
}
