#include "sr/nss/nss.h"

namespace {

uint32_t toFfxResourceState(SRResourceStates state, uint32_t fallbackState) {
    uint32_t result = 0;

    if (state & SR_RESOURCE_STATE_COMMON) {
        result |= FFX_API_RESOURCE_STATE_COMMON;
    }
    if (state & SR_RESOURCE_STATE_UNORDERED_ACCESS) {
        result |= FFX_API_RESOURCE_STATE_UNORDERED_ACCESS;
    }
    if (state & SR_RESOURCE_STATE_COMPUTE_READ) {
        result |= FFX_API_RESOURCE_STATE_COMPUTE_READ;
    }
    if (state & SR_RESOURCE_STATE_PIXEL_READ) {
        result |= FFX_API_RESOURCE_STATE_PIXEL_READ;
    }
    if (state & SR_RESOURCE_STATE_COPY_SRC) {
        result |= FFX_API_RESOURCE_STATE_COPY_SRC;
    }
    if (state & SR_RESOURCE_STATE_COPY_DEST) {
        result |= FFX_API_RESOURCE_STATE_COPY_DEST;
    }
    if (state & SR_RESOURCE_STATE_INDIRECT_ARGUMENT) {
        result |= FFX_API_RESOURCE_STATE_INDIRECT_ARGUMENT;
    }
    //if (state & SR_RESOURCE_STATE_PRESENT) {
    //    result |= FFX_API_RESOURCE_STATE_PRESENT;
    //}
    if (state & SR_RESOURCE_STATE_RENDER_TARGET) {
        result |= FFX_API_RESOURCE_STATE_RENDER_TARGET;
    }
    if (state & SR_RESOURCE_STATE_DEPTH_ATTACHEMENT) {
        result |= FFX_API_RESOURCE_STATE_DEPTH_STENCIL_READ;
    }

    return result != 0 ? result : fallbackState;
}

}  // namespace

uint32_t srTextureFormatToFfxSurfaceFormat(SRTextureFormat format) {
    switch (format) {
        case SR_TEXTURE_FORMAT_UNKNOWN:
            return FFX_API_SURFACE_FORMAT_UNKNOWN;
        case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R32G32B32A32_TYPELESS;
        case SR_TEXTURE_FORMAT_R32G32B32A32_UINT:
            return FFX_API_SURFACE_FORMAT_R32G32B32A32_UINT;
        case SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT:
            return FFX_API_SURFACE_FORMAT_R32G32B32A32_FLOAT;
        case SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT:
            return FFX_API_SURFACE_FORMAT_R16G16B16A16_FLOAT;
        case SR_TEXTURE_FORMAT_R32G32B32_FLOAT:
            return FFX_API_SURFACE_FORMAT_R32G32B32_FLOAT;
        case SR_TEXTURE_FORMAT_R32G32_FLOAT:
            return FFX_API_SURFACE_FORMAT_R32G32_FLOAT;
        case SR_TEXTURE_FORMAT_R8_UINT:
            return FFX_API_SURFACE_FORMAT_R8_UINT;
        case SR_TEXTURE_FORMAT_R32_UINT:
            return FFX_API_SURFACE_FORMAT_R32_UINT;
        case SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R8G8B8A8_TYPELESS;
        case SR_TEXTURE_FORMAT_R8G8B8A8_UNORM:
            return FFX_API_SURFACE_FORMAT_R8G8B8A8_UNORM;
        case SR_TEXTURE_FORMAT_R8G8B8A8_SNORM:
            return FFX_API_SURFACE_FORMAT_R8G8B8A8_SNORM;
        case SR_TEXTURE_FORMAT_R8G8B8A8_SRGB:
            return FFX_API_SURFACE_FORMAT_R8G8B8A8_SRGB;
        case SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS:
            return FFX_API_SURFACE_FORMAT_B8G8R8A8_TYPELESS;
        case SR_TEXTURE_FORMAT_B8G8R8A8_UNORM:
            return FFX_API_SURFACE_FORMAT_B8G8R8A8_UNORM;
        case SR_TEXTURE_FORMAT_B8G8R8A8_SRGB:
            return FFX_API_SURFACE_FORMAT_B8G8R8A8_SRGB;
        case SR_TEXTURE_FORMAT_R11G11B10_FLOAT:
            return FFX_API_SURFACE_FORMAT_R11G11B10_FLOAT;
        case SR_TEXTURE_FORMAT_R10G10B10A2_UNORM:
            return FFX_API_SURFACE_FORMAT_R10G10B10A2_UNORM;
        case SR_TEXTURE_FORMAT_R16G16_FLOAT:
            return FFX_API_SURFACE_FORMAT_R16G16_FLOAT;
        case SR_TEXTURE_FORMAT_R16G16_UINT:
            return FFX_API_SURFACE_FORMAT_R16G16_UINT;
        case SR_TEXTURE_FORMAT_R16G16_SINT:
            return FFX_API_SURFACE_FORMAT_R16G16_SINT;
        case SR_TEXTURE_FORMAT_R16_FLOAT:
            return FFX_API_SURFACE_FORMAT_R16_FLOAT;
        case SR_TEXTURE_FORMAT_R16_UINT:
            return FFX_API_SURFACE_FORMAT_R16_UINT;
        case SR_TEXTURE_FORMAT_R16_UNORM:
            return FFX_API_SURFACE_FORMAT_R16_UNORM;
        case SR_TEXTURE_FORMAT_R16_SNORM:
            return FFX_API_SURFACE_FORMAT_R16_SNORM;
        case SR_TEXTURE_FORMAT_R8_UNORM:
            return FFX_API_SURFACE_FORMAT_R8_UNORM;
        case SR_TEXTURE_FORMAT_R8G8_UNORM:
            return FFX_API_SURFACE_FORMAT_R8G8_UNORM;
        case SR_TEXTURE_FORMAT_R8G8_UINT:
            return FFX_API_SURFACE_FORMAT_R8G8_UINT;
        case SR_TEXTURE_FORMAT_R32_FLOAT:
            return FFX_API_SURFACE_FORMAT_R32_FLOAT;
        case SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP:
            return FFX_API_SURFACE_FORMAT_R9G9B9E5_SHAREDEXP;
        case SR_TEXTURE_FORMAT_R16G16B16A16_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R16G16B16A16_TYPELESS;
        case SR_TEXTURE_FORMAT_R32G32_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R32G32_TYPELESS;
        case SR_TEXTURE_FORMAT_R10G10B10A2_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R10G10B10A2_TYPELESS;
        case SR_TEXTURE_FORMAT_R16G16_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R16G16_TYPELESS;
        case SR_TEXTURE_FORMAT_R16_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R16_TYPELESS;
        case SR_TEXTURE_FORMAT_R8_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R8_TYPELESS;
        case SR_TEXTURE_FORMAT_R8G8_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R8G8_TYPELESS;
        case SR_TEXTURE_FORMAT_R32_TYPELESS:
            return FFX_API_SURFACE_FORMAT_R32_TYPELESS;
        case SR_TEXTURE_FORMAT_D32_SFLOAT:
            // NSS consumes depth as a scalar float resource.
            return FFX_API_SURFACE_FORMAT_R32_FLOAT;
        case SR_TEXTURE_FORMAT_R16G16B16A16_SNORM:
            // The FFX API has no R16G16B16A16_SNORM entry.
            return FFX_API_SURFACE_FORMAT_UNKNOWN;
        default:
            return FFX_API_SURFACE_FORMAT_UNKNOWN;
    }
}

uint32_t srTextureResourceUsageToFfx(SRResourceUsage usage) {
    uint32_t result = FFX_API_RESOURCE_USAGE_READ_ONLY;

    if (usage & SR_RESOURCE_USAGE_RENDERTARGET) {
        result |= FFX_API_RESOURCE_USAGE_RENDERTARGET;
    }
    if (usage & SR_RESOURCE_USAGE_UAV) {
        result |= FFX_API_RESOURCE_USAGE_UAV;
    }
    if (usage & SR_RESOURCE_USAGE_DEPTHTARGET) {
        result |= FFX_API_RESOURCE_USAGE_DEPTHTARGET;
    }
    if (usage & SR_RESOURCE_USAGE_INDIRECT) {
        result |= FFX_API_RESOURCE_USAGE_INDIRECT;
    }
    if (usage & SR_RESOURCE_USAGE_ARRAYVIEW) {
        result |= FFX_API_RESOURCE_USAGE_ARRAYVIEW;
    }
    if (usage & SR_RESOURCE_USAGE_STENCILTARGET) {
        result |= FFX_API_RESOURCE_USAGE_STENCILTARGET;
    }
    // FFX_API_RESOURCE_USAGE has no DCC equivalent; the storage/render-target
    // bits above preserve the usages that are relevant to NSS.
    return result;
}

uint32_t srTextureResourceStateToFfx(SRResourceStates state, uint32_t fallbackState) {
    return toFfxResourceState(state, fallbackState);
}

FfxApiResource srTextureResourceToFfxResource(const SRTextureResource *srTex) {
    FfxApiResource resource = {};
    if (!srTex || !srTex->exist || !srTex->handle) {
        return resource;
    }

    resource.resource = srTex->handle;
    resource.state = srTextureResourceStateToFfx(
        srTex->state,
        FFX_API_RESOURCE_STATE_COMPUTE_READ);
    resource.description.type = FFX_API_RESOURCE_TYPE_TEXTURE2D;
    resource.description.format = srTextureFormatToFfxSurfaceFormat(srTex->desc.format);
    resource.description.width = srTex->desc.width;
    resource.description.height = srTex->desc.height;
    resource.description.depth = 1;
    resource.description.mipCount = srTex->desc.mipmapCount;
    resource.description.flags = FFX_API_RESOURCE_FLAGS_NONE;
    resource.description.usage = srTextureResourceUsageToFfx(srTex->desc.usage);
    return resource;
}

FfxApiResource srTextureResourceToFfxOutputResource(const SRTextureResource *srTex) {
    FfxApiResource resource = srTextureResourceToFfxResource(srTex);
    if (resource.resource) {
        // SR output resources are write targets even when the Java-side
        // wrapper still carries its read-state default.
        resource.state = FFX_API_RESOURCE_STATE_UNORDERED_ACCESS;
    }
    return resource;
}
