/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuFence;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.lang.reflect.Method;

public final class B3DVulkanBridge {
    private static final String B3D_VULKAN_DEVICE_CLASS = "com.mojang.blaze3d.vulkan.VulkanDevice";

    private static Object cachedBackend;
    private static VkInstance cachedInstance;
    private static VkDevice cachedDevice;
    private static VkPhysicalDevice cachedPhysicalDevice;
    private static Integer cachedGraphicsQueueFamily;
    private static Method instanceMethod;
    private static Method vkInstanceMethod;
    private static Method vkDeviceMethod;
    private static Method graphicsQueueMethod;
    private static Method queueFamilyIndexMethod;

    private B3DVulkanBridge() {
    }

    #if MC_VER >= MC_26_2
    public static boolean isB3DVulkanBackend() {
        Object backend = backend();
        return backend != null && B3D_VULKAN_DEVICE_CLASS.equals(backend.getClass().getName());
    }

    public static Object backend() {
        try {
            Object device = RenderSystem.getDevice();
            if (device == null || MinecraftRenderTargetUtil.cachedGpuDeviceBackendField == null) {
                return null;
            }
            Object backend = MinecraftRenderTargetUtil.cachedGpuDeviceBackendField.get(device);
            if (backend != cachedBackend) {
                clearCachedHandles();
                cachedBackend = backend;
            }
            return backend;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static VkInstance vkInstance() {
        Object backend = requireBackend();
        if (cachedInstance == null) {
            try {
                Object vulkanInstance = instanceMethod(backend).invoke(backend);
                cachedInstance = (VkInstance) vkInstanceMethod(vulkanInstance).invoke(vulkanInstance);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to read b3d VkInstance", t);
            }
        }
        return cachedInstance;
    }

    public static VkDevice vkDevice() {
        Object backend = requireBackend();
        if (cachedDevice == null) {
            try {
                cachedDevice = (VkDevice) vkDeviceMethod(backend).invoke(backend);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to read b3d VkDevice", t);
            }
        }
        return cachedDevice;
    }

    public static VkPhysicalDevice vkPhysicalDevice() {
        if (cachedPhysicalDevice == null) {
            cachedPhysicalDevice = vkDevice().getPhysicalDevice();
            if (cachedPhysicalDevice == null) {
                throw new IllegalStateException("b3d VkDevice did not expose VkPhysicalDevice");
            }
        }
        return cachedPhysicalDevice;
    }

    public static int graphicsQueueFamilyIndex() {
        Object backend = requireBackend();
        if (cachedGraphicsQueueFamily == null) {
            try {
                Object queue = graphicsQueueMethod(backend).invoke(backend);
                cachedGraphicsQueueFamily = (Integer) queueFamilyIndexMethod(queue).invoke(queue);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to read b3d graphics queue family", t);
            }
        }
        return cachedGraphicsQueueFamily;
    }

    public static Object createCommandEncoder() {
        Object backend = requireBackend();
        try {
            return backend.getClass().getMethod("createCommandEncoder").invoke(backend);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to read b3d VulkanCommandEncoder", t);
        }
    }

    public static void execute(Object encoder, org.lwjgl.vulkan.VkCommandBuffer commandBuffer) {
        try {
            encoder.getClass().getMethod("execute", org.lwjgl.vulkan.VkCommandBuffer.class).invoke(encoder, commandBuffer);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to execute SR command buffer on b3d encoder", t);
        }
    }

    public static GpuFence createFence(Object encoder) {
        try {
            return (GpuFence) encoder.getClass().getMethod("createFence").invoke(encoder);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to create b3d fence", t);
        }
    }

    public static boolean awaitFence(GpuFence fence, long timeoutNs) {
        return fence.awaitCompletion(timeoutNs);
    }

    private static Object requireBackend() {
        Object backend = backend();
        if (backend == null || !B3D_VULKAN_DEVICE_CLASS.equals(backend.getClass().getName())) {
            throw new IllegalStateException("Minecraft backend is not b3d Vulkan");
        }
        return backend;
    }

    private static Method instanceMethod(Object backend) throws NoSuchMethodException {
        if (instanceMethod == null) {
            instanceMethod = backend.getClass().getMethod("instance");
        }
        return instanceMethod;
    }

    private static Method vkDeviceMethod(Object backend) throws NoSuchMethodException {
        if (vkDeviceMethod == null) {
            vkDeviceMethod = backend.getClass().getMethod("vkDevice");
        }
        return vkDeviceMethod;
    }

    private static Method graphicsQueueMethod(Object backend) throws NoSuchMethodException {
        if (graphicsQueueMethod == null) {
            graphicsQueueMethod = backend.getClass().getMethod("graphicsQueue");
        }
        return graphicsQueueMethod;
    }

    private static Method vkInstanceMethod(Object vulkanInstance) throws NoSuchMethodException {
        if (vkInstanceMethod == null) {
            vkInstanceMethod = vulkanInstance.getClass().getMethod("vkInstance");
        }
        return vkInstanceMethod;
    }

    private static Method queueFamilyIndexMethod(Object queue) throws NoSuchMethodException {
        if (queueFamilyIndexMethod == null) {
            queueFamilyIndexMethod = queue.getClass().getMethod("queueFamilyIndex");
        }
        return queueFamilyIndexMethod;
    }

    private static void clearCachedHandles() {
        cachedInstance = null;
        cachedDevice = null;
        cachedPhysicalDevice = null;
        cachedGraphicsQueueFamily = null;
    }
    #else
    public static boolean isB3DVulkanBackend() {
        return false;
    }
    #endif
}
