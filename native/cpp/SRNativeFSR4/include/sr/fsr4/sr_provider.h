#pragma once
#include "sr/sr_api.h"
#include "sr/sr_modules.h"

extern "C" {
    SR_API SRReturnCode srGetFfxFSR4UpscaleProviders(SRUpscaleProvider * outProvider);
    SR_API SRReturnCode srGetFfxFSR4UpscaleProvidersCount(uint32_t * outCount);
}
