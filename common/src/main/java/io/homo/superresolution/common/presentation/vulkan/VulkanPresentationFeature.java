/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.common.framegeneration.FrameGenerationDescriptions;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;
import io.homo.superresolution.core.graphics.GraphicsDevice;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VulkanQueueUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRPresentId.VK_KHR_PRESENT_ID_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.NVLowLatency2.VK_NV_LOW_LATENCY_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

public final class VulkanPresentationFeature {
    private static final VulkanSurface SURFACE = new VulkanSurface();
    private static VkRenderSystem renderSystem;
    private static boolean configLoaded;
    private static Boolean startupRequested;

    public static boolean isAvailable() {
        return isAvailable;
    }

    private static boolean isAvailable;

    private VulkanPresentationFeature() {
    }

    public static boolean isRequested() {
        #if MC_VER >= MC_1_21_11 && MC_VER < MC_26_2 || MC_VER >= MC_1_21 && MC_VER < MC_1_21_2 || MC_VER == MC_1_20_1
        ensureConfigLoaded();
        if (startupRequested == null) {
            startupRequested = SuperResolutionConfig.isEnableVulkanPresentation()
                    && !SuperResolutionConfig.isSkipInitVulkan();
        }
        return startupRequested;
        #else
        return false;
        #endif
    }

    private static synchronized void ensureConfigLoaded() {
        if (!configLoaded) {
            SuperResolutionConfig.SPEC.load();
            configLoaded = true;
        }
    }

    public static boolean shouldInitializeStreamline() {
        // Streamline is Windows-only, and it must not be initialized when a backend that
        // does not use it is selected (those drive Reflex through Vulkan instead). This
        // is read once at startup, so switching the provider needs a restart. The
        // automatic entry keeps Streamline on Windows.
        return isRequested()
                && SuperResolutionConfig.CURRENT_OS_TYPE == OperatingSystemType.WINDOWS
                && FrameGenerationDescriptions.mayUseStreamline(
                        SuperResolutionConfig.getFrameGenerationProvider());
    }

    public static synchronized void prepare(VkRenderSystem target) {
        if (!isRequested() || renderSystem != null) {
            return;
        }
        isAvailable = true;
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
        renderSystem = target;
    }

    public static void createSurface(VkRenderSystem target) {
        if (isRequested()) {
            SURFACE.createSurface(target.getVulkanInstance());
        }
    }

    public static int findGraphicsPresentQueueFamily(
            MemoryStack stack,
            int queueType,
            VkPhysicalDevice physicalDevice
    ) {
        if (!isRequested()) {
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
        if (!isRequested()) {
            return;
        }
        if (!target.getCapabilities().getDeviceExtensions().contains(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            throw new IllegalStateException("Selected Vulkan device does not support VK_KHR_swapchain");
        }
        long currentContext = GLFW.glfwGetCurrentContext();
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
        if (!isRequested()) {
            return;
        }
        if (target.device() == null || target.getVulkanInstance() == null || SURFACE.surface() == 0L) {
            throw new IllegalStateException("Vulkan presentation initialization did not produce a usable device");
        }
        VulkanPresentationWindow.initialize(VulkanPresentationContext.initialize(SURFACE), SURFACE);
    }

    public static boolean shutdownApplicationManagedProvider(
            String providerId,
            Runnable teardown
    ) {
        return VulkanPresentationWindow.shutdownApplicationManagedProvider(providerId, teardown);
    }

    public static synchronized void shutdown() {
        VulkanPresentationWindow.shutdown();
        if (renderSystem != null && SURFACE.surface() != 0L && renderSystem.getVulkanInstance() != null) {
            SURFACE.destroySurface(renderSystem.getVulkanInstance());
        }
        renderSystem = null;
    }

    public static synchronized void disableAfterFailure(Throwable failure) {
        SuperResolution.LOGGER.error("Vulkan presentation initialization failed; disabling it for the next launch", failure);
        try {
            SuperResolutionConfig.setEnableVulkanPresentation(false);
            SuperResolutionConfig.SPEC.save();
        } catch (Throwable saveFailure) {
            failure.addSuppressed(saveFailure);
        }
    }
}
