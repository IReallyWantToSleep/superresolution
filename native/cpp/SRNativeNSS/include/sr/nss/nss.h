#pragma once

#include "sr/sr_api.h"

#include <ffx_api/ffx_api_types.h>

FfxApiResource srTextureResourceToFfxResource(const SRTextureResource *srTex);

FfxApiResource srTextureResourceToFfxOutputResource(const SRTextureResource *srTex);

uint32_t srTextureResourceUsageToFfx(SRResourceUsage usage);

uint32_t srTextureResourceStateToFfx(SRResourceStates state, uint32_t fallbackState);

uint32_t srTextureFormatToFfxSurfaceFormat(SRTextureFormat format);

#ifdef __cplusplus
extern "C" {
#endif

SR_API SRUpscaleContextCallbacks srGetArmNSSUpscaleCallbacks();

SR_API SRReturnCode srArmNSSVkCreateUpscaleContext(
    SRUpscaleContext *context,
    const SRCreateUpscaleContextDesc *desc);

SR_API SRReturnCode srArmNSSVkInitUpscaleContext(SRUpscaleContext *context);

SR_API SRReturnCode srArmNSSVkDestroyUpscaleContext(SRUpscaleContext *context);

SR_API SRReturnCode srArmNSSVkQueryUpscale(
    SRUpscaleContext *context,
    SRUpscaleContextQueryResult *result,
    SRUpscaleContextQueryType queryType);

SR_API SRReturnCode srArmNSSVkDispatchUpscale(
    SRUpscaleContext *context,
    const SRDispatchUpscaleDesc *desc);

SR_API SRReturnCode srArmNSSVkShutdown();

#ifdef __cplusplus
}
#endif
