package io.homo.superresolution.common.render.vulkan;

import static io.homo.superresolution.common.render.gl.GlConst.GL_RG16F;
import static io.homo.superresolution.common.render.gl.GlConst.GL_RGBA8;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R16G16_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;

public enum TextureFormat {
    RGBA8, RG16F;

    public static int toGL(TextureFormat format) {
        return switch (format) {
            case RG16F -> GL_RG16F;
            case RGBA8 -> GL_RGBA8;
        };
    }

    public static int toVK(TextureFormat format) {
        return switch (format) {
            case RG16F -> VK_FORMAT_R16G16_UNORM;
            case RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
        };
    }
}
