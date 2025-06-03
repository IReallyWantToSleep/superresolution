package io.homo.superresolution.core.graphics.vulkan.utils;

public class VulkanException extends RuntimeException {
    public VulkanException(String message) {
        super(message);
    }

    public VulkanException() {
        super("Unknown error occurred");
    }
}
