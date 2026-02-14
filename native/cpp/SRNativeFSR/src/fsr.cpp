#include "sr/fsr/fsr.h"

FfxResource srTextureResourceToFfxResource(const SRTextureResource *srTex)
{
    FfxResource resource = {};
    resource.resource = (void *)(srTex->handle);
    resource.state = FFX_RESOURCE_STATE_COMPUTE_READ;
    resource.description.format = srTextureFormatToFfxSurfaceFormat(srTex->desc.format);
    resource.description.width = srTex->desc.width;
    resource.description.height = srTex->desc.height;
    resource.description.depth = 1;
    resource.description.mipCount = srTex->desc.mipmapCount;
    resource.description.type = FFX_RESOURCE_TYPE_TEXTURE2D;
    resource.description.usage = srTextureResourceUsageToFfx(srTex->desc.usage);
    resource.description.flags = FFX_RESOURCE_FLAGS_NONE;
#ifdef _DEBUG
    resource.name[0] = L'\0';
#endif
    return resource;
}

FfxSurfaceFormat srTextureFormatToFfxSurfaceFormat(SRTextureFormat format)
{
    switch (format)
    {
    case (SR_TEXTURE_FORMAT_UNKNOWN):
        return FFX_SURFACE_FORMAT_UNKNOWN;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS):
        return FFX_SURFACE_FORMAT_R32G32B32A32_TYPELESS;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_UINT):
        return FFX_SURFACE_FORMAT_R32G32B32A32_UINT;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT):
        return FFX_SURFACE_FORMAT_R32G32B32A32_FLOAT;
    case (SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT):
        return FFX_SURFACE_FORMAT_R16G16B16A16_FLOAT;
    case (SR_TEXTURE_FORMAT_R32G32B32_FLOAT):
        return FFX_SURFACE_FORMAT_R32G32B32_FLOAT;
    case (SR_TEXTURE_FORMAT_R32G32_FLOAT):
        return FFX_SURFACE_FORMAT_R32G32_FLOAT;
    case (SR_TEXTURE_FORMAT_R8_UINT):
        return FFX_SURFACE_FORMAT_R8_UINT;
    case (SR_TEXTURE_FORMAT_R32_UINT):
        return FFX_SURFACE_FORMAT_R32_UINT;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS):
        return FFX_SURFACE_FORMAT_R8G8B8A8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_UNORM):
        return FFX_SURFACE_FORMAT_R8G8B8A8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_SNORM):
        return FFX_SURFACE_FORMAT_R8G8B8A8_SNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_SRGB):
        return FFX_SURFACE_FORMAT_R8G8B8A8_SRGB;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS):
        return FFX_SURFACE_FORMAT_B8G8R8A8_UNORM;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_UNORM):
        return FFX_SURFACE_FORMAT_B8G8R8A8_UNORM;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_SRGB):
        return FFX_SURFACE_FORMAT_B8G8R8A8_SRGB;
    case (SR_TEXTURE_FORMAT_R11G11B10_FLOAT):
        return FFX_SURFACE_FORMAT_R11G11B10_FLOAT;
    case (SR_TEXTURE_FORMAT_R10G10B10A2_UNORM):
        return FFX_SURFACE_FORMAT_R10G10B10A2_UNORM;
    case (SR_TEXTURE_FORMAT_R16G16_FLOAT):
        return FFX_SURFACE_FORMAT_R16G16_FLOAT;
    case (SR_TEXTURE_FORMAT_R16G16_UINT):
        return FFX_SURFACE_FORMAT_R16G16_UINT;
    case (SR_TEXTURE_FORMAT_R16G16_SINT):
        return FFX_SURFACE_FORMAT_R16G16_SINT;
    case (SR_TEXTURE_FORMAT_R16_FLOAT):
        return FFX_SURFACE_FORMAT_R16_FLOAT;
    case (SR_TEXTURE_FORMAT_R16_UINT):
        return FFX_SURFACE_FORMAT_R16_UINT;
    case (SR_TEXTURE_FORMAT_R16_UNORM):
        return FFX_SURFACE_FORMAT_R16_UNORM;
    case (SR_TEXTURE_FORMAT_R16_SNORM):
        return FFX_SURFACE_FORMAT_R16_SNORM;
    case (SR_TEXTURE_FORMAT_R8_UNORM):
        return FFX_SURFACE_FORMAT_R8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8_UNORM):
        return FFX_SURFACE_FORMAT_R8G8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8_UINT):
        return FFX_SURFACE_FORMAT_R8G8_UINT;
    case (SR_TEXTURE_FORMAT_R32_FLOAT):
        return FFX_SURFACE_FORMAT_R32_FLOAT;
    case (SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP):
        return FFX_SURFACE_FORMAT_R9G9B9E5_SHAREDEXP;
    case (SR_TEXTURE_FORMAT_D32_SFLOAT):
        return FFX_SURFACE_FORMAT_UNKNOWN;
    default:
        return FFX_SURFACE_FORMAT_UNKNOWN;
    }
}

FfxResourceUsage srTextureResourceUsageToFfx(SRResourceUsage usage)
{
    FfxResourceUsage ffxUsage = FFX_RESOURCE_USAGE_READ_ONLY;
    if (usage == SR_RESOURCE_USAGE_RENDERTARGET)
        ffxUsage = FFX_RESOURCE_USAGE_RENDERTARGET;
    else if (usage == SR_RESOURCE_USAGE_UAV)
        ffxUsage = FFX_RESOURCE_USAGE_UAV;
    else if (usage == SR_RESOURCE_USAGE_DEPTHTARGET)
        ffxUsage = FFX_RESOURCE_USAGE_DEPTHTARGET;
    else if (usage == SR_RESOURCE_USAGE_INDIRECT)
        ffxUsage = FFX_RESOURCE_USAGE_INDIRECT;
    else if (usage == SR_RESOURCE_USAGE_ARRAYVIEW)
        ffxUsage = FFX_RESOURCE_USAGE_ARRAYVIEW;
    else if (usage == SR_RESOURCE_USAGE_STENCILTARGET)
        ffxUsage = FFX_RESOURCE_USAGE_STENCILTARGET;
    else if (usage == SR_RESOURCE_USAGE_DCC_RENDERTARGET)
        ffxUsage = FFX_RESOURCE_USAGE_DCC_RENDERTARGET;
    return ffxUsage;
}