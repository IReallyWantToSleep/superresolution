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

import org.jetbrains.annotations.Nullable;

/**
 * What SR added to a Vulkan device that Minecraft created: queues for the frame generation and
 * present work SR submits from its own threads, and the extensions native Reflex needs. Queues
 * and extensions can only be requested when the device is created, which Minecraft does before
 * SR wraps the device ({@link VulkanDevice#borrowed}). So the additions are recorded here at
 * creation and read back when SR wraps it.
 */
public final class BorrowedDeviceSetup {
    private static volatile @Nullable QueueReservation queues;
    private static volatile boolean lowLatencyFeatures;

    private BorrowedDeviceSetup() {
    }

    public static void reserveQueues(QueueReservation value) {
        queues = value;
    }

    /** The queues reserved on a device whose graphics queues come from this family, if any. */
    public static @Nullable QueueReservation queuesFor(int queueFamilyIndex) {
        QueueReservation value = queues;
        return value != null && value.queueFamilyIndex() == queueFamilyIndex ? value : null;
    }

    /** Records whether VK_KHR_present_id (with presentId) and VK_NV_low_latency2 were enabled. */
    public static void setLowLatencyFeatures(boolean enabled) {
        lowLatencyFeatures = enabled;
    }

    public static boolean lowLatencyFeatures() {
        return lowLatencyFeatures;
    }

    /**
     * @param providerId                the application-managed provider selected at startup
     * @param queueFamilyIndex          Minecraft's graphics queue family
     * @param availableQueueCount       queues that family exposes
     * @param frameGenerationQueueIndex SR's frame generation queue, or -1 if the family had no room
     * @param presentQueueIndex         SR's dedicated present queue, or -1
     */
    public record QueueReservation(
            String providerId,
            int queueFamilyIndex,
            int availableQueueCount,
            int frameGenerationQueueIndex,
            int presentQueueIndex
    ) {
        public boolean hasFrameGenerationQueue() {
            return frameGenerationQueueIndex >= 0;
        }

        public boolean hasPresentQueue() {
            return presentQueueIndex >= 0;
        }
    }
}
