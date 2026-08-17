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

import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.GL_D3D12_FENCE_VALUE_EXT;
import static org.lwjgl.opengl.EXTSemaphoreWin32.GL_HANDLE_TYPE_D3D12_FENCE_EXT;
import static org.lwjgl.opengl.EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glGetError;

/**
 * OpenGL semaphore imported from the shared ID3D12Fence owned by a
 * {@link D3D12InteropContext}.
 */
public final class D3D12InteropSemaphore implements AutoCloseable {
    private int semaphore;

    public D3D12InteropSemaphore(long sharedFenceHandle) {
        if (sharedFenceHandle == 0) {
            throw new IllegalArgumentException("The D3D12 fence handle is null.");
        }
        clearGlErrors();
        try {
            semaphore = glGenSemaphoresEXT();
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
        } catch (Throwable throwable) {
            close();
            throw throwable;
        }
    }

    public void signal(long fenceValue, int[] textures, int[] layouts) {
        validate(textures, layouts);
        glSemaphoreParameterui64EXT(
                semaphore,
                GL_D3D12_FENCE_VALUE_EXT,
                fenceValue);
        glSignalSemaphoreEXT(
                semaphore,
                new int[0],
                textures,
                layouts);
    }

    public void waitFor(long fenceValue, int[] textures, int[] layouts) {
        validate(textures, layouts);
        glSemaphoreParameterui64EXT(
                semaphore,
                GL_D3D12_FENCE_VALUE_EXT,
                fenceValue);
        glWaitSemaphoreEXT(
                semaphore,
                new int[0],
                textures,
                layouts);
    }

    private static void validate(int[] textures, int[] layouts) {
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

    @Override
    public void close() {
        if (semaphore != 0) {
            glDeleteSemaphoresEXT(semaphore);
            semaphore = 0;
        }
    }
}
