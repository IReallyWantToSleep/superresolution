#include "sr/sr_api.h"
#include "sr/fsr/fsr3.h"
#include "FidelityFX/host/backends/vk/ffx_vk.h"
#include "FidelityFX/host/ffx_fsr3upscaler.h"
#include <cstring>
#include <cstdlib>
#include "sr/fsr/sr_provider.h"
struct SRFsr3PrivateData
{
    FfxInterface *ffxInterface;
    FfxFsr3UpscalerContext *context;
    void *scratchBuffer;
};
#ifdef __cplusplus
extern "C"
{
#endif

    SR_API SRReturnCode srFfxFsr3CreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        VkDeviceContext deviceContext = {
            (VkDevice)(desc->device),
            (VkPhysicalDevice)(desc->phyDevice),
            (PFN_vkGetDeviceProcAddr)(desc->deviceProcAddr),
        };
        FfxDevice device = ffxGetDeviceVK(&deviceContext);
        size_t scratchBufferSize = ffxGetScratchMemorySizeVK((VkPhysicalDevice)(desc->phyDevice), 1);
        void *scratchBuffer = calloc(1, scratchBufferSize);
        FfxInterface *ffxInterface = new FfxInterface();
        SRFSR_CHECK(ffxGetInterfaceVK(ffxInterface, device, scratchBuffer, scratchBufferSize, 1));
        FfxFsr3UpscalerContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR3UPSCALER_ENABLE_DEBUG_CHECKING;
        fsrContexDesc.backendInterface = *ffxInterface;
        fsrContexDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        fsrContexDesc.maxUpscaleSize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.fpMessage = desc->messageCallback ? reinterpret_cast<FfxFsr3UpscalerMessage>(desc->messageCallback) : nullptr;
        FfxFsr3UpscalerContext *fsr3Context = new FfxFsr3UpscalerContext();
        FfxErrorCode code = ffxFsr3UpscalerContextCreate(fsr3Context, &fsrContexDesc);
        if (code != FFX_OK)
        {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR3 Context create failed");
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(code).c_str());
            free(scratchBuffer);
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
        }
        SRFsr3PrivateData *privateData = new SRFsr3PrivateData();
        privateData->context = fsr3Context;
        privateData->ffxInterface = ffxInterface;
        privateData->scratchBuffer = scratchBuffer;

        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        context->userContext = privateData;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3DestroyUpscaleContext(SRUpscaleContext *context)
    {
        SRFsr3PrivateData *privateData = (SRFsr3PrivateData *)context->userContext;
        ffxFsr3UpscalerContextDestroy(privateData->context);
        free(privateData->scratchBuffer);
        delete privateData->context;
        // delete privateData->interface;
        // delete privateData;
        context->userContext = nullptr;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3QueryUpscale(SRUpscaleContextQueryResult *result, SRUpscaleContext *context, SRUpscaleContextQueryType queryType)
    {
        SRUpscaleContextQueryResult *outResult = result;
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionId = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR, FFX_FSR3UPSCALER_VERSION_MINOR, FFX_FSR3UPSCALER_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionNumber = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR, FFX_FSR3UPSCALER_VERSION_MINOR, FFX_FSR3UPSCALER_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionName = const_cast<char *>("3.1.4");
            break;
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
            // FSR3不支持
            ((SRUpscaleContextQueryGpuMemoryInfoResult *)outResult)->gpuMemory = 0;
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
            break;

        default:
            break;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3DispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        FfxFsr3UpscalerContext *fsr3Context = ((SRFsr3PrivateData *)context->userContext)->context;

        FfxFsr3UpscalerDispatchDescription dispatchDesc = *new FfxFsr3UpscalerDispatchDescription();
        dispatchDesc.commandList = ffxGetCommandListVK((VkCommandBuffer)(desc->commandList));

        if (desc->color.exist)
            dispatchDesc.color = SRTextureResourceToFfxResource(&desc->color);
        if (desc->depth.exist)
            dispatchDesc.depth = SRTextureResourceToFfxResource(&desc->depth);
        if (desc->motionVectors.exist)
            dispatchDesc.motionVectors = SRTextureResourceToFfxResource(&desc->motionVectors);
        if (desc->exposure.exist)
            dispatchDesc.exposure = SRTextureResourceToFfxResource(&desc->exposure);
        if (desc->reactive.exist)
            dispatchDesc.reactive = SRTextureResourceToFfxResource(&desc->reactive);
        if (desc->transparencyAndComposition.exist)
            dispatchDesc.transparencyAndComposition = SRTextureResourceToFfxResource(&desc->transparencyAndComposition);
        if (desc->output.exist)
            dispatchDesc.output = SRTextureResourceToFfxResource(&desc->output);

        dispatchDesc.jitterOffset = {desc->jitterOffset.x, desc->jitterOffset.y};
        dispatchDesc.motionVectorScale = {desc->motionVectorScale.x, desc->motionVectorScale.y};
        dispatchDesc.renderSize = {desc->renderSize.x, desc->renderSize.y};

        dispatchDesc.enableSharpening = desc->enableSharpening;
        dispatchDesc.sharpness = desc->sharpness;
        dispatchDesc.frameTimeDelta = desc->frameTimeDelta;
        dispatchDesc.preExposure = desc->preExposure;
        dispatchDesc.reset = desc->reset;
        dispatchDesc.cameraNear = desc->cameraNear;
        dispatchDesc.cameraFar = desc->cameraFar;
        dispatchDesc.cameraFovAngleVertical = desc->cameraFovAngleVertical;
        dispatchDesc.viewSpaceToMetersFactor = desc->viewSpaceToMetersFactor;
        SRFSR_CHECK(ffxFsr3UpscalerContextDispatch(fsr3Context, &dispatchDesc));
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
    SR_API SRUpscaleContextCallbacks srGetFfxFSR3UpscaleCallbacks()
    {
        static SRUpscaleContextCallbacks callbacks = {
              .pCreate = (SRCreateFunc)srFfxFsr3CreateUpscaleContext,
              .pDestroy = (SRDestroyFunc)srFfxFsr3DestroyUpscaleContext,
              .pQuery = (SRQueryFunc)srFfxFsr3QueryUpscale,
              .pDispatchUpscale = (SRDispatchUpscaleFunc)srFfxFsr3DispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
