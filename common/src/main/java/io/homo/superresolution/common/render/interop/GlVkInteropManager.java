package io.homo.superresolution.common.render.interop;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.render.vulkan.*;
import io.homo.superresolution.common.render.vulkan.shader.VkComputeShader;
import io.homo.superresolution.common.render.vulkan.shader.VkShaderUniform;
import io.homo.superresolution.common.render.vulkan.shader.VkShaderUniformType;
import io.homo.superresolution.common.utils.FileReadHelper;
import org.lwjgl.vulkan.VK;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

public class GlVkInteropManager implements Destroyable {
    public VkApplication vulkanApp;
    public boolean supportVulkan = false;

    public static void main(String[] args) {
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GlVkInteropManager a = new GlVkInteropManager();
        a.init();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        long window = glfwCreateWindow(854, 480, "test", 0L, 0L);
        VkComputeShader s = new VkComputeShader(a.vulkanApp.deviceManager);
        VkShaderUniform bufferUni = new VkShaderUniform()
                .binding(0)
                .type(VkShaderUniformType.buffer)
                .size(256);
        s.addUniform(bufferUni);
        s.addUniform(new VkShaderUniform()
                .binding(1)
                .type(VkShaderUniformType.sampler)
                .sampler(a.vulkanApp.deviceManager.textureSampler)
        );
        s.addUniform(new VkShaderUniform()
                .binding(2)
                .type(VkShaderUniformType.sampledImage)
        );
        s.addUniform(new VkShaderUniform()
                .binding(3)
                .type(VkShaderUniformType.storageImage)
        );
        s.addUniform(new VkShaderUniform()
                .binding(4)
                .type(VkShaderUniformType.sampledImage)
        );
        s.addUniform(new VkShaderUniform()
                .binding(5)
                .type(VkShaderUniformType.sampledImage)
        );
        s.setShaderBin(FileReadHelper.readSpvFile("/shader/nis_scaler_glsl.spv"));
        s.build();
        while (!glfwWindowShouldClose(window)) {
            vkDeviceWaitIdle(a.vulkanApp.deviceManager.device);
            glfwPollEvents();
        }
    }

    public static boolean isSupportVulkan() {
        if (SuperResolution.interopManager != null) {
            if (SuperResolution.interopManager.supportVulkan)
                return SuperResolution.interopManager.vulkanApp != null;
        }
        return false;
    }

    public void init() {
        if (Config.isSkipInitVulkan()) return;
        try {
            VK.create();
        } catch (Exception | Error e) {
            if (!e.getMessage().contains("Vulkan has already been created")) {
                supportVulkan = false;
                VkApplication.LOGGER.error("Vulkan初始化失败，似乎缺少Vulkan运行库，错误 {}", e.getMessage());
                return;
            }
        }
        vulkanApp = VkApplication.create()
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceRequiredExtensions(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME);
        try {
            vulkanApp.init();
            supportVulkan = true;
            return;
        } catch (VkException vkException) {
            VkApplication.LOGGER.error("Vulkan初始化失败，已禁用Vulkan，错误 {}", vkException.getMessage());
        } catch (Exception e) {
            VkApplication.LOGGER.error("Vulkan初始化失败，发生未知错误，已禁用Vulkan，错误 {}", e.getMessage());
        }
        supportVulkan = false;
    }

    @Override
    public void destroy() {
        if (vulkanApp != null) vulkanApp.destroy();
    }
}
