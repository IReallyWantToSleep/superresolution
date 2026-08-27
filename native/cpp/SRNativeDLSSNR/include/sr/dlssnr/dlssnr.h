#pragma once
#include "sr/sr_api.h"
#ifdef __cplusplus
extern "C" {
    #endif
    SR_API SRUpscaleContextCallbacks srGetDLSSNRUpscaleCallbacks();

    SR_API SRReturnCode srDLSSNRLoadFunctionsFromDll(const char *dllPath, SRMessageCallback messageCallback);
    #ifdef __cplusplus
}
#endif
