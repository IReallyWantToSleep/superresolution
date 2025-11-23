#include "sr/sr_api.h"

#include "FidelityFX/host/ffx_fsr2.h"
#include "FidelityFX/host/backends/vk/ffx_vk.h"

#define SRFSR_CHECK(_expr)                         \
    if (FfxErrorCode _rc = (_expr); _rc != FFX_OK) \
    return (SRReturnCode)SR_RETURN_CODE_ERROR


FfxResource srTextureResourceToFfxResource(const SRTextureResource * srTex);