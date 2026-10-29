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

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFilterMode;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureMipmapSettings;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.impl.texture.TextureWrapMode;
import io.homo.superresolution.srapi.SRSurfaceFormat;

import java.util.Objects;

public final class D3D12Texture2D implements ITexture, AutoCloseable {
    private final D3D12Device device;
    private final TextureDescription description;
    private final SRSurfaceFormat surfaceFormat;
    private final int dxgiFormat;
    private final int resourceFlags;
    private final boolean shared;
    private long nativeResource;
    private long allocationSize;
    private long sharedHandle;
    private long nativeHandle;
    private int externalBorrowCount;

    D3D12Texture2D(
            D3D12Device device,
            TextureDescription description,
            SRSurfaceFormat surfaceFormat,
            int dxgiFormat,
            int resourceFlags,
            boolean shared) {
        this.device = Objects.requireNonNull(device, "device");
        device.assertLifecycleLockHeld();
        this.description = Objects.requireNonNull(description, "description");
        this.surfaceFormat = Objects.requireNonNull(surfaceFormat, "surfaceFormat");
        this.dxgiFormat = dxgiFormat;
        this.resourceFlags = resourceFlags;
        this.shared = shared;
    }

    void initializeLocked(int mipLevels, D3D12ResourceState initialState) {
        device.assertLifecycleLockHeld();
        Objects.requireNonNull(initialState, "initialState");
        if (nativeHandle != 0) {
            throw new IllegalStateException("D3D12 Texture2D is already initialized");
        }
        nativeHandle = D3D12Exception.requireHandle(
                D3D12Native.nCreateTexture2D(
                        device.nativeHandleLocked(),
                        description.getWidth(),
                        description.getHeight(),
                        mipLevels,
                        surfaceFormat.value,
                        resourceFlags,
                        initialState.nativeCode(),
                        shared),
                "Create D3D12 Texture2D");
        long queriedResource = D3D12Native.nGetNativeTextureResource(nativeHandle);
        long queriedSize = D3D12Native.nGetTextureAllocationSize(nativeHandle);
        long queriedSharedHandle = shared
                ? D3D12Native.nGetTextureSharedHandle(nativeHandle)
                : 0;
        if (queriedResource == 0 || queriedSize <= 0 || (shared && queriedSharedHandle == 0)) {
            throw D3D12Exception.fromLastError(
                    "Query D3D12 Texture2D metadata", null);
        }
        this.nativeResource = queriedResource;
        this.allocationSize = queriedSize;
        this.sharedHandle = queriedSharedHandle;
    }

    public D3D12Device device() {
        return device;
    }

    public SRSurfaceFormat surfaceFormat() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return surfaceFormat;
        });
    }

    public int dxgiFormat() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return dxgiFormat;
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

    /**
     * Returns the Win32 NT handle borrowed from the native texture owner.
     * The caller must not close it and must not retain it past this texture.
     */
    public long sharedHandle() {
        return device.withLifecycleLock(this::sharedHandleLocked);
    }

    long sharedHandleLocked() {
        ensureOpenLocked();
        if (!shared) {
            throw new IllegalStateException("D3D12 Texture2D was not created as shared");
        }
        return sharedHandle;
    }

    ExternalBorrowLease borrowExternal() {
        return device.withLifecycleLock(this::borrowExternalLocked);
    }

    private ExternalBorrowLease borrowExternalLocked() {
        ensureOpenLocked();
        if (externalBorrowCount == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Too many active D3D12 Texture2D external borrows");
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
                    "D3D12 Texture2D external borrow count underflow");
        }
        --externalBorrowCount;
        device.releaseExternalBorrowLocked();
        lease.closed = true;
    }

    public long allocationSize() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return allocationSize;
        });
    }

    public D3D12ResourceState initialState() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return D3D12ResourceState.fromNativeCode(
                    D3D12Native.nGetTextureInitialState(nativeHandle));
        });
    }

    public D3D12ResourceState committedState() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return D3D12ResourceState.fromNativeCode(
                    D3D12Native.nGetTextureCommittedState(nativeHandle));
        });
    }

    public void assumeCommittedState(D3D12ResourceState state) {
        device.withLifecycleLock(() -> assumeCommittedStateLocked(state));
    }

    void assumeCommittedStateLocked(D3D12ResourceState state) {
        ensureOpenLocked();
        Objects.requireNonNull(state, "state");
        D3D12Exception.check(
                D3D12Native.nSetTextureCommittedState(nativeHandle, state.nativeCode()),
                "Set D3D12 Texture2D committed state");
        device.onTextureStateChanged(this, state);
    }

    @Override
    public TextureFormat getTextureFormat() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getFormat();
        });
    }

    @Override
    public TextureUsages getTextureUsages() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getUsages();
        });
    }

    @Override
    public TextureType getTextureType() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getType();
        });
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getFilterMode();
        });
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getWrapMode();
        });
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getMipmapSettings();
        });
    }

    @Override
    public TextureDescription getTextureDescription() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description;
        });
    }

    @Override
    public int getWidth() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getWidth();
        });
    }

    @Override
    public int getHeight() {
        return device.withLifecycleLock(() -> {
            ensureOpenLocked();
            return description.getHeight();
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

    boolean isDestroyedLocked() {
        device.assertLifecycleLockHeld();
        return nativeHandle == 0;
    }

    private void ensureOpenLocked() {
        device.assertLifecycleLockHeld();
        if (nativeHandle == 0) {
            throw new IllegalStateException("D3D12 Texture2D is destroyed");
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
            device.onTextureDestroyedLocked(this);
            return;
        }
        if (externalBorrowCount != 0) {
            throw new IllegalStateException(
                    "Cannot destroy the D3D12 Texture2D while " +
                            externalBorrowCount + " external borrow(s) are active");
        }
        D3D12Native.nDestroyTexture2D(nativeHandle);
        nativeHandle = 0;
        device.onTextureDestroyedLocked(this);
    }

    @Override
    public void close() {
        destroy();
    }

    static final class ExternalBorrowLease implements AutoCloseable {
        private final D3D12Texture2D owner;
        private boolean closed;

        private ExternalBorrowLease(D3D12Texture2D owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            owner.device.withLifecycleLock(() ->
                    owner.releaseExternalBorrowLocked(this));
        }
    }
}
