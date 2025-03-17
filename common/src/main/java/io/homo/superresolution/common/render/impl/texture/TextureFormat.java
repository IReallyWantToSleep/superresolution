package io.homo.superresolution.common.render.impl.texture;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.homo.superresolution.common.render.gl.GlConst.*;
import static org.lwjgl.opengl.GL30.GL_R32UI;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.vulkan.VK10.*;

public enum TextureFormat {
    RGBA8(GL_RGBA8, VK_FORMAT_R8G8B8A8_UNORM),
    RGBA16F(GL_RGBA16F, VK_FORMAT_R16G16B16A16_SFLOAT),
    RGB8(GL_RGB8, VK_FORMAT_R8G8B8_UNORM),
    RG16F(GL_RG16F, VK_FORMAT_R16G16_SFLOAT),
    R32F(GL_R32F, VK_FORMAT_R32_SFLOAT),
    R32UI(GL_R32UI, VK_FORMAT_R32_UINT),
    DEPTH32F(GL_DEPTH_COMPONENT32F, VK_FORMAT_D32_SFLOAT),
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, VK_FORMAT_D24_UNORM_S8_UINT),
    DEPTH24(GL_DEPTH_COMPONENT24, VK_FORMAT_X8_D24_UNORM_PACK32);


    private static final Map<Integer, TextureFormat> GL_TO_FORMAT;
    private static final Map<Integer, TextureFormat> VK_TO_FORMAT;

    static {
        Map<Integer, TextureFormat> glMap = new HashMap<>();
        Map<Integer, TextureFormat> vkMap = new HashMap<>();

        for (TextureFormat format : values()) {
            glMap.put(format.glFormat, format);
            vkMap.put(format.vkFormat, format);
        }

        GL_TO_FORMAT = Collections.unmodifiableMap(glMap);
        VK_TO_FORMAT = Collections.unmodifiableMap(vkMap);
    }

    private final int glFormat;
    private final int vkFormat;

    TextureFormat(int glFormat, int vkFormat) {
        this.glFormat = glFormat;
        this.vkFormat = vkFormat;
    }

    public static @NotNull TextureFormat fromVk(int format) {
        TextureFormat result = VK_TO_FORMAT.get(format);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported Vulkan format: 0x" +
                    Integer.toHexString(format).toUpperCase());
        }
        return result;
    }

    public static @NotNull TextureFormat fromGl(int format) {
        TextureFormat result = GL_TO_FORMAT.get(format);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported OpenGL format: 0x" +
                    Integer.toHexString(format).toUpperCase());
        }
        return result;
    }

    public int gl() {
        return glFormat;
    }

    public int vk() {
        return vkFormat;
    }
}