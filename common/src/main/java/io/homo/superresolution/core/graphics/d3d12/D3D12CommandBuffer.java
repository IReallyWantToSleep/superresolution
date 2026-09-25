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
import io.homo.superresolution.core.graphics.impl.command.CommandBufferState;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandPool;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.utils.ThrowableUtil;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class D3D12CommandBuffer implements ICommandBuffer, AutoCloseable {
    private final D3D12Device device;
    private final D3D12CommandPool ownerPool;
    private final CommandBufferBehavior behavior;
    private long nativeAllocatorHandle;
    private long nativeCommandListHandle;
    private CommandBufferState state = CommandBufferState.Executable;
    private long completionValue;
    private boolean hasUnsubmittedRecording;
    private boolean nativeLeaseActive;
    private boolean externalRecordingSealed;
    private boolean poisoned;
    private boolean initialized;
    private final IdentityHashMap<D3D12Texture2D, D3D12ResourceState> recordedTextureStates =
            new IdentityHashMap<>();
    private final BiConsumer<D3D12Texture2D, D3D12ResourceState>
            submittedTextureStateValidator = this::validateSubmittedTextureStateLocked;
    private final BiConsumer<D3D12Texture2D, D3D12ResourceState>
            submittedTextureStateCommitter = this::commitSubmittedTextureStateLocked;

    D3D12CommandBuffer(
            D3D12Device device,
            D3D12CommandPool ownerPool,
            CommandBufferBehavior behavior) {
        this.device = Objects.requireNonNull(device, "device");
        device.assertLifecycleLockHeld();
        this.ownerPool = Objects.requireNonNull(ownerPool, "ownerPool");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
    }

    void initializeLocked() {
        device.assertLifecycleLockHeld();
        if (initialized || nativeAllocatorHandle != 0 || nativeCommandListHandle != 0) {
            throw new IllegalStateException("D3D12 command buffer is already initialized");
        }
        nativeAllocatorHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateCommandAllocator(device.nativeHandleLocked()),
                "Create D3D12 command allocator");
        nativeCommandListHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateCommandList(
                        device.nativeHandleLocked(), nativeAllocatorHandle),
                "Create D3D12 command list");
        device.setDebugNameLocked(
                nativeAllocatorHandle,
                "SuperResolution D3D12 Command Allocator " + behavior);
        device.setDebugNameLocked(
                nativeCommandListHandle,
                "SuperResolution D3D12 Command List " + behavior);
        initialized = true;
    }

    @Override
    public void begin() {
        device.withLifecycleLock(this::beginLocked);
    }

    void beginLocked() {
        ensureUsableLocked();
        refreshCompletionLocked();
        if (state == CommandBufferState.Recording) {
            throw new IllegalStateException("D3D12 command buffer is already recording");
        }
        if (state == CommandBufferState.Pending) {
            throw new IllegalStateException("D3D12 command buffer is still in flight");
        }
        if (hasUnsubmittedRecording) {
            throw new IllegalStateException(
                    "D3D12 command buffer has executable commands; submit or reset it before begin");
        }
        D3D12Exception.check(
                D3D12Native.nBeginCommandList(nativeCommandListHandle),
                "Begin D3D12 command list");
        recordedTextureStates.clear();
        externalRecordingSealed = false;
        nativeLeaseActive = false;
        state = CommandBufferState.Recording;
    }

    @Override
    public void end() {
        device.withLifecycleLock(this::endLocked);
    }

    void endLocked() {
        ensureUsableLocked();
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException("D3D12 command buffer is not recording");
        }
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    "Close the native D3D12 command-list lease before ending the command buffer");
        }
        D3D12Exception.check(
                D3D12Native.nEndCommandList(nativeCommandListHandle),
                "End D3D12 command list");
        hasUnsubmittedRecording = true;
        state = CommandBufferState.Executable;
    }

    @Override
    public void reset() {
        device.withLifecycleLock(this::resetLocked);
    }

    void resetLocked() {
        ensureUsableLocked();
        if (behavior == CommandBufferBehavior.OneTimeSubmit) {
            throw new IllegalStateException("Cannot reset a one-time D3D12 command buffer");
        }
        if (!ownerPool.flags().contains(CommandPoolFlags.Reset)) {
            throw new IllegalStateException("D3D12 command pool does not allow command buffer reset");
        }
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    "Close the native D3D12 command-list lease before resetting the command buffer");
        }
        refreshCompletionLocked();
        if (state == CommandBufferState.Pending) {
            throw new IllegalStateException(
                    "Cannot reset a D3D12 command buffer while it is in flight");
        }
        D3D12Exception.check(
                D3D12Native.nAbortCommandList(nativeCommandListHandle),
                "Reset D3D12 command list");
        completionValue = 0;
        hasUnsubmittedRecording = false;
        externalRecordingSealed = false;
        recordedTextureStates.clear();
        state = CommandBufferState.Executable;
    }

    @Override
    public void destroy() {
        device.withLifecycleLock(this::destroyLocked);
    }

    void destroyLocked() {
        device.assertLifecycleLockHeld();
        destroyInternalLocked(false);
    }

    void destroyAfterOneTimeSubmitLocked() {
        device.assertLifecycleLockHeld();
        destroyInternalLocked(true);
    }

    void validatePoolResetLocked() {
        device.assertLifecycleLockHeld();
        ensureUsableLocked();
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    "Close the native D3D12 command-list lease before resetting its pool");
        }
        refreshCompletionLocked();
        if (state == CommandBufferState.Pending) {
            throw new IllegalStateException(
                    "Cannot reset a D3D12 command pool with an in-flight command buffer");
        }
    }

    void validatePoolDestroyLocked(boolean allowPending) {
        device.assertLifecycleLockHeld();
        if (state == CommandBufferState.Destroyed) {
            return;
        }
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    "Close the native D3D12 command-list lease before destroying its pool");
        }
        if (state == CommandBufferState.Pending && !allowPending) {
            refreshCompletionLocked();
            if (state == CommandBufferState.Pending) {
                throw new IllegalStateException(
                        "Cannot destroy a D3D12 command pool with an in-flight command buffer");
            }
        }
    }

    void destroyAfterDeviceIdleLocked() {
        device.assertLifecycleLockHeld();
        destroyInternalLocked(true);
    }

    private void destroyInternalLocked(boolean allowPending) {
        device.assertLifecycleLockHeld();
        if (state == CommandBufferState.Destroyed) {
            return;
        }
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    "Close the native D3D12 command-list lease before destroying the command buffer");
        }
        if (state == CommandBufferState.Pending && !allowPending) {
            refreshCompletionLocked();
            if (state == CommandBufferState.Pending) {
                throw new IllegalStateException(
                        "Cannot destroy a D3D12 command buffer while it is in flight");
            }
        }
        Throwable failure = null;
        if (initialized && !poisoned && state != CommandBufferState.Pending &&
                nativeCommandListHandle != 0) {
            try {
                D3D12Exception.check(
                        D3D12Native.nAbortCommandList(nativeCommandListHandle),
                        "Abort D3D12 command list before destroy");
            } catch (Throwable throwable) {
                failure = appendFailure(failure, throwable);
            }
        }
        if (nativeCommandListHandle != 0) {
            try {
                D3D12Exception.check(
                        D3D12Native.nDestroyCommandList(nativeCommandListHandle),
                        "Destroy D3D12 command list");
                nativeCommandListHandle = 0;
            } catch (Throwable throwable) {
                poisoned = true;
                failure = appendFailure(failure, throwable);
            }
        }
        if (nativeAllocatorHandle != 0) {
            try {
                D3D12Native.nDestroyCommandAllocator(nativeAllocatorHandle);
                nativeAllocatorHandle = 0;
            } catch (Throwable throwable) {
                poisoned = true;
                failure = appendFailure(failure, throwable);
            }
        }
        if (nativeCommandListHandle == 0 && nativeAllocatorHandle == 0) {
            completionValue = 0;
            hasUnsubmittedRecording = false;
            externalRecordingSealed = true;
            recordedTextureStates.clear();
            state = CommandBufferState.Destroyed;
            initialized = false;
            ownerPool.onCommandBufferDestroyedLocked(this);
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure != null) {
            throw new D3D12Exception("Failed to destroy D3D12 command buffer: " + failure);
        }
    }

    private static Throwable appendFailure(
            Throwable failure,
            Throwable addition) {
        if (failure == null) {
            return addition;
        }
        if (failure != addition) {
            failure.addSuppressed(addition);
        }
        return failure;
    }

    @Override
    public void submit(IDevice targetDevice) {
        if (targetDevice != device) {
            throw new IllegalArgumentException("D3D12 command buffer belongs to a different device");
        }
        targetDevice.submitCommandBuffer(this);
    }

    @Override
    public D3D12Device getDevice() {
        return device;
    }

    @Override
    public ICommandDecoder decoder() {
        return device.commandDecoder();
    }

    @Override
    public ICommandPool ownerPool() {
        return ownerPool;
    }

    @Override
    public CommandBufferState state() {
        return device.withLifecycleLock(this::stateLocked);
    }

    CommandBufferState stateLocked() {
        device.assertLifecycleLockHeld();
        if (state == CommandBufferState.Pending) {
            refreshCompletionLocked();
        }
        return state;
    }

    @Override
    public boolean isFenceSignaled() {
        return device.withLifecycleLock(this::isFenceSignaledLocked);
    }

    private boolean isFenceSignaledLocked() {
        device.assertLifecycleLockHeld();
        if (state != CommandBufferState.Pending || completionValue == 0) {
            return true;
        }
        if (device.completedSubmissionValueLocked() >= completionValue) {
            completionValue = 0;
            state = CommandBufferState.Executable;
            return true;
        }
        return false;
    }

    @Override
    public void waitForFence() {
        device.withLifecycleLock(this::waitForFenceLocked);
    }

    void waitForFenceLocked() {
        device.assertLifecycleLockHeld();
        if (state != CommandBufferState.Pending || completionValue == 0) {
            return;
        }
        device.waitForSubmissionLocked(completionValue);
        completionValue = 0;
        state = CommandBufferState.Executable;
    }

    @Override
    public CommandBufferBehavior behavior() {
        return device.withLifecycleLock(this::behaviorLocked);
    }

    CommandBufferBehavior behaviorLocked() {
        device.assertLifecycleLockHeld();
        return behavior;
    }

    boolean isInitializedLocked() {
        device.assertLifecycleLockHeld();
        return initialized;
    }

    public NativeCommandListLease leaseNativeCommandList() {
        return device.withLifecycleLock(() -> {
            requireDecoderRecordingLocked("leaseNativeCommandList");
            long nativeCommandList = D3D12Exception.requireHandle(
                    D3D12Native.nGetNativeCommandList(nativeCommandListHandle),
                    "Borrow recording D3D12 command list");
            NativeCommandListLease lease = new NativeCommandListLease(
                    this,
                    nativeCommandList);
            device.retainExternalBorrowLocked();
            nativeLeaseActive = true;
            return lease;
        });
    }

    void requireDecoderRecording(String operation) {
        device.withLifecycleLock(() -> requireDecoderRecordingLocked(operation));
    }

    void requireDecoderRecordingLocked(String operation) {
        ensureUsableLocked();
        if (state != CommandBufferState.Recording) {
            throw new IllegalStateException(
                    operation + ": D3D12 command buffer is not recording");
        }
        if (nativeLeaseActive) {
            throw new IllegalStateException(
                    operation + ": native D3D12 command-list lease is active");
        }
        if (externalRecordingSealed) {
            throw new IllegalStateException(
                    operation + ": native D3D12 recording was leased; only end/reset/destroy is now allowed");
        }
    }

    long nativeHandleForDecoder(String operation) {
        return device.withLifecycleLock(() -> nativeHandleForDecoderLocked(operation));
    }

    long nativeHandleForDecoderLocked(String operation) {
        requireDecoderRecordingLocked(operation);
        return nativeCommandListHandle;
    }

    void markSubmittedLocked(long submittedCompletionValue) {
        ensureUsableLocked();
        hasUnsubmittedRecording = false;
        externalRecordingSealed = false;
        completionValue = submittedCompletionValue;
        state = submittedCompletionValue == 0
                ? CommandBufferState.Executable
                : CommandBufferState.Pending;
        recordedTextureStates.forEach(submittedTextureStateCommitter);
        recordedTextureStates.clear();
    }

    private void commitSubmittedTextureStateLocked(
            D3D12Texture2D texture,
            D3D12ResourceState state) {
        if (!texture.isDestroyedLocked()) {
            device.onTextureStateCommittedLocked(texture, state);
        }
    }

    private void validateSubmittedTextureStateLocked(
            D3D12Texture2D texture,
            D3D12ResourceState ignoredState) {
        if (!texture.isDestroyedLocked()) {
            device.validateTextureStateCommitLocked(texture);
        }
    }

    void markSubmissionFailedLocked() {
        device.assertLifecycleLockHeld();
        poisoned = true;
        hasUnsubmittedRecording = false;
        externalRecordingSealed = true;
        completionValue = 0;
        recordedTextureStates.clear();
        state = CommandBufferState.Executable;
    }

    void markExecutedUntrackedLocked() {
        device.assertLifecycleLockHeld();
        poisoned = true;
        hasUnsubmittedRecording = false;
        externalRecordingSealed = true;
        completionValue = 0;
        recordedTextureStates.forEach(submittedTextureStateCommitter);
        recordedTextureStates.clear();
        state = CommandBufferState.Executable;
    }

    D3D12ResourceState recordTextureStateLocked(
            D3D12Texture2D texture,
            D3D12ResourceState state) {
        device.assertLifecycleLockHeld();
        return recordedTextureStates.put(texture, state);
    }

    void restoreRecordedTextureStateLocked(
            D3D12Texture2D texture,
            D3D12ResourceState previousState) {
        device.assertLifecycleLockHeld();
        if (previousState == null) {
            recordedTextureStates.remove(texture);
        } else {
            recordedTextureStates.put(texture, previousState);
        }
    }

    boolean readyForSubmitLocked() {
        ensureUsableLocked();
        refreshCompletionLocked();
        return state == CommandBufferState.Executable && hasUnsubmittedRecording && !poisoned;
    }

    long nativeHandleForSubmitLocked() {
        if (!readyForSubmitLocked()) {
            throw new IllegalStateException(
                    "D3D12 command buffer must contain executable commands before submit");
        }
        recordedTextureStates.forEach(submittedTextureStateValidator);
        return nativeCommandListHandle;
    }

    private void closeNativeLeaseLocked(NativeCommandListLease lease) {
        device.assertLifecycleLockHeld();
        if (lease.owner != this || lease.closed) {
            return;
        }
        if (nativeLeaseActive) {
            nativeLeaseActive = false;
            externalRecordingSealed = true;
            device.releaseExternalBorrowLocked();
        }
        lease.closed = true;
    }

    private long nativeLeaseHandleLocked(NativeCommandListLease lease) {
        device.assertLifecycleLockHeld();
        if (lease.owner != this || lease.closed || !nativeLeaseActive) {
            throw new IllegalStateException("Native D3D12 command-list lease is closed");
        }
        ensureUsableLocked();
        return lease.nativeCommandList;
    }

    private void setExternalTextureStateLocked(
            NativeCommandListLease lease,
            D3D12Texture2D texture,
            D3D12ResourceState state) {
        device.assertLifecycleLockHeld();
        if (lease.owner != this || lease.closed || !nativeLeaseActive) {
            throw new IllegalStateException("Native D3D12 command-list lease is closed");
        }
        ensureUsableLocked();
        device.requireTextureLocked(texture, "setTextureState");
        D3D12ResourceState previousState = recordTextureStateLocked(texture, state);
        try {
            D3D12Exception.check(
                    D3D12Native.nSetCommandTextureState(
                            nativeCommandListHandle,
                            texture.nativeHandleLocked(),
                            state.nativeCode()),
                    "Set command-local D3D12 texture state");
        } catch (Throwable throwable) {
            restoreRecordedTextureStateLocked(texture, previousState);
            throw throwable;
        }
    }

    private void refreshCompletionLocked() {
        device.assertLifecycleLockHeld();
        if (state == CommandBufferState.Pending && completionValue != 0 &&
                device.completedSubmissionValueLocked() >= completionValue) {
            completionValue = 0;
            state = CommandBufferState.Executable;
        }
    }

    private void ensureUsableLocked() {
        device.assertLifecycleLockHeld();
        if (state == CommandBufferState.Destroyed || nativeCommandListHandle == 0) {
            throw new IllegalStateException("D3D12 command buffer is destroyed");
        }
        if (poisoned) {
            throw new IllegalStateException(
                    "D3D12 command buffer is poisoned after a failed submission");
        }
        device.ensureOpenLocked();
    }

    @Override
    public void close() {
        destroy();
    }

    public static final class NativeCommandListLease implements AutoCloseable {
        private final D3D12CommandBuffer owner;
        private final long nativeCommandList;
        private boolean closed;

        private NativeCommandListLease(D3D12CommandBuffer owner, long nativeCommandList) {
            this.owner = owner;
            this.nativeCommandList = nativeCommandList;
        }

        public long handle() {
            return owner.device.withLifecycleLock(() ->
                    owner.nativeLeaseHandleLocked(this));
        }

        public void setTextureState(
                D3D12Texture2D texture,
                D3D12ResourceState state) {
            owner.device.withLifecycleLock(() -> owner.setExternalTextureStateLocked(
                    this,
                    Objects.requireNonNull(texture, "texture"),
                    Objects.requireNonNull(state, "state")));
        }

        @Override
        public void close() {
            owner.device.withLifecycleLock(() -> owner.closeNativeLeaseLocked(this));
        }
    }
}
