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

import io.homo.superresolution.common.perf.PerformanceTracker;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK10.*;

/**
 * GPU timing for the Vulkan half of the frame. The OpenGL side already has
 * {@link PerformanceTracker}'s {@code glQueryCounter} path, but nothing measured the
 * upscale, frame-generation or present work, which made it impossible to tell whether a
 * frame was bound by GL rendering, the Vulkan upscale or presentation.
 * <p>
 * Each region owns two timestamps in a single pool. The reset is recorded into the same
 * command buffer immediately before the opening write, which keeps the whole lifecycle
 * inside one submission and avoids depending on the optional {@code hostQueryReset}
 * feature. Results are polled without {@code VK_QUERY_RESULT_WAIT_BIT} so collection
 * never blocks the caller; a region that is not ready yet is simply retried next frame.
 */
public final class VulkanTimestampProfiler implements AutoCloseable {
    /** Regions tracked concurrently. Each costs two queries; a frame uses a handful. */
    private static final int MAX_REGIONS = 64;
    /** Frames a region may stay unresolved before its slot is assumed abandoned. */
    private static final int ABANDONED_POLL_LIMIT = 240;

    private final VulkanDevice device;
    private final long queryPool;
    private final double timestampPeriodNanos;
    private final String[] slotNames = new String[MAX_REGIONS];
    private final boolean[] slotPending = new boolean[MAX_REGIONS];
    private final boolean[] slotClosed = new boolean[MAX_REGIONS];
    private final int[] slotPolls = new int[MAX_REGIONS];
    private int cursor;
    private boolean destroyed;

    private VulkanTimestampProfiler(VulkanDevice device, long queryPool, double timestampPeriodNanos) {
        this.device = device;
        this.queryPool = queryPool;
        this.timestampPeriodNanos = timestampPeriodNanos;
    }

    /**
     * @return a profiler, or {@code null} when the queue cannot write timestamps at all
     * (some drivers report {@code timestampValidBits == 0}) or the pool cannot be made.
     */
    public static VulkanTimestampProfiler createIfSupported(VulkanDevice device, VulkanQueue queue) {
        if (device == null || queue == null) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(device.getPhysicalDevice(), properties);
            float timestampPeriod = properties.limits().timestampPeriod();
            if (timestampPeriod <= 0.0f) {
                return null;
            }
            if (!queueWritesTimestamps(stack, device, queue.getQueueFamilyIndex())) {
                return null;
            }

            VkQueryPoolCreateInfo createInfo = VkQueryPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO)
                    .queryType(VK_QUERY_TYPE_TIMESTAMP)
                    .queryCount(MAX_REGIONS * 2);
            LongBuffer pointer = stack.mallocLong(1);
            VK_CHECK(
                    vkCreateQueryPool(device.getVkDevice(), createInfo, null, pointer),
                    "Failed to create the Vulkan timestamp query pool"
            );
            long pool = pointer.get(0);
            device.setDebugName(VK_OBJECT_TYPE_QUERY_POOL, pool, "SR GPU Timestamps");
            return new VulkanTimestampProfiler(device, pool, timestampPeriod);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static boolean queueWritesTimestamps(MemoryStack stack, VulkanDevice device, int queueFamilyIndex) {
        int[] count = new int[]{0};
        vkGetPhysicalDeviceQueueFamilyProperties(device.getPhysicalDevice(), count, null);
        if (count[0] <= queueFamilyIndex) {
            return false;
        }
        VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.calloc(count[0], stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device.getPhysicalDevice(), count, families);
        return families.get(queueFamilyIndex).timestampValidBits() != 0;
    }

    /**
     * Opens a region in {@code commandBuffer}. Must be called outside a render pass, and
     * the returned slot has to be handed to {@link #endRegion} on the same command buffer.
     *
     * @return the slot id, or {@code -1} when nothing was recorded (caller should skip
     * the matching {@code endRegion}).
     */
    public int beginRegion(VkCommandBuffer commandBuffer, String name) {
        if (destroyed || commandBuffer == null || name == null) {
            return -1;
        }
        int slot = findFreeSlot();
        if (slot < 0) {
            return -1;
        }
        int base = slot * 2;
        // Reset and both writes live in one submission, so the pool never needs a host
        // reset and a slot can be recycled as soon as its results have been read.
        vkCmdResetQueryPool(commandBuffer, queryPool, base, 2);
        vkCmdWriteTimestamp(commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, queryPool, base);
        slotNames[slot] = name;
        slotPending[slot] = true;
        slotClosed[slot] = false;
        slotPolls[slot] = 0;
        return slot;
    }

    public void endRegion(VkCommandBuffer commandBuffer, int slot) {
        if (destroyed || commandBuffer == null || slot < 0 || slot >= MAX_REGIONS || !slotPending[slot]) {
            return;
        }
        vkCmdWriteTimestamp(
                commandBuffer,
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                queryPool,
                slot * 2 + 1
        );
        slotClosed[slot] = true;
    }

    /**
     * Publishes every region whose timestamps have landed. Call once per frame; regions
     * still in flight stay pending and are picked up on a later call.
     */
    public void collect() {
        if (destroyed) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer results = stack.mallocLong(2);
            for (int slot = 0; slot < MAX_REGIONS; slot++) {
                if (!slotPending[slot]) {
                    continue;
                }
                if (!slotClosed[slot]) {
                    // beginRegion ran but endRegion never did, so the region threw
                    // between the two writes. Nothing will ever resolve it.
                    if (++slotPolls[slot] > ABANDONED_POLL_LIMIT) {
                        releaseSlot(slot);
                    }
                    continue;
                }
                int status = vkGetQueryPoolResults(
                        device.getVkDevice(),
                        queryPool,
                        slot * 2,
                        2,
                        results,
                        Long.BYTES,
                        VK_QUERY_RESULT_64_BIT
                );
                if (status == VK_NOT_READY) {
                    // A command buffer that was recorded and then reset instead of
                    // submitted leaves its queries unwritten, and they would stay
                    // NOT_READY forever. Reclaim the slot rather than slowly starving
                    // the pool until profiling silently stops.
                    if (++slotPolls[slot] > ABANDONED_POLL_LIMIT) {
                        releaseSlot(slot);
                    }
                    continue;
                }
                if (status != VK_SUCCESS) {
                    // Drop the sample rather than leaking the slot forever.
                    releaseSlot(slot);
                    continue;
                }
                long start = results.get(0);
                long end = results.get(1);
                if (end > start) {
                    PerformanceTracker.submitExternalGpuSample(
                            slotNames[slot],
                            (long) ((end - start) * timestampPeriodNanos)
                    );
                }
                releaseSlot(slot);
            }
        }
    }

    @Override
    public void close() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        vkDestroyQueryPool(device.getVkDevice(), queryPool, null);
    }

    private int findFreeSlot() {
        for (int i = 0; i < MAX_REGIONS; i++) {
            int slot = (cursor + i) % MAX_REGIONS;
            if (!slotPending[slot]) {
                cursor = (slot + 1) % MAX_REGIONS;
                return slot;
            }
        }
        // Every slot is awaiting results; skip this region instead of stalling.
        return -1;
    }

    private void releaseSlot(int slot) {
        slotPending[slot] = false;
        slotClosed[slot] = false;
        slotPolls[slot] = 0;
        slotNames[slot] = null;
    }
}
