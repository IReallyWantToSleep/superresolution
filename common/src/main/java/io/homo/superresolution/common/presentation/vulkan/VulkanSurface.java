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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import org.lwjgl.PointerBuffer;
#if MC_VER >= MC_26_3
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
#else
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWVulkan;
#endif
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.User32;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

#if MC_VER < MC_26_3
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
#endif
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public final class VulkanSurface {
    private long handle = VK_NULL_HANDLE;
    private long surface = VK_NULL_HANDLE;
    private boolean shown;
    private boolean framebufferResized;
    private int framebufferWidth = 1;
    private int framebufferHeight = 1;

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("windows");
    }

    public boolean isShown() {
        return shown;
    }

    public long getSurface() {
        return surface;
    }

    public long getHandle() {
        return handle;
    }

    public void attach(long presentationHandle) {
        if (presentationHandle == VK_NULL_HANDLE) {
            throw new IllegalArgumentException("Presentation handle must not be null");
        }
        if (handle == presentationHandle) {
            return;
        }
        if (handle != VK_NULL_HANDLE) {
            throw new IllegalStateException("Vulkan presentation window is already attached");
        }
        #if MC_VER >= MC_26_3
        if ((SDLVideo.SDL_GetWindowFlags(presentationHandle) & SDLVideo.SDL_WINDOW_VULKAN) == 0L) {
            throw new IllegalStateException("Vulkan presentation window must use SDL_WINDOW_VULKAN");
        }
        #else
        if (GLFW.glfwGetWindowAttrib(presentationHandle, GLFW_CLIENT_API) != GLFW_NO_API) {
            throw new IllegalStateException("Vulkan presentation window must use GLFW_NO_API");
        }
        #endif
        if (presentationHandle != MinecraftWindow.getWindowHandle()) {
            throw new IllegalStateException("Presentation handle is not the Minecraft window");
        }
        if (presentationHandle != PresentationWindowState.presentationHandle()) {
            throw new IllegalStateException("Presentation handle does not match the backend bridge");
        }
        if (PresentationWindowState.isRender(presentationHandle)) {
            throw new IllegalStateException("Presentation handle unexpectedly owns the OpenGL context");
        }
        handle = presentationHandle;
        refreshFramebufferSize();
        framebufferResized = false;
    }

    public PointerBuffer requiredInstanceExtensions() {
        #if MC_VER >= MC_26_3
        PointerBuffer extensions = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
        if (extensions == null || !extensions.hasRemaining()) {
            throw new IllegalStateException("SDL did not provide required Vulkan instance extensions");
        }
        return extensions;
        #else
        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new IllegalStateException("GLFW Vulkan support is unavailable");
        }
        PointerBuffer extensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (extensions == null || !extensions.hasRemaining()) {
            throw new IllegalStateException("GLFW did not provide required Vulkan instance extensions");
        }
        return extensions;
        #endif
    }

    public void createSurface(VkInstance instance) {
        if (surface != VK_NULL_HANDLE) {
            return;
        }
        if (handle == VK_NULL_HANDLE) {
            throw new IllegalStateException("Presentation window must be attached before creating its surface");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer surfacePointer = stack.mallocLong(1);
            #if MC_VER >= MC_26_3
            if (!SDLVulkan.SDL_Vulkan_CreateSurface(handle, instance, null, surfacePointer)) {
                throw new IllegalStateException(
                        "Failed to create the Vulkan surface for SDL handle " + handle
                );
            }
            #else
            requiredInstanceExtensions();
            int result = isWindows()
                    ? createHookedWin32Surface(instance, stack, surfacePointer)
                    : GLFWVulkan.glfwCreateWindowSurface(instance, handle, null, surfacePointer);
            if (result != VK_SUCCESS) {
                throw new IllegalStateException(
                        "Failed to create the Vulkan surface for GLFW handle "
                                + handle
                                + ", VkResult="
                                + result
                );
            }
            #endif
            surface = surfacePointer.get(0);
        }
    }

    #if MC_VER < MC_26_3
    private int createHookedWin32Surface(
            VkInstance instance,
            MemoryStack stack,
            LongBuffer surfacePointer
    ) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(handle);
        long hinstance = User32.GetWindowLongPtr(hwnd, User32.GWL_HINSTANCE);
        if (hwnd == 0L || hinstance == 0L) {
            throw new IllegalStateException(
                    "Failed to resolve Win32 handles for the Vulkan presentation window"
            );
        }
        long function = VK10.vkGetInstanceProcAddr(
                instance,
                "vkCreateWin32SurfaceKHR"
        );
        if (function == 0L) {
            throw new IllegalStateException("vkCreateWin32SurfaceKHR is unavailable");
        }
        VkWin32SurfaceCreateInfoKHR createInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                .sType(KHRWin32Surface.VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                .hinstance(hinstance)
                .hwnd(hwnd);
        return JNI.callPPPPI(
                instance.address(),
                createInfo.address(),
                0L,
                MemoryUtil.memAddress(surfacePointer),
                function
        );
    }
    #endif

    public void recreateSurface(VkInstance instance) {
        destroySurface(instance);
        createSurface(instance);
    }

    public void refreshFramebufferSize() {
        if (handle == VK_NULL_HANDLE) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            #if MC_VER >= MC_26_3
            SDLVideo.SDL_GetWindowSizeInPixels(handle, width, height);
            #else
            GLFW.glfwGetFramebufferSize(handle, width, height);
            #endif
            int newWidth = width.get(0);
            int newHeight = height.get(0);
            if (newWidth != framebufferWidth || newHeight != framebufferHeight) {
                framebufferWidth = newWidth;
                framebufferHeight = newHeight;
                framebufferResized = true;
            }
        }
    }

    public boolean consumeFramebufferResized() {
        boolean resized = framebufferResized;
        framebufferResized = false;
        return resized;
    }

    public void show() {
        if (shown || handle == VK_NULL_HANDLE) {
            return;
        }
        #if MC_VER >= MC_26_3
        SDLVideo.SDL_ShowWindow(handle);
        SDLVideo.SDL_RaiseWindow(handle);
        #else
        GLFW.glfwShowWindow(handle);
        GLFW.glfwFocusWindow(handle);
        #endif
        shown = true;
    }

    public boolean shouldClose() {
        #if MC_VER >= MC_26_3
        return handle != VK_NULL_HANDLE
                && net.minecraft.client.Minecraft.getInstance().getWindow().shouldClose();
        #else
        return handle != VK_NULL_HANDLE && GLFW.glfwWindowShouldClose(handle);
        #endif
    }

    public boolean isMinimized() {
        if (handle == VK_NULL_HANDLE) {
            return false;
        }
        #if MC_VER >= MC_26_3
        return (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MINIMIZED) != 0L
                || framebufferWidth <= 0
                || framebufferHeight <= 0;
        #else
        return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
                || framebufferWidth <= 0
                || framebufferHeight <= 0;
        #endif
    }

    public void destroySurface(VkInstance instance) {
        if (surface == VK_NULL_HANDLE) {
            return;
        }
        if (isWindows()) {
            long function = VK10.vkGetInstanceProcAddr(
                    instance,
                    "vkDestroySurfaceKHR"
            );
            if (function == 0L) {
                throw new IllegalStateException("vkDestroySurfaceKHR is unavailable");
            }
            JNI.callPJPV(instance.address(), surface, 0L, function);
        } else {
            KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        }
        surface = VK_NULL_HANDLE;
    }

    public long handle() {
        return handle;
    }

    public long surface() {
        return surface;
    }

    public int framebufferWidth() {
        return framebufferWidth;
    }

    public int framebufferHeight() {
        return framebufferHeight;
    }
}
