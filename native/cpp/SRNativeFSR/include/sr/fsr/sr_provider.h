#include <vector>
#include "sr/sr_api.h"
#include "sr/sr_modules.h"
#include "fsr2.h"

static SRUpscaleProvider g_providers[1];
static bool g_initialized = false;

static void ensureInitialized()
{
    if (!g_initialized)
    {
        g_providers[0].providerId = SR_MODULES_FSR2_ID;
        g_providers[0].callbacks = srGetFfxFSR2UpscaleCallbacks();
        g_initialized = true;
    }
}

extern "C"
{
    SR_API SRReturnCode srGetFfxFSRUpscaleProviders(SRUpscaleProvider *outProvider)
    {
        ensureInitialized();
        outProvider[0] = g_providers[0];
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetFfxFSRUpscaleProvidersCount(uint32_t *outCount)
    {
        ensureInitialized();
        *outCount = 1;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
}
