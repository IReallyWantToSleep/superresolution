/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.lwjgl.system.MemoryUtil.NULL;

public final class PresentationWindowState {
    private static long presentationHandle = NULL;
    private static long renderHandle = NULL;
    private static Thread ownerThread;

    private PresentationWindowState() {
    }

    public static synchronized void attachPresentation(long handle) {
        requireNonZero(handle, "presentation");
        attachOwnerThread();
        if (presentationHandle == handle) {
            return;
        }
        if (presentationHandle != NULL) {
            throw new IllegalStateException("Vulkan presentation handle is already attached");
        }
        if (handle == renderHandle) {
            throw new IllegalStateException("Presentation and render handles must be different");
        }
        presentationHandle = handle;
    }

    public static synchronized void attachRender(long handle) {
        requireNonZero(handle, "render");
        requireOwnerThread();
        if (renderHandle == handle) {
            return;
        }
        if (renderHandle != NULL) {
            throw new IllegalStateException("OpenGL render handle is already attached");
        }
        if (handle == presentationHandle) {
            throw new IllegalStateException("Presentation and render handles must be different");
        }
        renderHandle = handle;
    }

    public static synchronized long presentationHandle() {
        if (presentationHandle == NULL) {
            throw new IllegalStateException("Vulkan presentation handle is not attached");
        }
        return presentationHandle;
    }

    public static synchronized long renderHandle() {
        if (renderHandle == NULL) {
            throw new IllegalStateException("OpenGL render handle is not attached");
        }
        return renderHandle;
    }

    public static synchronized boolean isInitialized() {
        return presentationHandle != NULL && renderHandle != NULL;
    }

    public static synchronized boolean isRender(long handle) {
        return handle != NULL && handle == renderHandle;
    }

    public static synchronized void destroyRenderWindow() {
        if (renderHandle == NULL) {
            clearOwnerThreadIfUnused();
            return;
        }
        requireOwnerThread();
        long handle = renderHandle;
        renderHandle = NULL;
        if (GLFW.glfwGetCurrentContext() == handle) {
            GL.setCapabilities(null);
            GLFW.glfwMakeContextCurrent(NULL);
        }
        GLFW.glfwDestroyWindow(handle);
        clearOwnerThreadIfUnused();
    }

    public static synchronized void clearPresentationAfterWindowClose() {
        if (presentationHandle == NULL) {
            clearOwnerThreadIfUnused();
            return;
        }
        requireOwnerThread();
        presentationHandle = NULL;
        clearOwnerThreadIfUnused();
    }

    public static synchronized void resetAfterStartupFailure() {
        destroyRenderWindow();
        presentationHandle = NULL;
        clearOwnerThreadIfUnused();
    }

    public static synchronized void requireOwnerThread() {
        if (ownerThread == null || ownerThread != Thread.currentThread()) {
            throw new IllegalStateException("Presentation GLFW handles must be accessed on their owner thread");
        }
    }

    private static void attachOwnerThread() {
        Thread current = Thread.currentThread();
        if (ownerThread == null) {
            ownerThread = current;
        } else if (ownerThread != current) {
            throw new IllegalStateException("Presentation GLFW handles must be attached on the render thread");
        }
    }

    private static void requireNonZero(long handle, String kind) {
        if (handle == NULL) {
            throw new IllegalArgumentException(kind + " handle must not be null");
        }
    }

    private static void clearOwnerThreadIfUnused() {
        if (presentationHandle == NULL && renderHandle == NULL) {
            ownerThread = null;
        }
    }
}
