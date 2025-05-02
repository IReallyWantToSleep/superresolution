package io.homo.superresolution.core.vulkan.shader;

import static org.lwjgl.vulkan.VK10.*;

public enum VkShaderUniformType {
    buffer(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC),
    sampledImage(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE),
    storageImage(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE),
    sampler(VK_DESCRIPTOR_TYPE_SAMPLER);
    private final int value;

    VkShaderUniformType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
