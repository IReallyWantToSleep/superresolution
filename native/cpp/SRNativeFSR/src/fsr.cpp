#include "sr/fsr/fsr.h"

FfxResource srTextureResourceToFfxResource(const SRTextureResource* srTex)
{
    FfxResource resource = {};
    resource.resource = (void*)(srTex->handle);
    resource.state = FFX_RESOURCE_STATE_COMPUTE_READ;
    resource.description.format = static_cast<FfxSurfaceFormat>(srTex->desc.format);
    resource.description.width = srTex->desc.width;
    resource.description.height = srTex->desc.height;
    resource.description.depth = 1;
    resource.description.mipCount = srTex->desc.mipmapCount;
    resource.description.type = FFX_RESOURCE_TYPE_TEXTURE2D;
    resource.description.usage = static_cast<FfxResourceUsage>(srTex->desc.usage);
    resource.description.flags = FFX_RESOURCE_FLAGS_NONE;
#ifdef _DEBUG
    resource.name[0] = L'\0';
#endif
    return resource;
}