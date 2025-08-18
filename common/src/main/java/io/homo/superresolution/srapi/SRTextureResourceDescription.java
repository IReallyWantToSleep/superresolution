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
            case DEPTH32F -> SRSurfaceFormat.R32_FLOAT;
            case DEPTH24_STENCIL8, DEPTH24, DEPTH_COMPONENT, DEPTH32F_STENCIL8 -> SRSurfaceFormat.UNKNOWN;
            case R16_SNORM -> SRSurfaceFormat.R16_SNORM;
            case R11G11B10F -> SRSurfaceFormat.R11G11B10_FLOAT;
            case RGBA16 -> SRSurfaceFormat.R16G16B16A16_TYPELESS;
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
