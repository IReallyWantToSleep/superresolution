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

package io.homo.superresolution.common.minecraft;

#if MC_VER >= MC_26_2
import com.mojang.blaze3d.systems.RenderSystem;
#if MC_VER >= MC_26_3
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
#else
import com.mojang.blaze3d.buffers.GpuFence;
#endif
#endif
import io.homo.superresolution.core.graphics.vulkan.B3DVulkanCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

import java.lang.reflect.Method;

public final class B3DVulkanBridge {
    #if MC_VER >= MC_26_3 && IS_VULKAN == 1
    private static final String B3D_VULKAN_DEVICE_CLASS = "com.mojang.renderpearl.backend.vulkan.VulkanDevice";
    #else
    private static final String B3D_VULKAN_DEVICE_CLASS = "com.mojang.blaze3d.vulkan.VulkanDevice";
    #endif

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

    public static VkQueue graphicsQueue() {
        Object backend = requireBackend();
        try {
            Object queue = graphicsQueueMethod(backend).invoke(backend);
            return (VkQueue) queue.getClass().getMethod("vkQueue").invoke(queue);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to read Minecraft Vulkan graphics queue", t);
        }
    }

    public static int graphicsQueueIndex() {
        // Minecraft's graphics queue is always queue zero in RenderPearl's
        // Vulkan device creation path; keep this explicit for borrowed systems.
        return 0;
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
        instanceMethod = null;
        vkInstanceMethod = null;
        vkDeviceMethod = null;
        graphicsQueueMethod = null;
        queueFamilyIndexMethod = null;
    }
    #else
    public static boolean isB3DVulkanBackend() {
        return false;
    }
    #endif

    public static void awaitIdle() {
        #if MC_VER >= MC_26_3 && IS_VULKAN == 1
        if (!isB3DVulkanBackend()) {
            return;
        }
        var encoder = (com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder) createCommandEncoder();
        try (GpuFence fence = encoder.createFence()) {
            encoder.submit();
            if (!fence.awaitCompletion(Long.MAX_VALUE)) {
                throw new IllegalStateException("Minecraft Vulkan queue did not become idle");
            }
        }
        #endif
    }

    public static void recordCommands(Consumer<ICommandBuffer> recording) {
        #if MC_VER >= MC_26_3 && IS_VULKAN == 1
        recording.accept(new B3DVulkanCommandBuffer(RenderSystem.getDevice().createCommandEncoder()));
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan recording requires the Vulkan 26.3 configuration");
        #endif
    }

    public static CommandCompletion captureCompletion() {
        #if MC_VER >= MC_26_3 && IS_VULKAN == 1
        var encoder = (com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder) createCommandEncoder();
        GpuFence fence = encoder.createFence();
        return new CommandCompletion(encoder::submit,
                () -> fence.awaitCompletion(Long.MAX_VALUE), fence::close);
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan capture requires the Vulkan 26.3 configuration");
        #endif
    }

    /**
     * A fence for the current Minecraft encoder submission. An early flush must
     * submit its recording before waiting; normal frame submission marks it first.
     */
    public static final class CommandCompletion {
        private final Runnable submit;
        private final BooleanSupplier await;
        private final Runnable close;
        private boolean submitted;
        private boolean completed;

        CommandCompletion(Runnable submit, BooleanSupplier await, Runnable close) {
            this.submit = submit;
            this.await = await;
            this.close = close;
        }

        public void markSubmitted() {
            submitted = true;
        }

        public void awaitCompletion() {
            if (completed) {
                return;
            }
            if (!submitted) {
                submit.run();
                submitted = true;
            }
            if (!await.getAsBoolean()) {
                throw new IllegalStateException("Minecraft Vulkan capture submission did not complete");
            }
            completed = true;
            close.run();
        }
    }
}
