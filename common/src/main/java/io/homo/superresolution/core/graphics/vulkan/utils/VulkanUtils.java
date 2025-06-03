package io.homo.superresolution.core.graphics.vulkan.utils;

import io.homo.superresolution.core.graphics.vulkan.VulkanApplication;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanUtils {
    public static int VK_CHECK(int code) {
        if (code != VK_SUCCESS) {
            VulkanApplication.LOGGER.error("Vulkan error! Code:{}", code);
            throw new VulkanException();
        }
        return code;
    }

    public static int VK_CHECK(int code, String msg) {
        if (code != VK_SUCCESS) {
            VulkanApplication.LOGGER.error("Vulkan error! Code:{}", code);
            throw new VulkanException(msg);
        }
        return code;
    }
}
