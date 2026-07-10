#include "sr/sr_modules.h"
#include "sr/streamline/sr_provider.h"

static SRUpscaleProvider g_providers[1];
static bool g_initialized = false;

static void ensureInitialized() {
    if (!g_initialized) {
        g_providers[0].providerId = SR_MODULES_DLSS_ID;
        g_providers[0].callbacks = srGetStreamlineDLSSUpscaleCallbacks();
        g_initialized = true;
    }
}

extern "C" {
    SR_API SRReturnCode srGetStreamlineUpscaleProviders(SRUpscaleProvider *outProvider) {
        if (!outProvider) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        ensureInitialized();
        outProvider[0] = g_providers[0];
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetStreamlineUpscaleProvidersCount(uint32_t *outCount) {
        if (!outCount) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        ensureInitialized();
        *outCount = 1;
        return SR_RETURN_CODE_OK;
    }
}
