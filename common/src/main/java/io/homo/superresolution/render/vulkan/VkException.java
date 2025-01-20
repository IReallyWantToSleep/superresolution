package io.homo.superresolution.render.vulkan;

public class VkException extends RuntimeException {
    public VkException(String message) {
        super(message);
    }
    public VkException() {
        super("Unknown error occurred");
    }
}
