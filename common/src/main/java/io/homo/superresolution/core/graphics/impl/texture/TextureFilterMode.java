package io.homo.superresolution.core.graphics.impl.texture;

public enum TextureFilterMode {
    NEAREST,
    LINEAR;

    public int gl() {
        return switch (this) {
            case NEAREST -> org.lwjgl.opengl.GL11.GL_NEAREST;
            case LINEAR -> org.lwjgl.opengl.GL11.GL_LINEAR;
        };
    }

    public int vk() {
        return switch (this) {
            case NEAREST -> org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
            case LINEAR -> org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
        };
    }
}