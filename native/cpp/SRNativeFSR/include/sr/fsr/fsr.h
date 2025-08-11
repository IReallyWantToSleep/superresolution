#pragma once
#include "sr/sr_api.h"

#include "FidelityFX/host/ffx_fsr2.h"
#include "FidelityFX/host/backends/vk/ffx_vk.h"

#define SRFSR_CHECK(_expr)                         \
    if (FfxErrorCode _rc = (_expr); _rc != FFX_OK) \
    return (SRReturnCode)SR_RETURN_CODE_ERROR

FfxResource SRTextureResourceToFfxResource(const SRTextureResource *srTex)
{
    FfxResourceDescription desc = {};
    desc.format = static_cast<FfxSurfaceFormat>(srTex->desc.format);
    desc.width = srTex->desc.width;
    desc.height = srTex->desc.height;
    desc.mipCount = srTex->desc.mipmapCount;
    desc.type = FFX_RESOURCE_TYPE_TEXTURE2D;
    desc.usage = static_cast<FfxResourceUsage>(srTex->desc.usage);
    desc.flags = FFX_RESOURCE_FLAGS_NONE;
    return ffxGetResourceVK(
        (void *)(srTex->handle),
        desc,
        nullptr);
}