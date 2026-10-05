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

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.utils.ThrowableUtil;

import java.util.Objects;

public final class D3D12Queue implements GpuObject {
    private final D3D12Device device;
    private final long nativeQueue;

    D3D12Queue(D3D12Device device, long nativeQueue) {
        this.device = Objects.requireNonNull(device, "device");
        this.nativeQueue = D3D12Exception.requireHandle(nativeQueue, "Query native D3D12 direct queue");
    }

    public D3D12Device device() {
        return device;
    }

    public void submit(D3D12CommandBuffer commandBuffer) {
        submit(commandBuffer, null, 0, 0);
    }

    public void submit(
            D3D12CommandBuffer commandBuffer,
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        device.withLifecycleLock(() -> submitLocked(
                commandBuffer, sharedFence, waitValue, signalValue));
    }

    void submitLocked(
            D3D12CommandBuffer commandBuffer,
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        device.assertLifecycleLockHeld();
        CommandBufferBehavior behavior = null;
        Throwable failure = null;
        int disposition = D3D12Native.SUBMIT_NOT_EXECUTED;
        try {
            device.ensureOpenLocked();
            if (commandBuffer == null) {
                throw new IllegalArgumentException(
                        "D3D12 command buffer cannot be null");
            }
            if (commandBuffer.getDevice() != device) {
                throw new IllegalArgumentException(
                        "D3D12 command buffer belongs to a different device");
            }
            behavior = commandBuffer.behaviorLocked();
            long fenceHandle = validateFenceSubmissionLocked(
                    sharedFence, waitValue, signalValue);
            long nativeResult = D3D12Native.nSubmit(
                    device.nativeHandleLocked(),
                    commandBuffer.nativeHandleForSubmitLocked(),
                    fenceHandle,
                    waitValue,
                    signalValue);
            int result = (int) nativeResult;
            disposition = (int) (nativeResult >>> 32);
            if (result < 0) {
                Throwable submissionFailure;
                try {
                    submissionFailure = D3D12Exception.fromLastError(
                            "Submit D3D12 command list", result);
                } catch (Throwable diagnosticFailure) {
                    submissionFailure = diagnosticFailure;
                }
                if (disposition == D3D12Native.SUBMIT_SUBMITTED) {
                    commandBuffer.markSubmittedLocked(device.lastSubmittedValueLocked());
                } else if (disposition == D3D12Native.SUBMIT_EXECUTED_UNTRACKED) {
                    commandBuffer.markExecutedUntrackedLocked();
                } else {
                    commandBuffer.markSubmissionFailedLocked();
                }
                throw submissionFailure;
            }

            if (disposition != D3D12Native.SUBMIT_SUBMITTED) {
                if (disposition == D3D12Native.SUBMIT_NOT_EXECUTED) {
                    commandBuffer.markSubmissionFailedLocked();
                } else {
                    commandBuffer.markExecutedUntrackedLocked();
                }
                throw new D3D12Exception(
                        "Native D3D12 submit succeeded with unexpected disposition " +
                                disposition);
            }

            commandBuffer.markSubmittedLocked(device.lastSubmittedValueLocked());
            if (behavior == CommandBufferBehavior.OneTimeSubmit) {
                commandBuffer.waitForFenceLocked();
            }
        } catch (Throwable throwable) {
            failure = throwable;
        }

        if (failure != null && sharedFence != null) {
            try {
                if (disposition == D3D12Native.SUBMIT_NOT_EXECUTED) {
                    recoverSharedFenceLocked(sharedFence, waitValue, signalValue);
                } else {
                    recoverExecutedSharedFenceLocked(
                            sharedFence, waitValue, signalValue);
                }
            } catch (Throwable recoveryFailure) {
                addSuppressedNoThrow(failure, recoveryFailure);
            }
        }

        if (behavior == CommandBufferBehavior.OneTimeSubmit) {
            try {
                commandBuffer.destroyAfterOneTimeSubmitLocked();
            } catch (Throwable destroyFailure) {
                if (failure == null) {
                    failure = destroyFailure;
                } else {
                    addSuppressedNoThrow(failure, destroyFailure);
                }
            }
        }

        if (failure != null) {
            rethrow(failure);
        }
    }

    private long validateFenceSubmissionLocked(
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        if (sharedFence == null) {
            if (waitValue != 0 || signalValue != 0) {
                throw new IllegalArgumentException(
                        "A shared D3D12 fence is required for nonzero wait/signal values");
            }
            return 0;
        }
        if (sharedFence.device() != device) {
            throw new IllegalArgumentException("D3D12 fence belongs to a different device");
        }
        if (waitValue < 0 || signalValue <= waitValue) {
            throw new IllegalArgumentException(
                    "D3D12 submit requires waitValue >= 0 and signalValue > waitValue");
        }
        return sharedFence.nativeHandleLocked();
    }

    private static void rethrow(Throwable throwable) {
        ThrowableUtil.rethrowError(throwable);
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new D3D12Exception("Unexpected D3D12 submission failure: " + throwable);
    }

    private static void addSuppressedNoThrow(
            Throwable failure,
            Throwable addition) {
        if (failure == addition) {
            return;
        }
        try {
            failure.addSuppressed(addition);
        } catch (Throwable ignored) {
        }
    }

    public void recoverSharedFence(
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        device.withLifecycleLock(() -> recoverSharedFenceLocked(
                sharedFence, waitValue, signalValue));
    }

    void recoverSharedFenceLocked(
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        device.assertLifecycleLockHeld();
        device.ensureOpenLocked();
        if (sharedFence == null) {
            throw new IllegalArgumentException(
                    "D3D12 shared fence cannot be null");
        }
        if (sharedFence.device() != device) {
            throw new IllegalArgumentException("D3D12 fence belongs to a different device");
        }
        D3D12Exception.check(
                D3D12Native.nRecoverSharedFence(
                        device.nativeHandleLocked(),
                        sharedFence.nativeHandleLocked(),
                        waitValue,
                        signalValue),
                "Recover shared D3D12 fence handoff");
    }

    private void recoverExecutedSharedFenceLocked(
            D3D12Fence sharedFence,
            long waitValue,
            long signalValue) {
        device.assertLifecycleLockHeld();
        device.ensureOpenLocked();
        if (sharedFence == null) {
            throw new IllegalArgumentException(
                    "D3D12 shared fence cannot be null");
        }
        if (sharedFence.device() != device) {
            throw new IllegalArgumentException(
                    "D3D12 fence belongs to a different device");
        }
        D3D12Exception.check(
                D3D12Native.nRecoverExecutedSharedFence(
                        device.nativeHandleLocked(),
                        sharedFence.nativeHandleLocked(),
                        waitValue,
                        signalValue),
                "Recover executed D3D12 shared-fence handoff");
    }

    public void waitIdle() {
        device.waitIdle();
    }

    @Override
    public long handle() {
        return device.withLifecycleLock(() -> {
            device.ensureOpenLocked();
            return nativeQueue;
        });
    }
}
