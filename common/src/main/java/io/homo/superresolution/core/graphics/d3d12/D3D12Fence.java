/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;

import java.util.Objects;

public final class D3D12Fence implements GpuObject, Destroyable, AutoCloseable {
    private final D3D12Device device;
    private long nativeFence;
    private long sharedHandle;
    private long nativeHandle;
    private int externalBorrowCount;

    D3D12Fence(D3D12Device device) {
        this.device = Objects.requireNonNull(device, "device");
        device.assertLifecycleLockHeld();
    }

    void initializeLocked(long initialValue) {
        device.assertLifecycleLockHeld();
        if (nativeHandle != 0) {
            throw new IllegalStateException("D3D12 fence is already initialized");
        }
        nativeHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateSharedFence(device.nativeHandleLocked(), initialValue),
                "Create shared D3D12 fence");
        long queriedFence = D3D12Native.nGetNativeFence(nativeHandle);
        long queriedSharedHandle = D3D12Native.nGetSharedFenceHandle(nativeHandle);
        if (queriedFence == 0 || queriedSharedHandle == 0) {
            throw D3D12Exception.fromLastError(
                    "Query shared D3D12 fence metadata", null);
        }
        this.nativeFence = queriedFence;
        this.sharedHandle = queriedSharedHandle;
    }

    public D3D12Device device() {
        return device;
    }

    /**
     * Returns a borrowed Win32 NT handle. The native fence owner closes it.
     */
    public long sharedHandle() {
        return device.withLifecycleLock(this::sharedHandleLocked);
    }

    long sharedHandleLocked() {
        ensureOpenLocked();
        return sharedHandle;
    }

    ExternalBorrowLease borrowExternal() {
        return device.withLifecycleLock(this::borrowExternalLocked);
    }

    private ExternalBorrowLease borrowExternalLocked() {
        ensureOpenLocked();
        if (externalBorrowCount == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Too many active D3D12 fence external borrows");
        }
        ExternalBorrowLease lease = new ExternalBorrowLease(this);
        device.retainExternalBorrowLocked();
        ++externalBorrowCount;
        return lease;
    }

    private void releaseExternalBorrowLocked(ExternalBorrowLease lease) {
        device.assertLifecycleLockHeld();
        if (lease.closed) {
            return;
        }
        if (externalBorrowCount <= 0) {
            throw new IllegalStateException(
                    "D3D12 fence external borrow count underflow");
        }
        --externalBorrowCount;
        device.releaseExternalBorrowLocked();
        lease.closed = true;
    }

    public long reserveValue() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            long value = D3D12Native.nReserveSharedFenceValue(nativeHandle);
            if (value <= 0) {
                throw D3D12Exception.fromLastError(
                        "Reserve shared D3D12 fence value", null);
            }
            return value;
        });
    }

    public long completedValue() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return D3D12Native.nGetCompletedSharedFenceValue(nativeHandle);
        });
    }

    public boolean isComplete(long value) {
        requireFenceValue(value, "value");
        return completedValue() >= value;
    }

    public void signal(long value) {
        device.withLifecycleLock(() -> {
            ensureOpenLocked();
            requireFenceValue(value, "value");
            D3D12Exception.check(
                    D3D12Native.nSignalSharedFence(nativeHandle, value),
                    "Signal shared D3D12 fence");
        });
    }

    public void waitFor(long value) {
        waitFor(value, D3D12Native.WAIT_INFINITE);
    }

    public void waitFor(long value, int timeoutMilliseconds) {
        device.withLifecycleLock(() -> {
            ensureOpenLocked();
            requireFenceValue(value, "value");
            if (timeoutMilliseconds < 0 &&
                    timeoutMilliseconds != D3D12Native.WAIT_INFINITE) {
                throw new IllegalArgumentException(
                        "timeoutMilliseconds must be nonnegative or -1");
            }
            D3D12Exception.check(
                    D3D12Native.nWaitSharedFence(
                            nativeHandle, value, timeoutMilliseconds),
                    "Wait for shared D3D12 fence");
        });
    }

    @Override
    public long handle() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return nativeFence;
        });
    }

    long nativeHandle() {
        return device.withLifecycleLock(this::nativeHandleLocked);
    }

    long nativeHandleLocked() {
        ensureOpenLocked();
        return nativeHandle;
    }

    private static void requireFenceValue(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private void ensureOpenLocked() {
        device.assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            throw new IllegalStateException("D3D12 fence is destroyed");
        }
        device.ensureOpenLocked();
    }

    @Override
    public void destroy() {
        device.withLifecycleLock(this::destroyLocked);
    }

    void destroyLocked() {
        device.assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            device.onFenceDestroyedLocked(this);
            return;
        }
        if (externalBorrowCount != 0) {
            throw new IllegalStateException(
                    "Cannot destroy the D3D12 fence while " +
                            externalBorrowCount + " external borrow(s) are active");
        }
        D3D12Native.nDestroySharedFence(nativeHandle);
        nativeHandle = 0;
        device.onFenceDestroyedLocked(this);
    }

    @Override
    public void close() {
        destroy();
    }

    static final class ExternalBorrowLease implements AutoCloseable {
        private final D3D12Fence owner;
        private boolean closed;

        private ExternalBorrowLease(D3D12Fence owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            owner.device.withLifecycleLock(() ->
                    owner.releaseExternalBorrowLocked(this));
        }
    }
}
