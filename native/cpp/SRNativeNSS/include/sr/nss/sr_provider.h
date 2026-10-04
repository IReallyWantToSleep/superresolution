#pragma once
#include <vector>
#include "sr/sr_api.h"
#include "sr/sr_modules.h"

extern "C" {
    SR_API SRReturnCode srGetArmNSSUpscaleProviders(SRUpscaleProvider *outProvider);

    SR_API SRReturnCode srGetArmNSSUpscaleProvidersCount(uint32_t *outCount);
}
