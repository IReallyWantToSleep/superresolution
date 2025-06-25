package io.homo.superresolution.core.graphics.impl.texture;

public enum TextureWrapMode {
    REPEAT,
    MIRRORED_REPEAT,
    CLAMP_TO_EDGE,
    CLAMP_TO_BORDER;

    public int gl() {
        return switch (this) {
            case REPEAT -> org.lwjgl.opengl.GL11.GL_REPEAT;
            case MIRRORED_REPEAT -> org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT;
            case CLAMP_TO_EDGE -> org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
            case CLAMP_TO_BORDER -> org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
            default -> throw new IllegalArgumentException("未知的TextureWrapMode: " + this);
        };
    }

    public int vk() {
        return switch (this) {
            case REPEAT -> org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
            case MIRRORED_REPEAT -> org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
            case CLAMP_TO_EDGE -> org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
            case CLAMP_TO_BORDER -> org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
            default -> throw new IllegalArgumentException("未知的TextureWrapMode: " + this);
        };
    }
}