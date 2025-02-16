package io.homo.superresolution.common.render.vulkan;

import static org.lwjgl.vulkan.VK10.*;

public enum TextureUsage {
    sampledImage(VK_IMAGE_USAGE_SAMPLED_BIT),
    storageImage(VK_IMAGE_USAGE_STORAGE_BIT);
    private final int value;

    TextureUsage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
