package io.homo.superresolution.core.vulkan;

public class VkException extends RuntimeException {
    public VkException(String message) {
        super(message);
    }

    public VkException() {
        super("Unknown error occurred");
    }
}
