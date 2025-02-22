package io.homo.superresolution.common.render.vulkan.texture;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class VkAllocatedImage {
    public long image = VK_NULL_HANDLE;
    public long memory = VK_NULL_HANDLE;
    public long allocationSize = 0;

    public void destroy(VkDevice device) {
        vkDestroyImage(device, image, null);
        vkFreeMemory(device, memory, null);
        image = VK_NULL_HANDLE;
        memory = VK_NULL_HANDLE;
    }
}
