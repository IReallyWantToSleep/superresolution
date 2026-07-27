#include "sr/fsr/sr_provider.h"
#if defined(ON_WIN64)
#include "sr/fsr/ffx_api_upscale.h"
#endif

#if defined(ON_WIN64)
static constexpr uint32_t PROVIDER_COUNT = 3;
#else
static constexpr uint32_t PROVIDER_COUNT = 2;
#endif

static SRUpscaleProvider g_providers[PROVIDER_COUNT];
static bool g_initialized = false;

static void ensureInitialized() {
    if (!g_initialized) {
        g_providers[0].providerId = SR_MODULES_FSR2_ID;
        g_providers[0].callbacks = srGetFfxFSR2UpscaleCallbacks();

        g_providers[1].providerId = SR_MODULES_FSR3_ID;
        g_providers[1].callbacks = srGetFfxFSR3UpscaleCallbacks();

        #if defined(ON_WIN64)
        g_providers[2].providerId = SR_MODULES_FFX_API_UPSCALE_ID;
        g_providers[2].callbacks = srGetFfxApiUpscaleCallbacks();
        #endif
        g_initialized = true;
    }
}

extern "C" {
    SR_API SRReturnCode srGetFfxFSRUpscaleProviders(SRUpscaleProvider *outProvider) {
        ensureInitialized();
        outProvider[0] = g_providers[0];
        outProvider[1] = g_providers[1];
        #if defined(ON_WIN64)
        outProvider[2] = g_providers[2];
        #endif
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetFfxFSRUpscaleProvidersCount(uint32_t *outCount) {
        ensureInitialized();
        *outCount = PROVIDER_COUNT;
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }
}
