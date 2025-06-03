package io.homo.superresolution.core.graphics.impl.texture;

public enum TextureFilterMode {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR;
    
    public int gl() {
        return switch (this) {
            case NEAREST -> org.lwjgl.opengl.GL11.GL_NEAREST;
            case LINEAR -> org.lwjgl.opengl.GL11.GL_LINEAR;
            case NEAREST_MIPMAP_NEAREST -> org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_NEAREST -> org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_NEAREST;
            case NEAREST_MIPMAP_LINEAR -> org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_LINEAR;
            case LINEAR_MIPMAP_LINEAR -> org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
        };
    }

    public int vk() {
        return switch (this) {
            case NEAREST, NEAREST_MIPMAP_NEAREST, NEAREST_MIPMAP_LINEAR -> org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
            case LINEAR, LINEAR_MIPMAP_NEAREST, LINEAR_MIPMAP_LINEAR -> org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
        };
    }
}