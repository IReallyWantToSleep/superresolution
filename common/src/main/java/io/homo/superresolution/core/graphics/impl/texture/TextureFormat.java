/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.impl.texture;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.vulkan.VK10.*;

public enum TextureFormat {
    RGBA8(
            GL_RGBA8, VK_FORMAT_R8G8B8A8_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B,
            TextureComponent.A
    ),
    RGBA16F(
            GL_RGBA16F, VK_FORMAT_R16G16B16A16_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B,
            TextureComponent.A
    ),
    RGBA32F(
            GL_RGBA32F, VK_FORMAT_R32G32B32A32_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B,
            TextureComponent.A
    ),
    RGB8(
            GL_RGB8, VK_FORMAT_R8G8B8_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B
    ),
    RGB16F(
            GL_RGB16F, VK_FORMAT_R16G16B16_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B
    ),
    RGBA16(
            GL_RGBA16, VK_FORMAT_R16G16B16A16_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B,
            TextureComponent.A
    ),
    RG16F(
            GL_RG16F, VK_FORMAT_R16G16_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G
    ),
    RG32F(
            GL_RG32F, VK_FORMAT_R32G32_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G
    ),
    RG8(
            GL_RG8, VK_FORMAT_R8G8_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.R,
            TextureComponent.G
    ),
    R16F(
            GL_R16F, VK_FORMAT_R16_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R
    ),
    R8(
            GL_R8, VK_FORMAT_R8_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.R
    ),
    R32F(
            GL_R32F, VK_FORMAT_R32_SFLOAT,
            DataType.FLOAT,
            TextureComponent.R
    ),
    R32UI(
            GL_R32UI, VK_FORMAT_R32_UINT,
            DataType.UNSIGNED_INTEGER,
            TextureComponent.R
    ),
    DEPTH32F(
            GL_DEPTH_COMPONENT32F, VK_FORMAT_D32_SFLOAT,
            DataType.FLOAT,
            TextureComponent.Depth
    ),
    DEPTH32F_STENCIL8(
            GL_DEPTH32F_STENCIL8, VK_FORMAT_D32_SFLOAT_S8_UINT,
            DataType.FLOAT,
            TextureComponent.Depth,
            TextureComponent.Stencil
    ),
    DEPTH24_STENCIL8(
            GL_DEPTH24_STENCIL8, VK_FORMAT_D24_UNORM_S8_UINT,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.Depth,
            TextureComponent.Stencil
    ),
    DEPTH24(
            GL_DEPTH_COMPONENT24, VK_FORMAT_X8_D24_UNORM_PACK32,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.Depth
    ),
    DEPTH32(
            GL_DEPTH_COMPONENT32, VK_FORMAT_D32_SFLOAT,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.Depth
    ),
    R16_SNORM(
            GL_R16_SNORM, VK_FORMAT_R16_SNORM,
            DataType.SIGNED_NORMALIZED,
            TextureComponent.R
    ),
    R11G11B10F(
            GL_R11F_G11F_B10F,
            VK_FORMAT_B10G11R11_UFLOAT_PACK32,
            DataType.FLOAT,
            TextureComponent.R,
            TextureComponent.G,
            TextureComponent.B
    ),
    RGBA16_SNORM(
            GL_RGBA16_SNORM, VK_FORMAT_R16G16B16A16_SNORM,
            DataType.SIGNED_NORMALIZED,
            TextureComponent.R, TextureComponent.G, TextureComponent.B, TextureComponent.A
    ),
    @Deprecated
    DEPTH_COMPONENT(
            GL_DEPTH_COMPONENT, VK_FORMAT_D16_UNORM,
            DataType.UNSIGNED_NORMALIZED,
            TextureComponent.Depth
    );

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

    private final EnumSet<TextureComponent> components = EnumSet.noneOf(TextureComponent.class);
    private final int glFormat;
    private final int vkFormat;
    private final DataType dataType;

    TextureFormat(int glFormat, int vkFormat, DataType dataType, TextureComponent... component) {
        this.glFormat = glFormat;
        this.vkFormat = vkFormat;
        this.dataType = dataType;
        if (component != null && component.length > 0) {
            components.addAll(List.of(component));
        }
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

    public boolean isStencil() {
        return this.components.contains(TextureComponent.Stencil);
    }

    public boolean isDepth() {
        return this.components.contains(TextureComponent.Depth);
    }

    public boolean isInteger() {
        return dataType == DataType.UNSIGNED_INTEGER || dataType == DataType.SIGNED_INTEGER;
    }

    public boolean isFloat() {
        return dataType == DataType.FLOAT;
    }

    public boolean isNormalized() {
        return dataType == DataType.UNSIGNED_NORMALIZED || dataType == DataType.SIGNED_NORMALIZED;
    }

    public int getBytesPerPixel() {
        return switch (this) {
            case RGBA8 -> 4;
            case RGB8 -> 3;
            case RG8 -> 2;
            case R8 -> 1;
            case R16_SNORM, R16F -> 2;
            case RG16F -> 4;
            case RG32F -> 8;
            case R32F, R32UI -> 4;
            case RGBA16F,RGBA16_SNORM ,RGBA16 -> 8;
            case RGBA32F -> 16;
            case RGB16F, DEPTH32F_STENCIL8 -> 6;
            case R11G11B10F -> 4;
            case DEPTH32F, DEPTH32, DEPTH_COMPONENT -> 4;
            case DEPTH24, DEPTH24_STENCIL8 -> 4;
        };
    }

    public int getChannelCount() {
        return components.size();
    }

    public boolean hasRedChannel() {
        return components.contains(TextureComponent.R);
    }

    public boolean hasGreenChannel() {
        return components.contains(TextureComponent.G);
    }

    public boolean hasBlueChannel() {
        return components.contains(TextureComponent.B);
    }

    public boolean hasAlphaChannel() {
        return components.contains(TextureComponent.A);
    }

    public boolean isDepthStencil() {
        return isDepth() && isStencil();
    }

    public DataType getDataType() {
        return dataType;
    }

    public int gl() {
        return glFormat;
    }

    public int vk() {
        return vkFormat;
    }

    public String getGlslFormatQualifier() {
        return switch (this) {
            case RGBA8 -> "rgba8";
            case RGBA16F -> "rgba16f";
            case RGBA32F -> "rgba32f";
            case RGBA16 -> "rgba16";
            case RG16F -> "rg16f";
            case RG32F -> "rg32f";
            case RG8 -> "rg8";
            case R16F -> "r16f";
            case R8 -> "r8";
            case R32F -> "r32f";
            case R32UI -> "r32ui";
            case R16_SNORM -> "r16_snorm";
            case R11G11B10F -> "r11f_g11f_b10f";
            case RGBA16_SNORM -> "rgba16_snorm";
            default -> null;
        };
    }

    public enum TextureComponent {
        R,
        G,
        B,
        A,
        Depth,
        Stencil
    }

    public enum DataType {
        UNSIGNED_INTEGER,
        SIGNED_INTEGER,
        FLOAT,
        UNSIGNED_NORMALIZED,
        SIGNED_NORMALIZED,
    }
}