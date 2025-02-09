package io.homo.superresolution.common.render;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.render.gl.texture.NativeImageTexture;
import io.homo.superresolution.common.render.vulkan.VkApplication;
import io.homo.superresolution.common.render.vulkan.VkComputeShader;
import io.homo.superresolution.common.render.vulkan.VkShaderUniform;
import io.homo.superresolution.common.render.vulkan.VkShaderUniformType;
import io.homo.superresolution.common.utils.FileReadHelper;

import static io.homo.superresolution.common.render.gl.GlConst.GL_RGBA8;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemory.VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphore.VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRFormatFeatureFlags2.VK_KHR_FORMAT_FEATURE_FLAGS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

public class GlVkInteropManager implements Destroyable {
    public VkApplication vulkanApp;

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
        NativeImageTexture testTex = new NativeImageTexture(278, 180, GL_RGBA8, FileReadHelper.readTexture("logo.png"));
        while (!glfwWindowShouldClose(window)) {
            vkDeviceWaitIdle(a.vulkanApp.deviceManager.device);
            glfwPollEvents();

        }
    }

    public void init() {
        vulkanApp = VkApplication.create()
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addInstanceRequiredExtensions(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_FORMAT_FEATURE_FLAGS_2_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_WIN32_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME)
                .init();
    }

    @Override
    public void destroy() {
        if (vulkanApp != null) vulkanApp.destroy();
    }
}
