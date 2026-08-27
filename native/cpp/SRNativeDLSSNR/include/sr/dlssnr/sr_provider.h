#pragma once
#include "sr/sr_api.h"
#include "sr/sr_modules.h"
#include "dlssnr.h"

extern "C" {
    SR_API SRReturnCode srGetDLSSNRUpscaleProviders(SRUpscaleProvider *outProvider);

    SR_API SRReturnCode srGetDLSSNRUpscaleProvidersCount(uint32_t *outCount);
}
