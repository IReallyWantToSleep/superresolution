#pragma once
#include <vector>
#include "sr/sr_api.h"
#include "sr/sr_modules.h"
#include "dlss.h"
extern "C" {
    SR_API SRReturnCode srGetDLSSUpscaleProviders(SRUpscaleProvider *outProvider);
    SR_API SRReturnCode srGetDLSSUpscaleProvidersCount(uint32_t *outCount);
}