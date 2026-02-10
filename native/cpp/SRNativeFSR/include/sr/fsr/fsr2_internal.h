#pragma once
#include "sr/sr_api.h"

#ifdef __cplusplus
extern "C"
{
#endif
    SR_API SRReturnCode srFfxFsr2VkCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc);
    SR_API SRReturnCode srFfxFsr2VkInitUpscaleContext(SRUpscaleContext *context);
    SR_API SRReturnCode srFfxFsr2VkDestroyUpscaleContext(SRUpscaleContext *context);
    SR_API SRReturnCode srFfxFsr2VkQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType);
    SR_API SRReturnCode srFfxFsr2VkDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc);

#ifdef __cplusplus
}
#endif
