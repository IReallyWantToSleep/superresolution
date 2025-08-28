#include "sr/fsr/fsr.h"

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
