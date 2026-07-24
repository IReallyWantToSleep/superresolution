/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRTimelineSemaphore;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkLatencySleepInfoNV;
import org.lwjgl.vulkan.VkLatencySleepModeInfoNV;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;
import org.lwjgl.vulkan.VkSetLatencyMarkerInfoNV;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.NVLowLatency2.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Drives NVIDIA Reflex through VK_NV_low_latency2 for platforms where the
 * Streamline sl.reflex plugin is unavailable (Linux). The presentation swapchain
 * registers itself here; the low-latency provider then applies the sleep mode,
 * sleeps the render thread via a timeline semaphore, and stamps latency markers.
 *
 * <p>Present ids advance by a fixed stride per rendered frame so DLSS-FG frames
 * presented ahead of the real frame can take the ids just below it while staying
 * monotonic, as VK_KHR_present_id requires. Interpolated presents are reported
 * with the out-of-band markers so the driver excludes them from latency pacing.
 *
 * <p>Lock order: {@code LOCK} first, then the queue submit lock. The semaphore
 * wait in {@link #sleep()} happens outside both so markers and paced presents
 * never stall behind a sleeping render thread.
 */
public final class VulkanLowLatency {
    private static final int PRESENT_ID_STRIDE = 16;
    private static final long SLEEP_TIMEOUT_NANOS = 250_000_000L;

    private static final Object LOCK = new Object();

    private static VulkanDevice device;
    private static boolean supported;
    private static boolean active;
    private static boolean broken;

    private static long swapchain;
    private static boolean swapchainLatencyMode;

    private static long sleepSemaphore;
    private static long sleepValue;

    private static long framePresentId;
    private static long lastIssuedPresentId;
    private static long[] batchPresentIds;
    private static int batchCursor;
    private static long currentPresentId;
    private static boolean currentPresentOutOfBand;

    private static boolean sleepModeRequested;
    private static boolean requestedLowLatency;
    private static boolean requestedBoost;
    private static int requestedIntervalUs;
    private static long appliedSwapchain;
    private static boolean appliedLowLatency;
    private static boolean appliedBoost;
    private static int appliedIntervalUs;

    private VulkanLowLatency() {
    }

    /**
     * Records whether the freshly created device can drive VK_NV_low_latency2
     * (extension + presentId + timelineSemaphore enabled, Streamline not owning
     * Reflex through its interposer).
     */
    public static void onDeviceCreated(VulkanDevice createdDevice, boolean lowLatencySupported) {
        synchronized (LOCK) {
            resetLocked();
            device = createdDevice;
            supported = lowLatencySupported;
            if (lowLatencySupported) {
                VkRenderSystem.LOGGER.info("VK_NV_low_latency2 is available; native Reflex can be used");
            }
        }
    }

    public static void onDeviceDestroyed() {
        synchronized (LOCK) {
            if (device != null && sleepSemaphore != VK_NULL_HANDLE) {
                vkDestroySemaphore(device.getVkDevice(), sleepSemaphore, null);
            }
            resetLocked();
        }
    }

    public static boolean isSupported() {
        synchronized (LOCK) {
            return supported && !broken && device != null;
        }
    }

    /**
     * Enabled by the Vulkan Reflex provider while it owns low-latency handling;
     * everything (markers, present ids, sleeps) is dormant otherwise.
     */
    public static void setActive(boolean value) {
        synchronized (LOCK) {
            active = value;
            if (!value) {
                batchPresentIds = null;
                batchCursor = 0;
                currentPresentId = 0L;
                currentPresentOutOfBand = false;
            }
        }
    }

    public static void onSwapchainCreated(long handle, boolean latencyModeEnabled) {
        synchronized (LOCK) {
            swapchain = handle;
            swapchainLatencyMode = latencyModeEnabled;
            batchPresentIds = null;
            batchCursor = 0;
            if (sleepModeRequested) {
                ensureSleepModeAppliedLocked();
            }
        }
    }

    public static void onSwapchainDestroyed(long handle) {
        synchronized (LOCK) {
            if (swapchain == handle) {
                swapchain = VK_NULL_HANDLE;
                swapchainLatencyMode = false;
                batchPresentIds = null;
                batchCursor = 0;
            }
        }
    }

    /**
     * Advances the present id of the upcoming real frame; called once per game
     * frame before the simulation markers.
     */
    public static void nextFrame() {
        synchronized (LOCK) {
            if (!supported || broken || !active) {
                return;
            }
            framePresentId += PRESENT_ID_STRIDE;
        }
    }

    /**
     * Pre-allocates the present ids for one frame-generation batch (interpolated
     * frames first, real frame last). Allocation happens at submit time so a new
     * frame starting on the render thread cannot shift ids under the pacer.
     */
    public static void expectGeneratedBatch(int generatedCount) {
        synchronized (LOCK) {
            if (!enabledLocked() || framePresentId == 0L || generatedCount <= 0) {
                batchPresentIds = null;
                batchCursor = 0;
                return;
            }
            int count = Math.min(generatedCount, PRESENT_ID_STRIDE - 1);
            long[] ids = new long[count + 1];
            for (int i = 0; i < count; i++) {
                ids[i] = framePresentId - count + i;
            }
            ids[count] = framePresentId;
            batchPresentIds = ids;
            batchCursor = 0;
        }
    }

    /**
     * Assigns the id for the present that is about to be queued and returns it
     * (0 when low-latency tracking is off, meaning: do not chain VkPresentIdKHR).
     */
    public static long beginPresent(boolean outOfBand) {
        synchronized (LOCK) {
            if (!enabledLocked()) {
                return 0L;
            }
            long desired;
            if (batchPresentIds != null && batchCursor < batchPresentIds.length) {
                desired = batchPresentIds[batchCursor++];
                if (batchCursor == batchPresentIds.length) {
                    batchPresentIds = null;
                    batchCursor = 0;
                }
            } else {
                desired = framePresentId;
            }
            if (desired == 0L) {
                return 0L;
            }
            long issued = Math.max(desired, lastIssuedPresentId + 1);
            lastIssuedPresentId = issued;
            currentPresentId = issued;
            currentPresentOutOfBand = outOfBand;
            return issued;
        }
    }

    public static void endPresent() {
        synchronized (LOCK) {
            currentPresentId = 0L;
            currentPresentOutOfBand = false;
        }
    }

    /**
     * Emits the present start/end marker for the present prepared by
     * {@link #beginPresent(boolean)}, out-of-band for interpolated frames.
     */
    public static void presentPhaseMarker(boolean start) {
        synchronized (LOCK) {
            if (!enabledLocked() || currentPresentId == 0L) {
                return;
            }
            int marker;
            if (currentPresentOutOfBand) {
                marker = start
                        ? VK_LATENCY_MARKER_OUT_OF_BAND_PRESENT_START_NV
                        : VK_LATENCY_MARKER_OUT_OF_BAND_PRESENT_END_NV;
            } else {
                marker = start ? VK_LATENCY_MARKER_PRESENT_START_NV : VK_LATENCY_MARKER_PRESENT_END_NV;
            }
            emitMarkerLocked(currentPresentId, marker);
        }
    }

    /**
     * Emits a simulation/render-submit/flash marker against the current frame.
     */
    public static void frameMarker(int marker) {
        synchronized (LOCK) {
            if (!enabledLocked() || framePresentId == 0L) {
                return;
            }
            emitMarkerLocked(framePresentId, marker);
        }
    }

    /**
     * Applies the Reflex sleep mode; retried automatically when the swapchain is
     * recreated. Returns whether the mode is applied to the live swapchain.
     */
    public static boolean setSleepMode(boolean lowLatencyMode, boolean boost, int minimumIntervalUs) {
        synchronized (LOCK) {
            sleepModeRequested = true;
            requestedLowLatency = lowLatencyMode;
            requestedBoost = boost;
            requestedIntervalUs = Math.max(0, minimumIntervalUs);
            return ensureSleepModeAppliedLocked();
        }
    }

    /**
     * Schedules the driver-paced wakeup and blocks on it; called once per frame
     * from the render thread before input sampling.
     */
    public static void sleep() {
        long semaphore;
        long value;
        VkDevice vkDevice;
        synchronized (LOCK) {
            if (!enabledLocked() || !sleepModeRequested) {
                return;
            }
            if (!requestedLowLatency && requestedIntervalUs <= 0) {
                return;
            }
            if (!ensureSleepModeAppliedLocked() || !ensureSleepSemaphoreLocked()) {
                return;
            }
            value = ++sleepValue;
            semaphore = sleepSemaphore;
            vkDevice = device.getVkDevice();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkLatencySleepInfoNV sleepInfo = VkLatencySleepInfoNV.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_LATENCY_SLEEP_INFO_NV)
                        .signalSemaphore(semaphore)
                        .value(value);
                int result;
                synchronized (device.getMainQueue().submitLock()) {
                    result = vkLatencySleepNV(vkDevice, swapchain, sleepInfo);
                }
                if (result != VK_SUCCESS) {
                    fail("vkLatencySleepNV", result);
                    return;
                }
            }
        }

        // Wait outside the locks so markers and paced presents keep flowing.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .semaphoreCount(1)
                    .pSemaphores(stack.longs(semaphore))
                    .pValues(stack.longs(value));
            if (vkDevice.getCapabilities().vkWaitSemaphores != VK_NULL_HANDLE) {
                VK12.vkWaitSemaphores(vkDevice, waitInfo, SLEEP_TIMEOUT_NANOS);
            } else {
                KHRTimelineSemaphore.vkWaitSemaphoresKHR(vkDevice, waitInfo, SLEEP_TIMEOUT_NANOS);
            }
        }
    }

    private static boolean enabledLocked() {
        return supported
                && !broken
                && active
                && device != null
                && swapchain != VK_NULL_HANDLE
                && swapchainLatencyMode;
    }

    private static boolean ensureSleepModeAppliedLocked() {
        if (!enabledLocked() || !sleepModeRequested) {
            return false;
        }
        if (appliedSwapchain == swapchain
                && appliedLowLatency == requestedLowLatency
                && appliedBoost == requestedBoost
                && appliedIntervalUs == requestedIntervalUs) {
            return true;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkLatencySleepModeInfoNV sleepModeInfo = VkLatencySleepModeInfoNV.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_LATENCY_SLEEP_MODE_INFO_NV)
                    .lowLatencyMode(requestedLowLatency)
                    .lowLatencyBoost(requestedBoost)
                    .minimumIntervalUs(requestedIntervalUs);
            int result;
            synchronized (device.getMainQueue().submitLock()) {
                result = vkSetLatencySleepModeNV(device.getVkDevice(), swapchain, sleepModeInfo);
            }
            if (result != VK_SUCCESS) {
                fail("vkSetLatencySleepModeNV", result);
                return false;
            }
        }
        appliedSwapchain = swapchain;
        appliedLowLatency = requestedLowLatency;
        appliedBoost = requestedBoost;
        appliedIntervalUs = requestedIntervalUs;
        return true;
    }

    private static boolean ensureSleepSemaphoreLocked() {
        if (sleepSemaphore != VK_NULL_HANDLE) {
            return true;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo typeCreateInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK12.VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK12.VK_SEMAPHORE_TYPE_TIMELINE)
                    .initialValue(0L);
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(typeCreateInfo.address());
            LongBuffer pointer = stack.mallocLong(1);
            int result = vkCreateSemaphore(device.getVkDevice(), createInfo, null, pointer);
            if (result != VK_SUCCESS) {
                fail("vkCreateSemaphore (Reflex timeline semaphore)", result);
                return false;
            }
            sleepSemaphore = pointer.get(0);
            sleepValue = 0L;
            return true;
        }
    }

    private static void emitMarkerLocked(long presentId, int marker) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSetLatencyMarkerInfoNV markerInfo = VkSetLatencyMarkerInfoNV.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SET_LATENCY_MARKER_INFO_NV)
                    .presentID(presentId)
                    .marker(marker);
            synchronized (device.getMainQueue().submitLock()) {
                vkSetLatencyMarkerNV(device.getVkDevice(), swapchain, markerInfo);
            }
        }
    }

    private static void fail(String operation, int result) {
        broken = true;
        VkRenderSystem.LOGGER.warn(
                "{} failed with VkResult={}; disabling native Reflex for this session",
                operation,
                result
        );
    }

    private static void resetLocked() {
        device = null;
        supported = false;
        active = false;
        broken = false;
        swapchain = VK_NULL_HANDLE;
        swapchainLatencyMode = false;
        sleepSemaphore = VK_NULL_HANDLE;
        sleepValue = 0L;
        framePresentId = 0L;
        lastIssuedPresentId = 0L;
        batchPresentIds = null;
        batchCursor = 0;
        currentPresentId = 0L;
        currentPresentOutOfBand = false;
        sleepModeRequested = false;
        requestedLowLatency = false;
        requestedBoost = false;
        requestedIntervalUs = 0;
        appliedSwapchain = VK_NULL_HANDLE;
        appliedLowLatency = false;
        appliedBoost = false;
        appliedIntervalUs = 0;
    }
}
