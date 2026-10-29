/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.presentation.window;

#if MC_VER >= MC_26_3
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import org.lwjgl.sdl.SDLVideo;
import static org.lwjgl.system.MemoryUtil.NULL;
#elif MC_VER >= MC_26_1
import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuDevice;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUS_ON_SHOW;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.system.MemoryUtil.NULL;
#endif

#if MC_VER >= MC_26_1
public final class VulkanPresentationGlBackend extends GlBackend {
#if MC_VER >= MC_26_3
    private static final long SDL_WINDOW_OPENGL = 2L;
    private static final long SDL_WINDOW_VULKAN = 268435456L;
    private static final String HELPER_TITLE = "Super Resolution OpenGL Context";

    private long presentationWindow = NULL;
    private boolean creatingHelperWindow;
    private long renderWindow = NULL;

    @Override
    public long createWindow(String title, int width, int height, long flags) {
        if (creatingHelperWindow) {
            long helper = super.createWindow(title, width, height, flags);
            if (renderWindow == NULL) {
                renderWindow = helper;
                PresentationWindowState.attachRender(helper);
            }
            return helper;
        }

        if (presentationWindow != NULL) {
            return super.createWindow(title, width, height, flags);
        }

        long window = SDLVideo.SDL_CreateWindow(
                title,
                width,
                height,
                (flags & ~SDL_WINDOW_OPENGL) | SDL_WINDOW_VULKAN
        );
        if (window == NULL) {
            throw new IllegalStateException("Failed to create the Vulkan presentation window");
        }
        presentationWindow = window;
        PresentationWindowState.attachPresentation(window);
        return window;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
        creatingHelperWindow = true;
        try {
            GpuDevice device = super.createDevice(debugOptions);
            if (renderWindow == NULL) {
                throw new IllegalStateException("RenderPearl did not create an OpenGL helper window");
            }
            return device;
        } catch (Throwable throwable) {
            PresentationBackendManager.disableAfterFailure(throwable);
            return throwBackendCreationFailure(throwable);
        } finally {
            creatingHelperWindow = false;
        }
    }

    private static <T> T throwBackendCreationFailure(Throwable cause) throws BackendCreationException {
        if (cause instanceof BackendCreationException failure) {
            throw failure;
        }
        BackendCreationException failure = new BackendCreationException(
                "Failed to initialize Vulkan presentation: " + cause.getMessage(),
                BackendCreationException.Reason.OTHER
        );
        failure.addSuppressed(cause);
        throw failure;
    }
#else
    private static final String HELPER_TITLE = "Super Resolution OpenGL Context";

    @Override
    public void setWindowHints() {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    }

    #if !(MC_VER >= MC_26_2)
    @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {
        String message = "Failed to create the Vulkan presentation window";
        if (error != null) {
            message += ": " + error;
        }
        throw new BackendCreationException(message);
    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions) {
        long helper = NULL;
        try {
            if (window == NULL) {
                throw new IllegalStateException("Minecraft provided a null presentation window");
            }
            PresentationWindowState.attachPresentation(window);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
            super.setWindowHints();

            helper = GLFW.glfwCreateWindow(1, 1, HELPER_TITLE, NULL, NULL);
            if (helper == NULL) {
                throw new IllegalStateException("Failed to create the hidden OpenGL helper window");
            }
            PresentationWindowState.attachRender(helper);

            GpuDevice device = super.createDevice(helper, defaultShaderSource, debugOptions);
            validateHandles(window, helper);
            return device;
        } catch (Throwable throwable) {
            if (helper != NULL && !PresentationWindowState.isRender(helper)) {
                GLFW.glfwDestroyWindow(helper);
            }
            PresentationWindowState.resetAfterStartupFailure();
            PresentationBackendManager.disableAfterFailure(throwable);
            return throwBackendCreationFailure(throwable);
        } finally {
            GLFW.glfwDefaultWindowHints();
        }
    }

    private static void validateHandles(long presentation, long render) {
        if (presentation == render) {
            throw new IllegalStateException("Presentation and render windows unexpectedly share a handle");
        }
        if (GLFW.glfwGetWindowAttrib(presentation, GLFW_CLIENT_API) != GLFW_NO_API) {
            throw new IllegalStateException("Minecraft presentation window was not created with GLFW_NO_API");
        }
        if (GLFW.glfwGetWindowAttrib(render, GLFW_CLIENT_API) != GLFW_OPENGL_API) {
            throw new IllegalStateException("Hidden render window does not own an OpenGL context");
        }
        if (GLFW.glfwGetCurrentContext() != render) {
            throw new IllegalStateException("Hidden render window is not the current OpenGL context");
        }
    }

    private static <T> T throwBackendCreationFailure(Throwable cause) {
        BackendCreationException failure = new BackendCreationException(
                "Failed to initialize Vulkan presentation: " + cause.getMessage()
        );
        failure.addSuppressed(cause);
        return VulkanPresentationGlBackend.<RuntimeException, T>sneakyThrow(failure);
    }
    #else
        @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {
        String message = "Failed to create the Vulkan presentation window";
        if (error != null) {
            message += ": " + error;
        }
        throw new BackendCreationException(message,BackendCreationException.Reason.OTHER);
    }

    @Override
    public @NonNull GpuDevice createDevice(
            long window,
            @NonNull ShaderSource defaultShaderSource,
            @NonNull GpuDebugOptions debugOptions,
            @NonNull Runnable criticalShaderLoader) {
        long helper = NULL;
        try {
            if (window == NULL) {
                throw new IllegalStateException("Minecraft provided a null presentation window");
            }
            PresentationWindowState.attachPresentation(window);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
            super.setWindowHints();

            helper = GLFW.glfwCreateWindow(1, 1, HELPER_TITLE, NULL, NULL);
            if (helper == NULL) {
                throw new IllegalStateException("Failed to create the hidden OpenGL helper window");
            }
            PresentationWindowState.attachRender(helper);

            GpuDevice device = super.createDevice(helper, defaultShaderSource, debugOptions,criticalShaderLoader);
            validateHandles(window, helper);
            return device;
        } catch (Throwable throwable) {
            if (helper != NULL && !PresentationWindowState.isRender(helper)) {
                GLFW.glfwDestroyWindow(helper);
            }
            PresentationWindowState.resetAfterStartupFailure();
            PresentationBackendManager.disableAfterFailure(throwable);
            return throwBackendCreationFailure(throwable);
        } finally {
            GLFW.glfwDefaultWindowHints();
        }
    }

    private static void validateHandles(long presentation, long render) {
        if (presentation == render) {
            throw new IllegalStateException("Presentation and render windows unexpectedly share a handle");
        }
        if (GLFW.glfwGetWindowAttrib(presentation, GLFW_CLIENT_API) != GLFW_NO_API) {
            throw new IllegalStateException("Minecraft presentation window was not created with GLFW_NO_API");
        }
        if (GLFW.glfwGetWindowAttrib(render, GLFW_CLIENT_API) != GLFW_OPENGL_API) {
            throw new IllegalStateException("Hidden render window does not own an OpenGL context");
        }
        if (GLFW.glfwGetCurrentContext() != render) {
            throw new IllegalStateException("Hidden render window is not the current OpenGL context");
        }
    }

    private static <T> T throwBackendCreationFailure(Throwable cause) {
        BackendCreationException failure = new BackendCreationException(
                "Failed to initialize Vulkan presentation: " + cause.getMessage(),
                BackendCreationException.Reason.OTHER
        );
        failure.addSuppressed(cause);
        return VulkanPresentationGlBackend.<RuntimeException, T>sneakyThrow(failure);
    }
    #endif

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }
#endif
}
#endif
