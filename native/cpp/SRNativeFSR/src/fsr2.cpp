#include "sr/sr_api.h"
#include "sr/fsr/fsr2_internal.h"
#include <cstring>

#ifdef __cplusplus
extern "C" {
    #endif

    SR_API SRReturnCode
    srFfxFsr2CreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
        if (desc->renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srFfxFsr2VkCreateUpscaleContext(context, desc);
        } else {
            if (desc->messageCallback) {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR2 only supports Vulkan");
            }
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }
    }

    SR_API SRReturnCode srFfxFsr2InitUpscaleContext(SRUpscaleContext *context) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srFfxFsr2VkInitUpscaleContext(context);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srFfxFsr2DestroyUpscaleContext(SRUpscaleContext *context) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srFfxFsr2VkDestroyUpscaleContext(context);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srFfxFsr2QueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result,
                                              SRUpscaleContextQueryType queryType) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srFfxFsr2VkQueryUpscale(context, result, queryType);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srFfxFsr2DispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
        if (context->desc.renderApiType == SR_RENDER_API_TYPE_VULKAN) {
            return srFfxFsr2VkDispatchUpscale(context, desc);
        }
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    SR_API SRReturnCode srFfxFsr2Shutdown() {
        return srFfxFsr2VkShutdown();
    }

    SR_API SRUpscaleContextCallbacks srGetFfxFSR2UpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc) srFfxFsr2CreateUpscaleContext,
            .pInit = (SRInitFunc) srFfxFsr2InitUpscaleContext,
            .pDestroy = (SRDestroyFunc) srFfxFsr2DestroyUpscaleContext,
            .pQuery = (SRQueryFunc) srFfxFsr2QueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc) srFfxFsr2DispatchUpscale,
            .pShutdown = (SRShutdownFunc) srFfxFsr2Shutdown,
        };
        return callbacks;
    }

    #ifdef __cplusplus
}
#endif