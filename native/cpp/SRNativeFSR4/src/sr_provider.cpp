#include "sr/fsr4/sr_provider.h"
#include "sr/fsr4/ffx_api_upscale.h"

#include <mutex>

static constexpr uint32_t PROVIDER_COUNT = 1;

static SRUpscaleProvider g_providers[PROVIDER_COUNT];
static std::once_flag g_initializeOnce;

static void ensureInitialized() {
    std::call_once(g_initializeOnce, [] {
        g_providers[0].providerId = SR_MODULES_FSR4_ID;
        g_providers[0].callbacks = srGetFfxApiUpscaleCallbacks();
    });
}

extern "C" {
    SR_API SRReturnCode srGetFfxFSR4UpscaleProviders(SRUpscaleProvider *outProvider) {
        ensureInitialized();
        outProvider[0] = g_providers[0];
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetFfxFSR4UpscaleProvidersCount(uint32_t *outCount) {
        ensureInitialized();
        *outCount = PROVIDER_COUNT;
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }
}