/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.debug;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTMemoryBudget;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryBudgetPropertiesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties2;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;

/**
 * Reports a GPU hang on Minecraft's Vulkan queue in the log, on its own.
 *
 * <p>On NVIDIA a hang in Minecraft's queue (Xid 109, context switch timeout) does not always
 * reach the game as {@code VK_ERROR_DEVICE_LOST}: the driver can keep reporting the device as
 * healthy while Minecraft's frame never completes, so the game freezes in an infinite fence wait
 * and writes no crash report. A daemon thread polls Minecraft's submit timeline semaphore. When
 * a submitted frame has not completed for {@link #STALL_NANOS}, it logs Minecraft's GPU
 * checkpoints, the recent submissions and completions on Minecraft's, SR's frame generation and
 * SR's present queue, the VRAM budget and the stacks of the threads involved.
 *
 * <p>The report never touches SR's {@code VulkanCommandBuffer}s: their wait methods hold the
 * buffer's monitor, and during a hang a waiting thread holds it forever. Completions on SR's
 * queues come from the threads that already wait for them. Polling stops at
 * {@link #shutdown()}, before Minecraft destroys the semaphore. {@code -Dsr.vk.hangWatchdog=false}
 * turns it off.
 */
public final class GpuHangWatchdog {
    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("sr.vk.hangWatchdog"));
    private static final long STALL_NANOS = 3_000_000_000L;
    private static final long POLL_MILLIS = 250L;
    private static final int EVENTS = 96;
    private static final int STACK_DEPTH = 10;
    private static final Set<String> THREADS = Set.of("Render thread", "SR-FrameGeneration-FG",
            "SR-FrameGeneration-Present", "SR-Frame-GPU-End");

    public enum Queue {
        MINECRAFT("minecraft"),
        FRAME_GENERATION("frame gen"),
        PRESENT("present");

        private final String label;

        Queue(String label) {
            this.label = label;
        }
    }

    private static final Object LOCK = new Object();
    private static final long[] eventTime = new long[EVENTS];
    private static final Queue[] eventQueue = new Queue[EVENTS];
    private static final String[] eventWhat = new String[EVENTS];
    private static final long[] eventFrame = new long[EVENTS];
    private static final long[] eventValue = new long[EVENTS];
    private static long eventCount;

    private static volatile VkDevice minecraftDevice;
    private static volatile long submitSemaphore;
    private static volatile long submittedValue;
    private static boolean closed;
    private static Thread thread;

    private GpuHangWatchdog() {
    }

    /** Render thread, right after Minecraft submitted frame {@code frame}. */
    public static void onMinecraftSubmitted(long frame) {
        if (!ENABLED) {
            return;
        }
        long[] timeline;
        try {
            timeline = B3DVulkanBridge.minecraftSubmitTimeline();
            if (timeline == null) {
                return;
            }
            if (minecraftDevice == null) {
                minecraftDevice = B3DVulkanBridge.vkDevice();
            }
        } catch (Throwable ignored) {
            return;
        }
        submitSemaphore = timeline[0];
        submittedValue = timeline[1];
        record(Queue.MINECRAFT, "submit", frame, timeline[1]);
        ensureThread();
    }

    /**
     * Notes a submission or an observed completion for the report. {@code what} should be a
     * constant: this runs a few times a frame.
     */
    public static void record(Queue queue, String what, long frame, long value) {
        if (!ENABLED) {
            return;
        }
        long now = System.nanoTime();
        synchronized (eventTime) {
            int slot = (int) (eventCount++ % EVENTS);
            eventTime[slot] = now;
            eventQueue[slot] = queue;
            eventWhat[slot] = what;
            eventFrame[slot] = frame;
            eventValue[slot] = value;
        }
    }

    /** Minecraft's close(), before it destroys its Vulkan objects. */
    public static void shutdown() {
        synchronized (LOCK) {
            closed = true;
        }
    }

    private static synchronized void ensureThread() {
        if (thread != null) {
            return;
        }
        thread = new Thread(GpuHangWatchdog::watch, "SR-GPU-Hang-Watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void watch() {
        long lastValue = -1L;
        long pendingSince = 0L;
        boolean reported = false;
        boolean failureReported = false;
        try {
            while (true) {
                Thread.sleep(POLL_MILLIS);
                long submitted = submittedValue;
                long now = System.nanoTime();
                long value;
                int result;
                synchronized (LOCK) {
                    if (closed) {
                        return;
                    }
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        LongBuffer counter = stack.mallocLong(1);
                        result = VK12.vkGetSemaphoreCounterValue(minecraftDevice, submitSemaphore, counter);
                        value = counter.get(0);
                    }
                    if (result != VK_SUCCESS) {
                        if (!failureReported) {
                            failureReported = true;
                            report(pendingSince == 0L ? 0L : now - pendingSince, submitted, -1L,
                                    "vkGetSemaphoreCounterValue returned " + result + " (device lost)");
                        }
                        continue;
                    }
                    if (value >= submitted || value != lastValue || pendingSince == 0L) {
                        if (reported) {
                            SuperResolution.LOGGER.warn(
                                    "[SR GPU hang] Minecraft's queue completed submit {} after {} ms",
                                    value, (now - pendingSince) / 1_000_000L);
                            reported = false;
                        }
                        lastValue = value;
                        pendingSince = value >= submitted ? 0L : now;
                        continue;
                    }
                    if (!reported && now - pendingSince >= STALL_NANOS) {
                        reported = true;
                        report(now - pendingSince, submitted, value, "VK_SUCCESS (not marked lost)");
                    }
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.warn("[SR GPU hang] watchdog stopped", throwable);
        }
    }

    private static void report(long stallNanos, long submitted, long completed, String deviceState) {
        StringBuilder text = new StringBuilder(4096);
        text.append("[SR GPU hang] Minecraft's GPU queue has not completed submit ")
                .append(completed + 1L).append(" for ").append(stallNanos / 1_000_000L)
                .append(" ms. Last submitted ").append(submitted).append(", completed ").append(completed)
                .append(". Semaphore query: ").append(deviceState)
                .append(". The game cannot recover from this; close it.\n");
        text.append("Minecraft checkpoints (last marker each stage passed):\n");
        try {
            text.append(B3DVulkanBridge.gpuCheckpointReport());
        } catch (Throwable throwable) {
            text.append("  unavailable: ").append(throwable).append('\n');
        }
        appendEvents(text);
        appendMemoryBudget(text);
        appendThreads(text);
        SuperResolution.LOGGER.error(text.toString());
    }

    private static void appendEvents(StringBuilder text) {
        long now = System.nanoTime();
        text.append("Recent queue events (ms before this report; value = Minecraft timeline value):\n");
        synchronized (eventTime) {
            long first = Math.max(0L, eventCount - EVENTS);
            for (long index = first; index < eventCount; index++) {
                int slot = (int) (index % EVENTS);
                text.append(String.format("  -%9.2f  %-9s  %-20s frame %d",
                        (now - eventTime[slot]) / 1e6, eventQueue[slot].label, eventWhat[slot],
                        eventFrame[slot]));
                if (eventValue[slot] != 0L) {
                    text.append("  value ").append(eventValue[slot]);
                }
                text.append('\n');
            }
        }
    }

    private static void appendMemoryBudget(StringBuilder text) {
        try {
            VkPhysicalDevice physical = minecraftDevice.getPhysicalDevice();
            if (!supportsMemoryBudget(physical)) {
                text.append("VRAM budget: VK_EXT_memory_budget unsupported\n");
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDeviceMemoryBudgetPropertiesEXT budget =
                        VkPhysicalDeviceMemoryBudgetPropertiesEXT.calloc(stack).sType$Default();
                VkPhysicalDeviceMemoryProperties2 properties =
                        VkPhysicalDeviceMemoryProperties2.calloc(stack).sType$Default().pNext(budget);
                VK11.vkGetPhysicalDeviceMemoryProperties2(physical, properties);
                int heaps = properties.memoryProperties().memoryHeapCount();
                text.append("VRAM budget:\n");
                for (int heap = 0; heap < heaps; heap++) {
                    text.append(String.format("  heap %d (%s): %d MiB used of %d MiB budget, %d MiB size%n",
                            heap,
                            (properties.memoryProperties().memoryHeaps(heap).flags() & 1) != 0 ? "device" : "host",
                            budget.heapUsage(heap) >> 20, budget.heapBudget(heap) >> 20,
                            properties.memoryProperties().memoryHeaps(heap).size() >> 20));
                }
            }
        } catch (Throwable throwable) {
            text.append("VRAM budget: unavailable: ").append(throwable).append('\n');
        }
    }

    private static boolean supportsMemoryBudget(VkPhysicalDevice physical) {
        // Heap, not the thread's MemoryStack: ~250 extensions overflow its 64 KiB.
        VkExtensionProperties.Buffer extensions = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkEnumerateDeviceExtensionProperties(physical, (String) null, count, null);
            extensions = VkExtensionProperties.malloc(count.get(0));
            vkEnumerateDeviceExtensionProperties(physical, (String) null, count, extensions);
            for (VkExtensionProperties extension : extensions) {
                if (EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME.equals(extension.extensionNameString())) {
                    return true;
                }
            }
            return false;
        } finally {
            if (extensions != null) {
                extensions.free();
            }
        }
    }

    private static void appendThreads(StringBuilder text) {
        text.append("Threads:\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread target = entry.getKey();
            if (!THREADS.contains(target.getName())) {
                continue;
            }
            text.append("  \"").append(target.getName()).append("\" ").append(target.getState()).append('\n');
            int printed = 0;
            for (StackTraceElement frame : entry.getValue()) {
                String className = frame.getClassName();
                // Skip the foreign-call and LWJGL JNI plumbing above the interesting frames.
                if (className.startsWith("java.lang.invoke.") || className.startsWith("jdk.internal.foreign.")
                        || className.startsWith("org.lwjgl.system.JNI")) {
                    continue;
                }
                text.append("      at ").append(frame).append('\n');
                if (++printed == STACK_DEPTH) {
                    break;
                }
            }
        }
    }
}
