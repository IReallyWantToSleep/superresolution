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

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.core.graphics.GraphicsDevice;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanQueueUtils;
import io.homo.superresolution.core.graphics.vulkan.VulkanTimestampProfiler;
import org.lwjgl.PointerBuffer;
#if MC_VER >= MC_26_3
import org.lwjgl.sdl.SDLVideo;
#else
import org.lwjgl.glfw.GLFW;
#endif
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

#if MC_VER >= MC_26_1
import static org.lwjgl.vulkan.EXTPresentTiming.VK_EXT_PRESENT_TIMING_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRCalibratedTimestamps.VK_KHR_CALIBRATED_TIMESTAMPS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPresentId.VK_KHR_PRESENT_ID_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPresentId2.VK_KHR_PRESENT_ID_2_EXTENSION_NAME;
#else
import static org.lwjgl.vulkan.KHRPresentId.VK_KHR_PRESENT_ID_EXTENSION_NAME;
#endif
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.NVLowLatency2.VK_NV_LOW_LATENCY_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanPresentationFeature {
    private static final VulkanSurface SURFACE = new VulkanSurface();
    private static VkRenderSystem renderSystem;
    private static VkInstance surfaceInstance;
    private static boolean isAvailable;

    private VulkanPresentationFeature() {
    }

    public static boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Drains any Vulkan GPU timestamps that have completed. Called once per frame from the
     * render thread; a no-op unless detailed profiling is on and the device supports
     * timestamp queries.
     */
    public static void collectGpuTimestamps() {
        VkRenderSystem system = renderSystem;
        if (system == null) {
            return;
        }
        VulkanDevice device = system.device();
        if (device == null) {
            return;
        }
        VulkanTimestampProfiler profiler = device.timestampProfiler();
        if (profiler != null) {
            profiler.collect();
        }
    }

    public static boolean shouldInitializeStreamline() {
        return PresentationBackendManager.shouldInitializeStreamline();
    }

    public static synchronized void prepare(VkRenderSystem target) {
        if (!PresentationBackendManager.isVulkanPresentationRequested() || renderSystem != null) {
            return;
        }
        long presentationHandle = MinecraftWindow.getWindowHandle();
        if (presentationHandle != PresentationWindowState.presentationHandle()) {
            throw new IllegalStateException("Minecraft window does not match the Vulkan presentation handle");
        }
        SURFACE.attach(presentationHandle);
        PointerBuffer extensions = SURFACE.requiredInstanceExtensions();
        for (int i = extensions.position(); i < extensions.limit(); i++) {
            target.addInstanceExtension(MemoryUtil.memUTF8(extensions.get(i)));
        }
        target.addDeviceExtension(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        // Native Reflex (VK_NV_low_latency2) rides on the presentation swapchain
        // and needs present ids to correlate latency markers with presents.
        target.addDeviceExtension(VK_KHR_PRESENT_ID_EXTENSION_NAME);
        target.addDeviceExtension(VK_NV_LOW_LATENCY_2_EXTENSION_NAME);
#if MC_VER >= MC_26_1
        target.addInstanceExtension(VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME);
        target.addDeviceExtension(VK_KHR_PRESENT_ID_2_EXTENSION_NAME);
        target.addDeviceExtension(VK_KHR_CALIBRATED_TIMESTAMPS_EXTENSION_NAME);
        target.addDeviceExtension(VK_EXT_PRESENT_TIMING_EXTENSION_NAME);
#endif
        renderSystem = target;
    }

    /**
     * RenderPearl creates its surface before Super Resolution starts. Keep the
     * surface here so the game never constructs a second swapchain for this window.
     */
    public static synchronized VulkanSurface attachBorrowedWindow(long windowHandle, VkDevice device, int queueFamily) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            throw new IllegalStateException("Native surface takeover requires Vulkan presentation");
        }
        PresentationWindowState.attachNativePresentation(windowHandle);
        SURFACE.attach(windowHandle);
        VkInstance instance = device.getPhysicalDevice().getInstance();
        if (surfaceInstance != null && surfaceInstance.address() != instance.address()) {
            throw new IllegalStateException("Presentation surface belongs to another Vulkan instance");
        }
        surfaceInstance = instance;
        validateBorrowedCapabilities(device);
        SURFACE.createSurface(instance);
        validatePresentQueue(device.getPhysicalDevice(), queueFamily);
        return SURFACE;
    }

    public static synchronized void prepareBorrowed(VkRenderSystem target) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return;
        }
        if (renderSystem != null && renderSystem != target) {
            throw new IllegalStateException("Another Vulkan render system already owns presentation");
        }
        renderSystem = target;
        attachBorrowedWindow(
                MinecraftWindow.getWindowHandle(),
                target.device().getVkDevice(),
                target.device().getMainQueue().getQueueFamilyIndex()
        );
    }

    public static void validateBorrowedDevice(VkRenderSystem target) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return;
        }
        validateBorrowedCapabilities(target.device().getVkDevice());
        validatePresentQueue(target.device().getPhysicalDevice(), target.device().getMainQueue().getQueueFamilyIndex());
    }

    private static void validateBorrowedCapabilities(VkDevice device) {
        VkInstance instance = device.getPhysicalDevice().getInstance();
        if (!instance.getCapabilities().VK_KHR_surface
                || !device.getCapabilities().VK_KHR_swapchain
                || device.getCapabilities().vkCreateSwapchainKHR == VK_NULL_HANDLE
                || device.getCapabilities().vkAcquireNextImageKHR == VK_NULL_HANDLE
                || device.getCapabilities().vkQueuePresentKHR == VK_NULL_HANDLE) {
            throw new IllegalStateException("Minecraft's Vulkan instance/device did not enable surface and swapchain support");
        }
        // Borrowed devices cannot enable optional present-id, Reflex or present-timing
        // features after creation. Their SR feature flags remain disabled.
    }

    private static void validatePresentQueue(VkPhysicalDevice physicalDevice, int queueFamily) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer supported = stack.ints(0);
            int result = vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamily, SURFACE.surface(), supported);
            if (result != VK_SUCCESS || supported.get(0) == 0) {
                throw new IllegalStateException("Minecraft's Vulkan graphics queue cannot present to the SR surface, VkResult=" + result);
            }
        }
    }

    public static void createSurface(VkRenderSystem target) {
        if (PresentationBackendManager.isVulkanPresentationRequested()) {
            surfaceInstance = target.getVulkanInstance();
            SURFACE.createSurface(target.getVulkanInstance());
        }
    }

    public static int findGraphicsPresentQueueFamily(
            MemoryStack stack,
            int queueType,
            VkPhysicalDevice physicalDevice
    ) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return VulkanQueueUtils.findQueueFamilyIndex(stack, queueType, physicalDevice);
        }
        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
        VkQueueFamilyProperties.Buffer properties = VkQueueFamilyProperties.calloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, properties);
        IntBuffer presentSupported = stack.mallocInt(1);
        for (int i = 0; i < properties.capacity(); i++) {
            if ((properties.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) == 0) {
                continue;
            }
            presentSupported.put(0, 0);
            int result = vkGetPhysicalDeviceSurfaceSupportKHR(
                    physicalDevice,
                    i,
                    SURFACE.surface(),
                    presentSupported
            );
            if (result == VK_SUCCESS && presentSupported.get(0) != 0) {
                return i;
            }
        }
        throw new IllegalStateException("No Vulkan queue family supports graphics and presentation");
    }

    public static void validateDevice(VkRenderSystem target, VkPhysicalDevice physicalDevice) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return;
        }
        if (!target.getCapabilities().getDeviceExtensions().contains(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            throw new IllegalStateException("Selected Vulkan device does not support VK_KHR_swapchain");
        }
        #if MC_VER >= MC_26_3
        long currentContext = SDLVideo.SDL_GL_GetCurrentWindow();
        #else
        long currentContext = GLFW.glfwGetCurrentContext();
        #endif
        long renderHandle = PresentationWindowState.renderHandle();
        long presentationHandle = PresentationWindowState.presentationHandle();
        if (currentContext != renderHandle || currentContext == presentationHandle) {
            throw new IllegalStateException("OpenGL device validation is running on the wrong GLFW context");
        }
        if (GL.getCapabilities() == null) {
            throw new IllegalStateException("OpenGL capabilities are unavailable on the hidden render context");
        }

        GraphicsDevice glDevice = GraphicsDevice.createFromOpenGL();
        GraphicsDevice vkDevice = GraphicsDevice.createFromVulkan(physicalDevice);
        String glRenderer = GL11.glGetString(GL11.GL_RENDERER);
        SuperResolution.LOGGER.info(
                "Vulkan presentation={}, render={}, OpenGL renderer='{}', Vulkan device='{}'",
                presentationHandle,
                renderHandle,
                glRenderer,
                vkDevice.deviceName()
        );
        if (!vkDevice.isCompatibleWith(glDevice)) {
            throw new IllegalStateException(
                    "Selected Vulkan device '" + vkDevice.deviceName()
                            + "' is not compatible with OpenGL renderer '" + glRenderer + "'"
            );
        }
    }

    public static synchronized void completeInitialization(VkRenderSystem target) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return;
        }
        if (target.device() == null || target.getVulkanInstance() == null || SURFACE.surface() == 0L) {
            throw new IllegalStateException("Vulkan presentation initialization did not produce a usable device");
        }
        VulkanPresentationWindow.initialize(VulkanPresentationContext.initialize(target, SURFACE), SURFACE);
        isAvailable = true;
    }

    public static synchronized void shutdown() {
        VulkanPresentationWindow.shutdown();
        if (SURFACE.surface() != 0L && surfaceInstance != null) {
            SURFACE.destroySurface(surfaceInstance);
        }
        SURFACE.detach();
        surfaceInstance = null;
        renderSystem = null;
        isAvailable = false;
    }

}
