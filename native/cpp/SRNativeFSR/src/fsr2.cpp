#include "sr/sr_api.h"
#include "sr/fsr/fsr2.h"
#include "FidelityFX/host/backends/vk/ffx_vk.h"
#include "FidelityFX/host/ffx_fsr2.h"
#include <cstring>
#include <cstdlib>
#include "sr/fsr/sr_provider.h"
struct SRFsr2PrivateData
{
    FfxInterface *ffxInterface;
    FfxFsr2Context *context;
    void *scratchBuffer;
};
#ifdef __cplusplus
extern "C"
{
#endif

    SR_API SRReturnCode srFfxFsr2CreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        VkDeviceContext deviceContext = {
            (VkDevice)(desc->device),
            (VkPhysicalDevice)(desc->phyDevice),
            (PFN_vkGetDeviceProcAddr)(desc->deviceProcAddr),
        };
        FfxDevice device = ffxGetDeviceVK(&deviceContext);
        size_t scratchBufferSize = ffxGetScratchMemorySizeVK((VkPhysicalDevice)(desc->phyDevice), 1);
        void *scratchBuffer = calloc(1, scratchBufferSize);
        FfxInterface ffxInterface = *new FfxInterface();
        SRFSR_CHECK(ffxGetInterfaceVK(&ffxInterface, device, scratchBuffer, scratchBufferSize, 1));
        FfxFsr2ContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR2_ENABLE_DEBUG_CHECKING;
        fsrContexDesc.backendInterface = ffxInterface;
        fsrContexDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        fsrContexDesc.displaySize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.fpMessage = desc->messageCallback ? reinterpret_cast<FfxFsr2Message>(desc->messageCallback) : nullptr;
        FfxFsr2Context *fsr2Context = new FfxFsr2Context();
        FfxErrorCode code = ffxFsr2ContextCreate(fsr2Context, &fsrContexDesc);
        if (code != FFX_OK)
        {
            free(scratchBuffer);
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
        }
        SRFsr2PrivateData *privateData = new SRFsr2PrivateData();
        privateData->context = fsr2Context;
        privateData->ffxInterface = &ffxInterface;
        privateData->scratchBuffer = scratchBuffer;

        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        context->userContext = privateData;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2DestroyUpscaleContext(SRUpscaleContext *context)
    {
        SRFsr2PrivateData *privateData = (SRFsr2PrivateData *)context->userContext;
        ffxFsr2ContextDestroy(privateData->context);
        free(privateData->scratchBuffer);
        delete privateData->context;
        // delete privateData->interface;
        // delete privateData;
        context->userContext = nullptr;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2QueryUpscale(SRUpscaleContextQueryResult *result, SRUpscaleContext *context, SRUpscaleContextQueryType queryType)
    {
        SRUpscaleContextQueryResult *outResult = result;
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionId = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionNumber = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionName = const_cast<char *>("2.3.3");
            break;
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
            // FSR2不支持
            ((SRUpscaleContextQueryGpuMemoryInfoResult *)outResult)->gpuMemory = 0;
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
            break;

        default:
            break;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2DispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        FfxFsr2Context *fsr2Context = ((SRFsr2PrivateData *)context->userContext)->context;

        FfxFsr2DispatchDescription dispatchDesc = *new FfxFsr2DispatchDescription();
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
        SRFSR_CHECK(ffxFsr2ContextDispatch(fsr2Context, &dispatchDesc));
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
    SR_API SRUpscaleContextCallbacks srGetFfxFSR2UpscaleCallbacks()
    {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc)srFfxFsr2CreateUpscaleContext,
            .pDestroy = (SRDestroyFunc)srFfxFsr2DestroyUpscaleContext,
            .pQuery = (SRQueryFunc)srFfxFsr2QueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc)srFfxFsr2DispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
