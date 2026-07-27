#pragma once
#include "sr/sr_api.h"
#include "sr/sr_modules.h"

extern "C" {
    SR_API SRReturnCode srGetFfxFSRUpscaleProviders(SRUpscaleProvider * outProvider);
    SR_API SRReturnCode srGetFfxFSRUpscaleProvidersCount(uint32_t * outCount);
}
