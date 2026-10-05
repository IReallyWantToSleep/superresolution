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

package io.homo.superresolution.srapi;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsage;

import java.util.EnumSet;

public class SRTextureResourceDescription {
    public SRSurfaceFormat format;
    public int width;
    public int height;
    public int mipmapCount;
    public int usage;

    public SRTextureResourceDescription(SRSurfaceFormat format, int width, int height, int mipmapCount, int usage) {
        this.format = format;
        this.width = width;
        this.height = height;
        this.mipmapCount = mipmapCount;
        this.usage = usage;
    }

    public SRTextureResourceDescription(int format, int width, int height, int mipmapCount, int usage) {
        this(
                SRSurfaceFormat.fromValue(format),
                width,
                height,
                mipmapCount,
                usage
        );
    }

    public SRTextureResourceDescription(ITexture texture) {
        this.format = switch (texture.getTextureFormat()) {
            case RGBA8 -> SRSurfaceFormat.R8G8B8A8_UNORM;
            case RGBA16F -> SRSurfaceFormat.R16G16B16A16_FLOAT;
            case RGB8, RGB16F -> SRSurfaceFormat.UNKNOWN;
            case RG16F -> SRSurfaceFormat.R16G16_FLOAT;
            case RG32F -> SRSurfaceFormat.R32G32_FLOAT;
            case RG8 -> SRSurfaceFormat.R8G8_UNORM;
            case R16F -> SRSurfaceFormat.R16_FLOAT;
            case R8 -> SRSurfaceFormat.R8_UNORM;
            case R32F -> SRSurfaceFormat.R32_FLOAT;
            case R32UI -> SRSurfaceFormat.R32_UINT;
            case DEPTH32 -> SRSurfaceFormat.R32_TYPELESS;
            case DEPTH32F -> SRSurfaceFormat.D32_SFLOAT;
            case DEPTH24_STENCIL8, DEPTH24, DEPTH_COMPONENT, DEPTH32F_STENCIL8 -> SRSurfaceFormat.UNKNOWN;
            case R16_SNORM -> SRSurfaceFormat.R16_SNORM;
            case R11G11B10F -> SRSurfaceFormat.R11G11B10_FLOAT;
            case RGBA16 -> SRSurfaceFormat.R16G16B16A16_TYPELESS;
            case RGBA16_SNORM -> SRSurfaceFormat.R16G16B16A16_SNORM;
            case RGBA32F -> SRSurfaceFormat.R32G32B32A32_FLOAT;
        };

        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.mipmapCount = texture.getMipmapSettings().getLevels();

        EnumSet<SRResourceUsage> usages = EnumSet.noneOf(SRResourceUsage.class);
        for (TextureUsage textureUsage : texture.getTextureUsages().getUsages()) {
            switch (textureUsage) {
                case Sampler -> usages.add(SRResourceUsage.READ_ONLY);
                case Storage -> usages.add(SRResourceUsage.UAV);
                case TransferSource -> usages.add(SRResourceUsage.INDIRECT);
                case TransferDestination -> usages.add(SRResourceUsage.RENDERTARGET);
                case AttachmentColor -> usages.add(SRResourceUsage.RENDERTARGET);
                case AttachmentDepth -> usages.add(SRResourceUsage.DEPTHTARGET);
            }
        }
        this.usage = SRResourceUsage.toBitmask(usages);
    }

}
