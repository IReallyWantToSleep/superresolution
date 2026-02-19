/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core;

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.opengl.GlRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_EXT_MUTABLE_DESCRIPTOR_TYPE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderFloat16Int8.VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderIntegerDotProduct.VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPushDescriptor.VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME;

public class RenderSystems {
    private static VkRenderSystem vulkan;
    private static GlRenderSystem opengl;

    public static void init() {
        opengl = new GlRenderSystem();
        opengl.initRenderSystem();
        initVulkan();
    }


    public static void destroy() {
        if (opengl != null) {
            opengl.destroyRenderSystem();
        }
        if (vulkan != null) {
            vulkan.destroyRenderSystem();
        }
    }

    public static boolean isSupportVulkan() {
        return vulkan != null;
    }

    public static boolean isSupportOpenGL() {
        return true;
    }

    private static void initVulkan() {
        if (SuperResolutionConfig.isSkipInitVulkan()) {
            return;
        }
        try {
            VK.create();
        } catch (Exception | Error e) {
            if (!e.getMessage().contains("Vulkan has already been created")) {
                VkRenderSystem.LOGGER.error("Vulkan初始化失败，似乎缺少Vulkan运行库，错误 {}", e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        vulkan = new VkRenderSystem();
        vulkan.addInstanceExtension(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceExtension(VK_EXT_MUTABLE_DESCRIPTOR_TYPE_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_SHADER_FLOAT16_INT8_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_SHADER_INTEGER_DOT_PRODUCT_EXTENSION_NAME)//XeSS
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME)
                .addDeviceExtension("VK_EXT_descriptor_indexing")
                .addDeviceExtension("VK_NVX_binary_import")
                .addDeviceExtension("VK_NVX_image_view_handle")
                .addDeviceExtension(VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME);
        if (Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS) {
            vulkan.addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                    .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME);
        }
        if (Platform.currentPlatform.getOS().type == OperatingSystemType.LINUX) {
            vulkan.addDeviceExtension(KHRExternalMemoryFd.VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME)
                    .addDeviceExtension(KHRExternalSemaphoreFd.VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME);
        }
        try {
            vulkan.initRenderSystem();
            return;
        } catch (VulkanException vkException) {
            VkRenderSystem.LOGGER.error("Vulkan初始化失败，已禁用Vulkan，错误 {}", vkException.getMessage());
            vkException.printStackTrace();
        } catch (Throwable e) {
            VkRenderSystem.LOGGER.error("Vulkan初始化失败，发生未知错误，已禁用Vulkan，错误 {}", e.getMessage());
            e.printStackTrace();
        }
        vulkan = null;
    }

    public static GlRenderSystem opengl() {
        return opengl;
    }

    public static VkRenderSystem vulkan() {
        return vulkan;
    }

    public static GlRenderSystem current() {
        return opengl;
    }
}
