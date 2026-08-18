#pragma once

#include "sr/sr_api.h"

#ifdef __cplusplus
extern "C" {
    #endif

    /**
     * Extra string parameter containing the absolute path to AMD's signed
     * amd_fidelityfx_upscaler_dx12.dll. If omitted, the provider attempts to
     * load that filename through the process' standard DLL search path.
     */
    #define SR_FFX_API_DLL_PATH_PARAM "ffxApiDllPath"

    SR_API SRUpscaleContextCallbacks srGetFfxApiUpscaleCallbacks();

    #ifdef __cplusplus
}
#endif