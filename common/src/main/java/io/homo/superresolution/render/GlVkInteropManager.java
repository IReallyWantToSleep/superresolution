package io.homo.superresolution.render;

import io.homo.superresolution.impl.Destroyable;
import io.homo.superresolution.render.vulkan.VkApplication;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRExternalSemaphoreCapabilities.VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;

public class GlVkInteropManager implements Destroyable {
    public VkApplication vulkanApp;

    public static void main(String[] args) {
        glfwInit();
        GlVkInteropManager a = new GlVkInteropManager();
        a.init();
        a.vulkanApp.loop();

    }

    public void init(){
        vulkanApp = VkApplication.create()
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME)
                .addInstanceRequiredExtensions(VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME)
                .addDeviceRequiredExtensions(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
                .init();
    }

    @Override
    public void destroy() {
        if (vulkanApp != null)vulkanApp.destroy();
    }
}
