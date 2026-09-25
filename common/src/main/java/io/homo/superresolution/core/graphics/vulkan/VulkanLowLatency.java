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

package io.homo.superresolution.core.graphics.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRTimelineSemaphore;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkLatencySleepInfoNV;
import org.lwjgl.vulkan.VkLatencySleepModeInfoNV;
import org.lwjgl.vulkan.VkOutOfBandQueueTypeInfoNV;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;
import org.lwjgl.vulkan.VkSetLatencyMarkerInfoNV;

import java.nio.LongBuffer;
import java.util.function.IntSupplier;

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
    private static long claimedFramePresentId;
    private static boolean fgQueueOutOfBandNotified;
    private static long[] legacyBatchPresentIds;
    private static int legacyBatchCursor;
    private static long legacyCurrentPresentId;
    private static boolean legacyCurrentPresentOutOfBand;

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
        }
    }

    public static void onSwapchainCreated(long handle, boolean latencyModeEnabled) {
        synchronized (LOCK) {
            swapchain = handle;
            swapchainLatencyMode = latencyModeEnabled;
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
            }
        }
    }

    /**
     * Advances the present id of the upcoming real frame; called once per game
     * frame before the simulation markers.
     */
    public static void nextFrame(long latencyFrameId) {
        synchronized (LOCK) {
            if (!supported || broken || !active) {
                return;
            }
            framePresentId += PRESENT_ID_STRIDE;
        }
    }

    /**
     * Returns the immutable native Reflex id for the current real/application frame.
     * The render thread captures this value into RealFrameJob before publication.
     */
    public static long currentFramePresentId() {
        synchronized (LOCK) {
            return enabledLocked() ? framePresentId : 0L;
        }
    }

    /**
     * Claims the current native Reflex present id for one rendered frame.
     *
     * <p>Minecraft may render more than once inside a single {@code runTick}
     * while loading a world. Those extra renders share the same simulation
     * marker id and therefore remain untracked instead of reusing a
     * {@code VkPresentIdKHR} value that was already issued.</p>
     */
    public static long claimCurrentFramePresentId() {
        synchronized (LOCK) {
            if (!enabledLocked()
                    || framePresentId == 0L
                    || claimedFramePresentId == framePresentId) {
                return 0L;
            }
            claimedFramePresentId = framePresentId;
            return framePresentId;
        }
    }

    /**
     * Serializes external synchronization of the presentation swapchain with
     * {@code VK_NV_low_latency2} calls that also reference that swapchain.
     */
    public static int synchronizedSwapchainOperation(IntSupplier operation) {
        if (operation == null) {
            throw new IllegalArgumentException("swapchain operation cannot be null");
        }
        synchronized (LOCK) {
            return operation.getAsInt();
        }
    }

    /**
     * Reserves monotonically increasing ids for generated presents while retaining
     * the render-thread real id for the final tracked present.
     */
    public static PresentBatchIds reservePresentBatch(long realPresentId, int generatedCount) {
        synchronized (LOCK) {
            if (generatedCount < 0 || generatedCount >= PRESENT_ID_STRIDE) {
                throw new IllegalArgumentException(
                        "generatedCount must be between 0 and " + (PRESENT_ID_STRIDE - 1)
                );
            }
            if (!enabledLocked() || realPresentId == 0L) {
                return new PresentBatchIds(new long[generatedCount], 0L);
            }
            long[] generatedIds = new long[generatedCount];
            long firstGeneratedId = realPresentId - generatedCount;
            for (int index = 0; index < generatedCount; index++) {
                generatedIds[index] = firstGeneratedId + index;
            }
            long minimumId = generatedCount == 0 ? realPresentId : generatedIds[0];
            if (minimumId <= 0L || minimumId <= lastIssuedPresentId) {
                throw new IllegalStateException(
                        "Reserved present ids must stay positive and must not collide with an already issued id: real="
                                + realPresentId + ", generatedCount=" + generatedCount
                                + ", minimum=" + minimumId
                                + ", lastIssued=" + lastIssuedPresentId
                );
            }
            lastIssuedPresentId = realPresentId;
            return new PresentBatchIds(generatedIds, realPresentId);
        }
    }

    public static void notifyFrameGenerationQueueOutOfBand(VulkanQueue queue) {
        synchronized (LOCK) {
            if (!enabledLocked() || fgQueueOutOfBandNotified || queue == null) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkOutOfBandQueueTypeInfoNV queueTypeInfo = VkOutOfBandQueueTypeInfoNV.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_OUT_OF_BAND_QUEUE_TYPE_INFO_NV)
                        .queueType(VK_OUT_OF_BAND_QUEUE_TYPE_RENDER_NV);
                synchronized (queue.submitLock()) {
                    vkQueueNotifyOutOfBandNV(queue.getQueue(), queueTypeInfo);
                }
                fgQueueOutOfBandNotified = true;
            }
        }
    }

    public static void renderSubmitMarker(long presentId, boolean outOfBand, boolean start) {
        synchronized (LOCK) {
            if (!enabledLocked() || presentId == 0L) {
                return;
            }
            int marker;
            if (outOfBand) {
                marker = start
                        ? VK_LATENCY_MARKER_OUT_OF_BAND_RENDERSUBMIT_START_NV
                        : VK_LATENCY_MARKER_OUT_OF_BAND_RENDERSUBMIT_END_NV;
            } else {
                marker = start
                        ? VK_LATENCY_MARKER_RENDERSUBMIT_START_NV
                        : VK_LATENCY_MARKER_RENDERSUBMIT_END_NV;
            }
            emitMarkerLocked(presentId, marker);
        }
    }

    public static void presentMarker(long presentId, boolean outOfBand, boolean start) {
        synchronized (LOCK) {
            if (!enabledLocked() || presentId == 0L) {
                return;
            }
            int marker;
            if (outOfBand) {
                marker = start
                        ? VK_LATENCY_MARKER_OUT_OF_BAND_PRESENT_START_NV
                        : VK_LATENCY_MARKER_OUT_OF_BAND_PRESENT_END_NV;
            } else {
                marker = start
                        ? VK_LATENCY_MARKER_PRESENT_START_NV
                        : VK_LATENCY_MARKER_PRESENT_END_NV;
            }
            emitMarkerLocked(presentId, marker);
        }
    }

    /** Legacy synchronous/interposer batch reservation. */
    public static void expectGeneratedBatch(int generatedCount) {
        synchronized (LOCK) {
            PresentBatchIds ids = reservePresentBatch(
                    claimCurrentFramePresentId(),
                    generatedCount
            );
            long[] generatedIds = ids.generatedPresentIds();
            legacyBatchPresentIds = new long[generatedIds.length + 1];
            System.arraycopy(generatedIds, 0, legacyBatchPresentIds, 0, generatedIds.length);
            legacyBatchPresentIds[generatedIds.length] = ids.realPresentId();
            legacyBatchCursor = 0;
        }
    }

    /** Legacy synchronous present marker context. */
    public static long beginPresent(boolean outOfBand) {
        synchronized (LOCK) {
            if (!enabledLocked()) {
                return 0L;
            }
            long presentId;
            if (legacyBatchPresentIds != null && legacyBatchCursor < legacyBatchPresentIds.length) {
                presentId = legacyBatchPresentIds[legacyBatchCursor++];
                if (legacyBatchCursor == legacyBatchPresentIds.length) {
                    legacyBatchPresentIds = null;
                    legacyBatchCursor = 0;
                }
            } else {
                presentId = reservePresentBatch(
                        claimCurrentFramePresentId(),
                        0
                ).realPresentId();
            }
            legacyCurrentPresentId = presentId;
            legacyCurrentPresentOutOfBand = outOfBand;
            return presentId;
        }
    }

    public static void presentPhaseMarker(boolean start) {
        presentMarker(legacyCurrentPresentId, legacyCurrentPresentOutOfBand, start);
    }

    public static void endPresent() {
        synchronized (LOCK) {
            legacyCurrentPresentId = 0L;
            legacyCurrentPresentOutOfBand = false;
        }
    }

    public record PresentBatchIds(long[] generatedPresentIds, long realPresentId) {
        public PresentBatchIds {
            generatedPresentIds = generatedPresentIds == null
                    ? new long[0]
                    : generatedPresentIds.clone();
        }

        @Override
        public long[] generatedPresentIds() {
            return generatedPresentIds.clone();
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
        claimedFramePresentId = 0L;
        legacyBatchPresentIds = null;
        legacyBatchCursor = 0;
        legacyCurrentPresentId = 0L;
        legacyCurrentPresentOutOfBand = false;
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
