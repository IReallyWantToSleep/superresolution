/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.presentation.v26_3;

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanPhysicalDevice;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import io.homo.superresolution.common.presentation.vulkan.MinecraftVulkanDeviceSetup;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Adds SR's Reflex extensions and frame generation queues to Minecraft's Vulkan device. */
@Mixin(VulkanBackend.class)
public abstract class MinecraftVulkanDeviceSetupMixin {
    @ModifyArg(
            method = "createDevice(Lcom/mojang/renderpearl/api/device/GpuDebugOptions;)Lcom/mojang/renderpearl/api/device/GpuDevice;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanBackend;createDevice(Lcom/mojang/renderpearl/backend/vulkan/init/FeatureSet;Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;)Lorg/lwjgl/vulkan/VkDevice;"
            ),
            index = 0
    )
    private FeatureSet super_resolution$addFeatures(FeatureSet featureSet, VulkanPhysicalDevice physicalDevice) {
        return MinecraftVulkanDeviceSetup.extendFeatures(physicalDevice, featureSet);
    }

    @Redirect(
            method = "createDevice(Lcom/mojang/renderpearl/backend/vulkan/init/FeatureSet;Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;)Lorg/lwjgl/vulkan/VkDevice;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;queueFamilyCreateInfoMap()Lit/unimi/dsi/fastutil/ints/Int2IntMap;"
            )
    )
    private static Int2IntMap super_resolution$addQueues(VulkanPhysicalDevice physicalDevice) {
        return MinecraftVulkanDeviceSetup.extendQueues(physicalDevice, physicalDevice.queueFamilyCreateInfoMap());
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class MinecraftVulkanDeviceSetupMixin {
}
#endif
