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

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsages;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.utils.ThrowableUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class D3D12Buffer implements IBuffer, AutoCloseable {
    public enum Heap {
        DEFAULT(D3D12Native.BUFFER_HEAP_DEFAULT),
        UPLOAD(D3D12Native.BUFFER_HEAP_UPLOAD),
        READBACK(D3D12Native.BUFFER_HEAP_READBACK);

        private final int nativeCode;

        Heap(int nativeCode) {
            this.nativeCode = nativeCode;
        }

        int nativeCode() {
            return nativeCode;
        }
    }

    private final D3D12Device device;
    private final BufferDescription description;
    private final Heap heap;
    private final D3D12ResourceState initialState;
    private final int resourceFlags;
    private final boolean shared;
    private long nativeResource;
    private long sharedHandle;
    private long nativeHandle;
    private ByteBuffer mappedData;
    private int mappedOffset;
    private int mappedLength;
    private boolean mappedCommitComplete;
    private D3D12Device.ExternalBorrowLease mappedDeviceBorrow;

    D3D12Buffer(
            D3D12Device device,
            BufferDescription description,
            Heap heap,
            D3D12ResourceState initialState,
            int resourceFlags,
            boolean shared) {
        this.device = Objects.requireNonNull(device, "device");
        device.assertLifecycleLockHeld();
        this.description = Objects.requireNonNull(description, "description");
        this.heap = Objects.requireNonNull(heap, "heap");
        this.initialState = Objects.requireNonNull(initialState, "initialState");
        this.resourceFlags = resourceFlags;
        this.shared = shared;
    }

    void initializeLocked() {
        device.assertLifecycleLockHeld();
        if (nativeHandle != 0) {
            throw new IllegalStateException("D3D12 buffer is already initialized");
        }
        nativeHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateBuffer(
                        device.nativeHandleLocked(),
                        description.size(),
                        heap.nativeCode(),
                        resourceFlags,
                        initialState.nativeCode(),
                        shared),
                "Create D3D12 buffer");
        long queriedResource = D3D12Native.nGetNativeBufferResource(nativeHandle);
        long queriedSharedHandle = shared
                ? D3D12Native.nGetBufferSharedHandle(nativeHandle)
                : 0;
        if (queriedResource == 0 || (shared && queriedSharedHandle == 0)) {
            throw D3D12Exception.fromLastError(
                    "Query D3D12 buffer metadata", null);
        }
        this.nativeResource = queriedResource;
        this.sharedHandle = queriedSharedHandle;
    }

    public D3D12Device device() {
        return device;
    }

    public Heap heap() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return heap;
        });
    }

    public D3D12ResourceState initialState() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return initialState;
        });
    }

    public int resourceFlags() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return resourceFlags;
        });
    }

    public boolean isShared() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return shared;
        });
    }

    public long sharedHandle() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            if (!shared) {
                throw new IllegalStateException("D3D12 buffer was not created as shared");
            }
            return sharedHandle;
        });
    }

    @Override
    public long getSize() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.size();
        });
    }

    long sizeLocked() {
        ensureOpenLocked();
        return description.size();
    }

    @Override
    public BufferUsages getUsages() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.usage();
        });
    }

    @Override
    public ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write) {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            if (!write) {
                throw D3D12Exception.unsupported("buffer read mapping");
            }
            if (mappedData != null) {
                throw new IllegalStateException("D3D12 buffer is already mapped");
            }
            if (offsetInBytes < 0 || lengthInBytes < 0 ||
                    (long) offsetInBytes + lengthInBytes > description.size()) {
                throw new IllegalArgumentException(
                        "D3D12 buffer map range is out of bounds");
            }

            ByteBuffer data;
            D3D12Device.ExternalBorrowLease deviceBorrow;
            if (heap == Heap.UPLOAD) {
                deviceBorrow = device.borrowExternalLocked();
                boolean nativeMapped = false;
                try {
                    data = D3D12Native.nMapBuffer(
                            nativeHandle,
                            offsetInBytes,
                            lengthInBytes);
                    if (data == null) {
                        throw D3D12Exception.fromLastError(
                                "Map D3D12 UPLOAD buffer",
                                null);
                    }
                    nativeMapped = true;
                    data.order(ByteOrder.nativeOrder());
                } catch (Throwable failure) {
                    if (nativeMapped) {
                        try {
                            D3D12Exception.check(
                                    D3D12Native.nUnmapBuffer(
                                            nativeHandle,
                                            offsetInBytes,
                                            lengthInBytes),
                                    "Roll back D3D12 UPLOAD buffer map");
                        } catch (Throwable unmapFailure) {
                            if (failure != unmapFailure) {
                                failure.addSuppressed(unmapFailure);
                            }
                        }
                    }
                    try {
                        deviceBorrow.close();
                    } catch (Throwable borrowFailure) {
                        if (failure != borrowFailure) {
                            failure.addSuppressed(borrowFailure);
                        }
                    }
                    ThrowableUtil.rethrowError(failure);
                    throw failure;
                }
            } else {
                data = ByteBuffer.allocateDirect(lengthInBytes)
                        .order(ByteOrder.nativeOrder());
                deviceBorrow = device.borrowExternalLocked();
            }

            mappedOffset = offsetInBytes;
            mappedLength = lengthInBytes;
            mappedCommitComplete = false;
            mappedData = data;
            mappedDeviceBorrow = deviceBorrow;
            return data;
        });
    }

    @Override
    public void unmap() {
        device.withLifecycleLock(() -> {
            ensureOpenLocked();
            if (mappedData == null) {
                throw new IllegalStateException("D3D12 buffer is not mapped");
            }
            if (!mappedCommitComplete) {
                if (heap == Heap.UPLOAD) {
                    D3D12Exception.check(
                            D3D12Native.nUnmapBuffer(
                                    nativeHandle,
                                    mappedOffset,
                                    mappedLength),
                            "Unmap D3D12 UPLOAD buffer");
                } else if (mappedLength > 0) {
                    ByteBuffer upload = mappedData.duplicate().order(ByteOrder.nativeOrder());
                    upload.clear();
                    device.uploadMappedBufferLocked(
                            this, mappedOffset, mappedLength, upload);
                }
                mappedCommitComplete = true;
            }
            D3D12Device.ExternalBorrowLease deviceBorrow = mappedDeviceBorrow;
            if (deviceBorrow == null) {
                throw new IllegalStateException(
                        "D3D12 buffer mapping lost its device borrow");
            }
            deviceBorrow.close();
            mappedData = null;
            mappedOffset = 0;
            mappedLength = 0;
            mappedCommitComplete = false;
            mappedDeviceBorrow = null;
        });
    }

    @Override
    public long handle() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return nativeResource;
        });
    }

    long nativeHandle() {
        return device.withLifecycleLock(this::nativeHandleLocked);
    }

    long nativeHandleLocked() {
        ensureOpenLocked();
        return nativeHandle;
    }

    private void ensureOpenLocked() {
        device.assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            throw new IllegalStateException("D3D12 buffer is destroyed");
        }
        device.ensureOpenLocked();
    }

    @Override
    public void destroy() {
        device.withLifecycleLock(() -> destroyLocked(false));
    }

    void destroyLocked(boolean allowMapped) {
        device.assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            device.onBufferDestroyedLocked(this);
            return;
        }
        if (mappedData != null && !allowMapped) {
            throw new IllegalStateException("Cannot destroy a mapped D3D12 buffer");
        }
        D3D12Native.nDestroyBuffer(nativeHandle);
        nativeHandle = 0;
        mappedData = null;
        mappedOffset = 0;
        mappedLength = 0;
        mappedCommitComplete = false;
        mappedDeviceBorrow = null;
        device.onBufferDestroyedLocked(this);
    }

    @Override
    public void close() {
        destroy();
    }
}
