/*
 * Super Resolution
 * Copyright (c) 2026. Xiang Keshen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.utils.ThrowableUtil;

import java.util.Objects;

import org.lwjgl.opengl.GL20;

import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.*;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glGetError;

/**
 * OpenGL semaphore imported from the shared ID3D12Fence owned by a
 * {@link D3D12Fence}.
 */
public final class D3D12InteropSemaphore implements AutoCloseable {
    private static final int[] NO_BUFFERS = new int[0];
    private final D3D12Fence owningFence;
    private int semaphore;
    private D3D12Fence.ExternalBorrowLease fenceBorrow;

    public D3D12InteropSemaphore(D3D12Fence fence) {
        owningFence = Objects.requireNonNull(fence, "fence");
        fenceBorrow = owningFence.borrowExternal();
    }

    /**
     * @deprecated pass the owning {@link D3D12Fence} instead.
     */
    @Deprecated
    public D3D12InteropSemaphore(long sharedFenceHandle) {
        owningFence = null;
        try {
            initialize(sharedFenceHandle);
        } catch (Throwable throwable) {
            try {
                close();
            } catch (Throwable cleanupFailure) {
                if (throwable != cleanupFailure) {
                    throwable.addSuppressed(cleanupFailure);
                }
            }
            ThrowableUtil.rethrowError(throwable);
            throw throwable;
        }
    }

    public synchronized void initializeImport() {
        if (owningFence == null) {
            throw new IllegalStateException(
                    "This OpenGL/D3D12 semaphore has no owning fence.");
        }
        owningFence.device().withLifecycleLock(() ->
                initialize(owningFence.sharedHandleLocked()));
    }

    private void initialize(long sharedFenceHandle) {
        if (sharedFenceHandle == 0) {
            throw new IllegalArgumentException("The D3D12 fence handle is null.");
        }
        if (semaphore != 0) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop semaphore is already initialized.");
        }
        clearGlErrors();
        semaphore = glGenSemaphoresEXT();
        if (semaphore == 0) {
            throw new IllegalStateException(
                    "Could not create an OpenGL interop semaphore.");
        }
        glImportSemaphoreWin32HandleEXT(
                semaphore,
                GL_HANDLE_TYPE_D3D12_FENCE_EXT,
                sharedFenceHandle);
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new IllegalStateException(
                    "Could not import the D3D12 fence into OpenGL " +
                            "(error 0x" + Integer.toHexString(error) + ").");
        }
    }

    private static void validate(
            long fenceValue,
            int[] textures,
            int[] layouts) {
        if (fenceValue <= 0) {
            throw new IllegalArgumentException("The D3D12 fence value must be positive.");
        }
        if (textures == null || layouts == null ||
                textures.length != layouts.length) {
            throw new IllegalArgumentException(
                    "Texture and layout arrays must be non-null and have equal length.");
        }
    }

    private static void clearGlErrors() {
        while (glGetError() != GL_NO_ERROR) {
            // Discard errors left by unrelated work so the import check below
            // reports only this operation.
        }
    }

    private static void checkGlError(String operation) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new IllegalStateException(
                    operation + " failed with OpenGL error 0x" +
                            Integer.toHexString(error) + ".");
        }
    }

    private void ensureOpen() {
        if (semaphore == 0) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 interop semaphore is closed.");
        }
    }

    public synchronized void signal(long fenceValue, int[] textures, int[] layouts) {
        ensureOpen();
        validate(fenceValue, textures, layouts);
        clearGlErrors();
        glSemaphoreParameterui64EXT(
                semaphore,
                GL_D3D12_FENCE_VALUE_EXT,
                fenceValue);
        glSignalSemaphoreEXT(
                semaphore,
                NO_BUFFERS,
                textures,
                layouts);
        checkGlError("Signal the OpenGL/D3D12 interop semaphore");
        GL20.glFlush();
    }

    public synchronized void waitFor(long fenceValue, int[] textures, int[] layouts) {
        ensureOpen();
        validate(fenceValue, textures, layouts);
        clearGlErrors();
        glSemaphoreParameterui64EXT(
                semaphore,
                GL_D3D12_FENCE_VALUE_EXT,
                fenceValue);
        glWaitSemaphoreEXT(
                semaphore,
                NO_BUFFERS,
                textures,
                layouts);
        checkGlError("Wait for the OpenGL/D3D12 interop semaphore");
    }

    public synchronized void waitForFenceOnly(long fenceValue) {
        waitFor(fenceValue, NO_BUFFERS, NO_BUFFERS);
    }

    @Override
    public synchronized void close() {
        if (semaphore != 0) {
            clearGlErrors();
            glDeleteSemaphoresEXT(semaphore);
            checkGlError("Delete the OpenGL/D3D12 interop semaphore");
            semaphore = 0;
        }
        if (fenceBorrow != null) {
            fenceBorrow.close();
            fenceBorrow = null;
        }
    }
}
