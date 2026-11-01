/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.mixin.core;

#if MC_VER == MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import com.mojang.renderpearl.backend.vulkan.init.VulkanPNextStruct;
import org.lwjgl.vulkan.VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceOpticalFlowFeaturesNV;
import org.lwjgl.vulkan.VkPhysicalDevicePrivateDataFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(VulkanFeatureSets.class)
public class VKOnly_SuperResolutionFeatures {
    @Unique
    private static final VulkanPNextStruct SR_MUTABLE_DESCRIPTOR_FEATURES =
            new VulkanPNextStruct(VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.class);
    @Unique
    private static final VulkanPNextStruct SR_SHADER_INTEGER_DOT_PRODUCT_FEATURES =
            new VulkanPNextStruct(VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR.class);
    @Unique
    private static final VulkanPNextStruct SR_PRIVATE_DATA_FEATURES =
            new VulkanPNextStruct(VkPhysicalDevicePrivateDataFeaturesEXT.class);
    @Unique
    private static final VulkanPNextStruct SR_OPTICAL_FLOW_FEATURES =
            new VulkanPNextStruct(VkPhysicalDeviceOpticalFlowFeaturesNV.class);

    @Unique
    private static final FeatureSet SR_REQUIRED = new FeatureSet(
            "SuperResolution required",
            Set.of(
                    "VK_KHR_external_memory",
                    "VK_KHR_external_semaphore",
                    "VK_KHR_dedicated_allocation",
                    "VK_KHR_get_memory_requirements2",
                    "VK_KHR_format_feature_flags2"
            ),
            Set.of()
    );
    @Unique
    private static final FeatureSet SR_OPTIONAL = new FeatureSet(
            "SuperResolution optional",
            Set.of(
                    "VK_EXT_mutable_descriptor_type",
                    "VK_EXT_descriptor_indexing",
                    "VK_EXT_private_data",
                    "VK_KHR_shader_float16_int8",
                    "VK_KHR_shader_integer_dot_product",
                    "VK_EXT_calibrated_timestamps",
                    "VK_NV_optical_flow",
                    "VK_NVX_binary_import",
                    "VK_NVX_image_view_handle",
                    "VK_EXT_shader_subgroup_partitioned"
            ),
            Set.of(
                    new VulkanFeature(VulkanFeatureSets.VK10_FEATURES_STRUCT, "shaderInt16"),
                    new VulkanFeature(VulkanFeatureSets.VK10_FEATURES_STRUCT, "shaderStorageImageWriteWithoutFormat"),
                    new VulkanFeature(SR_MUTABLE_DESCRIPTOR_FEATURES, "mutableDescriptorType"),
                    new VulkanFeature(SR_PRIVATE_DATA_FEATURES, "privateData"),
                    new VulkanFeature(SR_OPTICAL_FLOW_FEATURES, "opticalFlow"),
                    new VulkanFeature(VulkanFeatureSets.VK12_FEATURES_STRUCT, "shaderInt8"),
                    new VulkanFeature(VulkanFeatureSets.VK12_FEATURES_STRUCT, "shaderFloat16"),
                    new VulkanFeature(VulkanFeatureSets.VK12_FEATURES_STRUCT, "bufferDeviceAddress"),
                    new VulkanFeature(VulkanFeatureSets.VK12_FEATURES_STRUCT, "descriptorIndexing"),
                    new VulkanFeature(SR_SHADER_INTEGER_DOT_PRODUCT_FEATURES, "shaderIntegerDotProduct")
            )
    );
    @Unique
    private static final FeatureSet SR_WIN32_OPTIONAL = new FeatureSet(
            "SuperResolution Win32 external handles",
            Set.of(
                    "VK_KHR_external_memory_win32",
                    "VK_KHR_external_semaphore_win32"
            ),
            Set.of()
    );
    @Unique
    private static final FeatureSet SR_UNIX_OPTIONAL = new FeatureSet(
            "SuperResolution POSIX external handles",
            Set.of(
                    "VK_KHR_external_memory_fd",
                    "VK_KHR_external_semaphore_fd"
            ),
            Set.of()
    );

    @Shadow
    @Final
    @Mutable
    public static FeatureSet REQUIRED_FEATURESET;

    @Inject(method = "optionalFeatureSets", at = @At("RETURN"))
    private static void sr$addOptionalFeatures(CallbackInfoReturnable<Set<FeatureSet>> cir) {
        Set<FeatureSet> sets = cir.getReturnValue();
        sets.add(SR_OPTIONAL);
        sets.add(SR_WIN32_OPTIONAL);
        sets.add(SR_UNIX_OPTIONAL);
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void sr$requireFeatures(CallbackInfo ci) {
        REQUIRED_FEATURESET = new FeatureSet(
                "Minecraft and SuperResolution requirements",
                Set.of(REQUIRED_FEATURESET, SR_REQUIRED)
        );
    }
}
#else
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class VKOnly_SuperResolutionFeatures {
}
#endif
