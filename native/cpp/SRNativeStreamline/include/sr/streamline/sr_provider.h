#pragma once

#include "sr/streamline/streamline.h"

#ifdef __cplusplus
extern "C" {
#endif

SR_API SRReturnCode srGetStreamlineUpscaleProviders(SRUpscaleProvider *outProvider);
SR_API SRReturnCode srGetStreamlineUpscaleProvidersCount(uint32_t *outCount);

#ifdef __cplusplus
}
#endif
