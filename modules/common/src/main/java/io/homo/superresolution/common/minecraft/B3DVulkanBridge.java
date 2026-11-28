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
#if MC_VER >= MC_26_3
import io.homo.superresolution.core.graphics.vulkan.B3DVulkanCommandBuffer;
#endif
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import java.util.function.Consumer;

import java.lang.reflect.Method;
#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import io.homo.superresolution.common.debug.VulkanHangBisect;
import io.homo.superresolution.common.mixin.core.accessor.VulkanCommandEncoderAccessor;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.vulkan.VulkanTimestampProfiler;

import static org.lwjgl.vulkan.VK13.VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT;
#endif

public final class B3DVulkanBridge {
    #if MC_VER >= MC_26_3
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

    public static void submit(Object encoder) {
        try {
            encoder.getClass().getMethod("submit").invoke(encoder);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to submit b3d Vulkan commands", failure);
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

    /**
     * Records a GPU checkpoint into Minecraft's command buffer that is being recorded. On device
     * loss Minecraft's crash report names the last checkpoint that reached the top and the bottom
     * of the pipe, so checkpoints around SR's steps show which step the GPU stopped in. Shown as
     * {@code BEGIN_RENDER_PASS}/{@code END_RENDER_PASS} with the label. Render thread only;
     * a no-op off Minecraft's Vulkan backend or with {@code -Dsr.vk.checkpoints=false}.
     */
    public static void checkpoint(boolean begin, String label) {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        if (!VulkanHangBisect.CHECKPOINTS || !isB3DVulkanBackend()) {
            return;
        }
        var encoder = (VulkanCommandEncoderAccessor) createCommandEncoder();
        encoder.checkpointStorage().recordCheckpoint(encoder.currentCommandBuffer(),
                begin ? CheckpointExtension.CheckpointType.BEGIN_RENDER_PASS
                        : CheckpointExtension.CheckpointType.END_RENDER_PASS,
                () -> label);
        #endif
    }

    /**
     * Opens a GPU timing region in Minecraft's command buffer that is being recorded. Returns
     * the slot for {@link #endGpuRegion}, or -1 when GPU timing is off. Render thread only, and
     * outside a render pass.
     */
    public static int beginGpuRegion(String name) {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        VulkanTimestampProfiler profiler = RenderSystems.vulkan().device().timestampProfiler();
        if (profiler == null) {
            return -1;
        }
        return profiler.beginRegionAfterPriorWork(
                ((VulkanCommandEncoderAccessor) createCommandEncoder()).currentCommandBuffer(), name);
        #else
        return -1;
        #endif
    }

    /**
     * Closes a region from {@link #beginGpuRegion}. The closing timestamp goes into the command
     * buffer recorded now, which is a newer one if Minecraft submitted in between.
     */
    public static void endGpuRegion(int slot) {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        if (slot < 0) {
            return;
        }
        VulkanTimestampProfiler profiler = RenderSystems.vulkan().device().timestampProfiler();
        if (profiler != null) {
            profiler.endRegion(((VulkanCommandEncoderAccessor) createCommandEncoder()).currentCommandBuffer(), slot);
        }
        #endif
    }

    #if MC_VER == MC_26_3 && IS_VULKAN == 1
    public static void memoryBarrier() {
        var encoder = (VulkanCommandEncoderAccessor) createCommandEncoder();
        try (var stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(encoder.currentCommandBuffer(), stack);
        }
    }
    #endif

    /** Queues a GPU wait before subsequently recorded commands. Render thread only. */
    public static void waitSemaphore(long semaphore, long value) {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        var encoder = (VulkanCommandEncoder) createCommandEncoder();
        encoder.waitSemaphore(semaphore, value, VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT);
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan semaphore waits require the Vulkan 26.3 configuration");
        #endif
    }

    /** Signals after the current recording and submits it. Render thread only. */
    public static void submitSignalingSemaphore(long semaphore, long value) {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        var encoder = (VulkanCommandEncoder) createCommandEncoder();
        encoder.signalSemaphore(semaphore, value, VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT);
        encoder.submit();
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan semaphore signals require the Vulkan 26.3 configuration");
        #endif
    }

    public static CommandCompletion captureCompletion() {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        var encoder = (VulkanCommandEncoder) createCommandEncoder();
        var timeline = (VulkanCommandEncoderAccessor) encoder;
        return new CommandCompletion(encoder::submit, vkDevice(),
                timeline.submitSemaphore(), timeline.currentSubmitIndex());
        #else
        throw new UnsupportedOperationException("Minecraft Vulkan capture requires the Vulkan 26.3 configuration");
        #endif
    }

    /**
     * Minecraft's submit timeline semaphore and the value its latest submission signals. Render
     * thread, right after Minecraft submitted. Null off the Vulkan 26.3 configuration.
     */
    public static long[] minecraftSubmitTimeline() {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        var timeline = (VulkanCommandEncoderAccessor) createCommandEncoder();
        return new long[]{timeline.submitSemaphore(), timeline.currentSubmitIndex() - 1L};
        #else
        return null;
        #endif
    }

    /**
     * Minecraft's GPU checkpoints on its graphics queue, formatted as in its device-lost crash
     * report: the last marker each pipeline stage passed. NVIDIA returns them on a hung device
     * that the driver has not marked lost yet, too.
     */
    public static String gpuCheckpointReport() {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        Object backend = backend();
        if (!(backend instanceof com.mojang.renderpearl.backend.vulkan.VulkanDevice device)) {
            return "(not Minecraft's Vulkan backend)";
        }
        return com.mojang.renderpearl.backend.vulkan.VulkanUtils.formatCheckpoints(
                device.checkpointExtension().retrieveCheckpoints(true));
        #else
        return "(unavailable)";
        #endif
    }

    /**
     * Completion of the Minecraft encoder submission that is being recorded, as the value that
     * submission signals on Minecraft's submit timeline semaphore. Only the render thread may
     * submit it. Once it is submitted, any thread can wait for it: the wait is a plain
     * {@code vkWaitSemaphores} and leaves Minecraft's own fence bookkeeping alone.
     */
    public static final class CommandCompletion {
        private final Runnable submit;
        private final VkDevice device;
        private final long semaphore;
        private final long value;
        private volatile boolean submitted;
        private volatile boolean completed;

        CommandCompletion(Runnable submit, VkDevice device, long semaphore, long value) {
            this.submit = submit;
            this.device = device;
            this.semaphore = semaphore;
            this.value = value;
        }

        public long semaphore() {
            return semaphore;
        }

        public long value() {
            return value;
        }

        public void markSubmitted() {
            submitted = true;
        }

        /** Render thread only: submits Minecraft's recording if it still holds this work. */
        public void ensureSubmitted() {
            if (!submitted) {
                submit.run();
                submitted = true;
            }
        }

        /** Any thread, after the render thread has submitted the work. */
        public void awaitCompletion() {
            if (completed) {
                return;
            }
            if (!submitted) {
                throw new IllegalStateException("Minecraft Vulkan capture has not been submitted");
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                        .sType(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                        .semaphoreCount(1)
                        .pSemaphores(stack.longs(semaphore))
                        .pValues(stack.longs(value));
                int result = VK12.vkWaitSemaphores(device, waitInfo, Long.MAX_VALUE);
                if (result != VK10.VK_SUCCESS) {
                    throw new IllegalStateException(
                            "Minecraft Vulkan capture submission did not complete, VkResult=" + result);
                }
            }
            completed = true;
        }

        /**
         * Any thread: waits up to {@code timeoutNanos} for the submitted work. Returns false on
         * timeout, on device loss (Minecraft reports that itself) or when the work was never
         * submitted, instead of throwing.
         */
        public boolean awaitCompletion(long timeoutNanos) {
            if (completed) {
                return true;
            }
            if (!submitted) {
                return false;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                        .sType(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                        .semaphoreCount(1)
                        .pSemaphores(stack.longs(semaphore))
                        .pValues(stack.longs(value));
                if (VK12.vkWaitSemaphores(device, waitInfo, timeoutNanos) != VK10.VK_SUCCESS) {
                    return false;
                }
            }
            completed = true;
            return true;
        }
    }
}
