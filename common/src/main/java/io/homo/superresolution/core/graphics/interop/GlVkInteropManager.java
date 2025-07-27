package io.homo.superresolution.core.graphics.interop;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.vulkan.VulkanApplication;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.vulkan.VK;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;

public class GlVkInteropManager implements Destroyable {
    public VulkanApplication vulkanApp;
    public boolean supportVulkan = false;

    public static boolean isSupportVulkan() {
        if (SuperResolution.interopManager != null && !SuperResolutionConfig.isSkipInitVulkan()) {
            if (SuperResolution.interopManager.supportVulkan)
                return SuperResolution.interopManager.vulkanApp != null;
        }
        return false;
    }

    public void init() {
        if (SuperResolutionConfig.isSkipInitVulkan()) return;
        try {
            VK.create();
        } catch (Exception | Error e) {
            if (!e.getMessage().contains("Vulkan has already been created")) {
                supportVulkan = false;
                VulkanApplication.LOGGER.error("Vulkan初始化失败，似乎缺少Vulkan运行库，错误 {}", e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        vulkanApp = new VulkanApplication();
        vulkanApp
                .addInstanceExtension(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME)
                .addDeviceExtension(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME);
        try {
            vulkanApp.init();
            supportVulkan = true;
            return;
        } catch (VulkanException vkException) {
            VulkanApplication.LOGGER.error("Vulkan初始化失败，已禁用Vulkan，错误 {}", vkException.getMessage());
            vkException.printStackTrace();
        } catch (Exception e) {
            VulkanApplication.LOGGER.error("Vulkan初始化失败，发生未知错误，已禁用Vulkan，错误 {}", e.getMessage());
            e.printStackTrace();
        }
        supportVulkan = false;
    }

    @Override
    public void destroy() {
        if (vulkanApp != null) vulkanApp.destroy();
    }
}
