#pragma once
#include <vector>
#include "sr/sr_api.h"
#include "sr/sr_modules.h"
#include "xess.h"
extern "C" {
    SR_API SRReturnCode srGetXeSSUpscaleProviders(SRUpscaleProvider *outProvider);
    SR_API SRReturnCode srGetXeSSUpscaleProvidersCount(uint32_t *outCount);
}