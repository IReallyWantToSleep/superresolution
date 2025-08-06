package io.homo.superresolution.core;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperResolutionNativeHelper {
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-Native");

    public static void CPP_Log(String msg, int level) {
        switch (level) {
            case 0 -> LOGGER_CPP.info(msg);
            case 1 -> LOGGER_CPP.warn(msg);
            case 2 -> LOGGER_CPP.error(msg);
            case 3 -> LOGGER_CPP.debug(msg);
        }
    }

    public static long CPP_glfwGetProcAddress(String name) {
        return GLFW.glfwGetProcAddress(name);
    }

    public static long CPP_vkGetDeviceProcAddr(String name) {
        return VK10.vkGetDeviceProcAddr(
                ((VulkanDevice) RenderSystems.vulkan().device()).getVkDevice(),
                name
        );
    }
}
