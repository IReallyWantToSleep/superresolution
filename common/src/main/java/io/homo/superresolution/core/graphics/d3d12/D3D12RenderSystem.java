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

import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.utils.ThrowableUtil;

public final class D3D12RenderSystem implements IRenderSystem, AutoCloseable {
    public static final int DEBUG_NONE = D3D12Native.DEBUG_NONE;
    public static final int DEBUG_LAYER = D3D12Native.DEBUG_LAYER;
    public static final int DEBUG_GPU_VALIDATION = D3D12Native.DEBUG_GPU_VALIDATION;
    public static final int DEBUG_DRED = D3D12Native.DEBUG_DRED;

    private final long adapterLuid;
    private final int debugFlags;
    private D3D12Device device;
    private boolean initialized;
    private boolean destroyed;

    public D3D12RenderSystem(long adapterLuid) {
        this(adapterLuid, DEBUG_NONE);
    }

    public D3D12RenderSystem(long adapterLuid, int debugFlags) {
        if (adapterLuid == 0) {
            throw new IllegalArgumentException("D3D12 adapter LUID must be nonzero");
        }
        this.adapterLuid = adapterLuid;
        this.debugFlags = debugFlags;
    }

    @Override
    public synchronized void initRenderSystem() {
        if (destroyed) {
            throw new IllegalStateException("D3D12 render system is destroyed");
        }
        if (initialized) {
            return;
        }
        device = new D3D12Device(adapterLuid, debugFlags);
        initialized = true;
    }

    @Override
    public synchronized void destroyRenderSystem() {
        if (destroyed) {
            return;
        }
        D3D12Device deviceToDestroy = device;
        Throwable failure = null;
        try {
            if (deviceToDestroy != null) {
                deviceToDestroy.destroy();
            }
        } catch (Throwable throwable) {
            failure = throwable;
            if (deviceToDestroy != null && !deviceToDestroy.isDestroyed()) {
                rethrowDestroyFailure(throwable);
            }
        }
        device = null;
        destroyed = true;
        rethrowDestroyFailure(failure);
    }

    @Override
    public synchronized D3D12Device device() {
        if (destroyed) {
            throw new IllegalStateException("D3D12 render system is destroyed");
        }
        if (!initialized || device == null) {
            throw new IllegalStateException("D3D12 render system is not initialized");
        }
        device.ensureOpen();
        return device;
    }

    @Override
    public synchronized void finish() {
        device().waitIdle();
    }

    public synchronized boolean isDestroyed() {
        return destroyed;
    }

    public synchronized boolean isInitialized() {
        return initialized && !destroyed && device != null;
    }

    @Override
    public void close() {
        destroyRenderSystem();
    }

    private static void rethrowDestroyFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        ThrowableUtil.rethrowError(failure);
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new D3D12Exception("Failed to destroy D3D12 render system: " + failure);
    }
}
