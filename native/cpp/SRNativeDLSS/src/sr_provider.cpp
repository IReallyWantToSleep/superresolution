#include "sr/dlss/sr_provider.h"

static SRUpscaleProvider g_providers[1];
static bool g_initialized = false;

static void ensureInitialized() {
    if (!g_initialized) {
        g_providers[0].providerId = SR_MODULES_DLSS_ID;
        g_providers[0].callbacks = srGetDLSSUpscaleCallbacks();
    }
}

extern "C" {
    SR_API SRReturnCode srGetDLSSUpscaleProviders(SRUpscaleProvider *outProvider) {
        ensureInitialized();
        outProvider[0] = g_providers[0];
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetDLSSUpscaleProvidersCount(uint32_t *outCount) {
        ensureInitialized();
        *outCount = 1;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
}