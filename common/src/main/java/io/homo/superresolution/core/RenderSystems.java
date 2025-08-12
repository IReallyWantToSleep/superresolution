package io.homo.superresolution.core;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.opengl.GlRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import org.lwjgl.vulkan.KHRExternalMemoryFd;
import org.lwjgl.vulkan.KHRExternalSemaphoreFd;
import org.lwjgl.vulkan.VK;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;

public class RenderSystems {
    private static VkRenderSystem vulkan;
    private static GlRenderSystem opengl;

    public static void init() {
        opengl = new GlRenderSystem();
        opengl.initRenderSystem();
        initVulkan();
    }


    public static void destroy() {
        opengl.destroyRenderSystem();
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
        if (SuperResolutionConfig.isSkipInitVulkan()) return;
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
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)

                .addDeviceExtension(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME);
        if (Platform.currentPlatform.getOS().type == OSType.WINDOWS) {
            vulkan.addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                    .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME);
        }
        if (Platform.currentPlatform.getOS().type == OSType.LINUX) {
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
