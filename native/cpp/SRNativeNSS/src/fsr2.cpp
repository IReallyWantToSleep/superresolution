#include "sr/sr_api.h"
#include "sr/nss/nss.h"
#include <cstring>

#ifdef __cplusplus
extern "C" {
    #endif

    SR_API SRReturnCode

    srArmNSSCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
        if (desc->renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srArmNSSVkCreateUpscaleContext(context, desc);
        }
        if (desc->messageCallback) {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"NSS only supports Vulkan");
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srArmNSSInitUpscaleContext(SRUpscaleContext *context) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srArmNSSVkInitUpscaleContext(context);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srArmNSSDestroyUpscaleContext(SRUpscaleContext *context) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srArmNSSVkDestroyUpscaleContext(context);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srArmNSSQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result,
                                              SRUpscaleContextQueryType queryType) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srArmNSSVkQueryUpscale(context, result, queryType);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srArmNSSDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srArmNSSVkDispatchUpscale(context, desc);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srArmNSSShutdown() {
        return srArmNSSVkShutdown();
    }

    SR_API SRUpscaleContextCallbacks srGetArmNSSUpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = static_cast<SRCreateFunc>(srArmNSSCreateUpscaleContext),
            .pInit = static_cast<SRInitFunc>(srArmNSSInitUpscaleContext),
            .pDestroy = static_cast<SRDestroyFunc>(srArmNSSDestroyUpscaleContext),
            .pQuery = reinterpret_cast<SRQueryFunc>(srArmNSSQueryUpscale),
            .pDispatchUpscale = static_cast<SRDispatchUpscaleFunc>(srArmNSSDispatchUpscale),
            .pShutdown = static_cast<SRShutdownFunc>(srArmNSSShutdown),
        };
        return callbacks;
    }

    #ifdef __cplusplus
}
#endif
