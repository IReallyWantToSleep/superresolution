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

package io.homo.superresolution.common.presentation.vulkan;

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.backend.vulkan.VulkanPhysicalDevice;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import com.mojang.renderpearl.backend.vulkan.init.VulkanPNextStruct;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.presentation.PresentationBackendManager;
import io.homo.superresolution.core.graphics.vulkan.BorrowedDeviceSetup;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevicePresentIdFeaturesKHR;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.Set;

import static org.lwjgl.vulkan.KHRPresentId.VK_KHR_PRESENT_ID_EXTENSION_NAME;
import static org.lwjgl.vulkan.NVLowLatency2.VK_NV_LOW_LATENCY_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

/**
 * Adds what SR needs to the Vulkan device Minecraft creates. On Minecraft's Vulkan backend SR
 * runs on that device, and both of these can only be requested when it is created:
 * <ul>
 *     <li>native Reflex needs {@code VK_NV_low_latency2} and {@code VK_KHR_present_id}, which
 *     SR's own device also enables for presentation;</li>
 *     <li>an application-managed frame generation provider needs a queue of its own, plus one
 *     for presents, in the graphics queue family.</li>
 * </ul>
 * The result is recorded in {@link BorrowedDeviceSetup} for SR's wrapper of the device.
 */
public final class MinecraftVulkanDeviceSetup {
    private static final int SR_QUEUE_COUNT = 2;

    private MinecraftVulkanDeviceSetup() {
    }

    public static FeatureSet extendFeatures(VulkanPhysicalDevice physicalDevice, FeatureSet minecraftFeatures) {
        BorrowedDeviceSetup.setLowLatencyFeatures(false);
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return minecraftFeatures;
        }
        FeatureSet lowLatency = new FeatureSet(
                "Super Resolution native Reflex",
                Set.of(VK_KHR_PRESENT_ID_EXTENSION_NAME, VK_NV_LOW_LATENCY_2_EXTENSION_NAME),
                Set.of(new VulkanFeature(new VulkanPNextStruct(VkPhysicalDevicePresentIdFeaturesKHR.class), "presentId"))
        );
        try {
            if (!lowLatency.isSupported(physicalDevice.vkPhysicalDevice())) {
                SuperResolution.LOGGER.info("Native Reflex is unavailable: the device lacks {} or {}",
                        VK_KHR_PRESENT_ID_EXTENSION_NAME, VK_NV_LOW_LATENCY_2_EXTENSION_NAME);
                return minecraftFeatures;
            }
        } catch (BackendCreationException exception) {
            SuperResolution.LOGGER.warn("Could not query native Reflex support", exception);
            return minecraftFeatures;
        }
        BorrowedDeviceSetup.setLowLatencyFeatures(true);
        SuperResolution.LOGGER.info("Enabling {} and {} on Minecraft's Vulkan device for native Reflex",
                VK_KHR_PRESENT_ID_EXTENSION_NAME, VK_NV_LOW_LATENCY_2_EXTENSION_NAME);
        return minecraftFeatures.composite(lowLatency);
    }

    public static Int2IntMap extendQueues(VulkanPhysicalDevice physicalDevice, Int2IntMap minecraftQueues) {
        if (!PresentationBackendManager.isVulkanPresentationRequested()) {
            return minecraftQueues;
        }
        // Loading FrameGeneration registers the providers, Wisteria's included: their mods
        // subscribe in their client entry points, which run before Minecraft creates the device.
        FrameGeneration.StartupProviderSelection provider = FrameGeneration.startupProviderSelection();
        if (!provider.applicationManagedAsync()) {
            return minecraftQueues;
        }
        int family = physicalDevice.graphicsQueueFamilyAndIndex().firstInt();
        int minecraftCount = minecraftQueues.getOrDefault(family, 0);
        int available = queueCount(physicalDevice, family);
        int extra = Math.max(0, Math.min(SR_QUEUE_COUNT, available - minecraftCount));
        BorrowedDeviceSetup.reserveQueues(new BorrowedDeviceSetup.QueueReservation(
                provider.providerId(),
                family,
                available,
                extra >= 1 ? minecraftCount : -1,
                extra >= 2 ? minecraftCount + 1 : -1
        ));
        if (extra == 0) {
            SuperResolution.LOGGER.warn(
                    "Minecraft's graphics queue family {} has no free queue for frame generation provider '{}' ({} of {} used)",
                    family, provider.providerId(), minecraftCount, available);
            return minecraftQueues;
        }
        Int2IntMap queues = new Int2IntOpenHashMap(minecraftQueues);
        queues.put(family, minecraftCount + extra);
        SuperResolution.LOGGER.info(
                "Reserved {} queue(s) in Minecraft's graphics queue family {} for frame generation provider '{}'",
                extra, family, provider.providerId());
        return queues;
    }

    private static int queueCount(VulkanPhysicalDevice physicalDevice, int family) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.vkPhysicalDevice(), count, null);
            VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.vkPhysicalDevice(), count, families);
            return family >= 0 && family < families.capacity() ? families.get(family).queueCount() : 0;
        }
    }
}
#else
public final class MinecraftVulkanDeviceSetup {
    private MinecraftVulkanDeviceSetup() {
    }
}
#endif
