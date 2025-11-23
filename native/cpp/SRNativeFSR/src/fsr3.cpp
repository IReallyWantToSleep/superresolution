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

    SR_API SRReturnCode srFfxFsr3InitUpscaleContext(SRUpscaleContext *context)
    {
        const SRCreateUpscaleContextDesc *desc = &context->desc;
        SRFsr3PrivateData *privateData = (SRFsr3PrivateData *)context->userContext;

        FfxFsr3UpscalerContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR3UPSCALER_ENABLE_DEBUG_CHECKING;
        fsrContexDesc.backendInterface = *(privateData->ffxInterface);
        fsrContexDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        fsrContexDesc.maxUpscaleSize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.fpMessage = desc->messageCallback ? reinterpret_cast<FfxFsr3UpscalerMessage>(desc->messageCallback) : nullptr;

        FfxErrorCode code = ffxFsr3UpscalerContextCreate(privateData->context, &fsrContexDesc);
        if (code != FFX_OK)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR3 Context init failed");
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(code).c_str());
            }
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3CreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR3 only supports Vulkan");
            }
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }

        VkDeviceContext deviceContext = {
            (VkDevice)(desc->renderDeviceInfo.vulkan.device),
            (VkPhysicalDevice)(desc->renderDeviceInfo.vulkan.physicalDevice),
            (PFN_vkGetDeviceProcAddr)(desc->renderDeviceInfo.vulkan.deviceProcAddr),
        };
        FfxDevice device = ffxGetDeviceVK(&deviceContext);
        size_t scratchBufferSize = ffxGetScratchMemorySizeVK((VkPhysicalDevice)(desc->renderDeviceInfo.vulkan.physicalDevice), 1);
        void *scratchBuffer = calloc(1, scratchBufferSize);
        FfxInterface *ffxInterface = new FfxInterface();
        SRFSR_CHECK(ffxGetInterfaceVK(ffxInterface, device, scratchBuffer, scratchBufferSize, 1));

        FfxFsr3UpscalerContext *fsr3Context = new FfxFsr3UpscalerContext();

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

    SR_API SRReturnCode srFfxFsr3QueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType)
    {
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
        {
            SRQueryVersionResult outResult = {};
            outResult.versionId = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR, FFX_FSR3UPSCALER_VERSION_MINOR, FFX_FSR3UPSCALER_VERSION_PATCH);
            outResult.versionNumber = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR, FFX_FSR3UPSCALER_VERSION_MINOR, FFX_FSR3UPSCALER_VERSION_PATCH);
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
        {
            FfxEffectMemoryUsage usage = {};
            ffxFsr3UpscalerContextGetGpuMemoryUsage(((SRFsr3PrivateData *)context->userContext)->context, &usage);
            SRQueryGpuMemoryResult outResult = {};
            outResult.gpuMemory = usage.totalUsageInBytes;
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE:
        {
            SRQueryAvailabilityResult outResult = {};
            outResult.isAvailable = true;
            result->data = &outResult;
            break;
        }
        default:
            break;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3DispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        FfxFsr3UpscalerContext *fsr3Context = ((SRFsr3PrivateData *)context->userContext)->context;

        FfxFsr3UpscalerDispatchDescription dispatchDesc = {};
        dispatchDesc.commandList = ffxGetCommandListVK(desc->commandList.apiCommandBuffer.vulkan.commandBuffer);

        if (desc->color.exist)
            dispatchDesc.color = srTextureResourceToFfxResource(&desc->color);
        if (desc->depth.exist)
            dispatchDesc.depth = srTextureResourceToFfxResource(&desc->depth);
        if (desc->motionVectors.exist)
            dispatchDesc.motionVectors = srTextureResourceToFfxResource(&desc->motionVectors);
        if (desc->exposure.exist)
            dispatchDesc.exposure = srTextureResourceToFfxResource(&desc->exposure);
        if (desc->reactive.exist)
            dispatchDesc.reactive = srTextureResourceToFfxResource(&desc->reactive);
        if (desc->transparencyAndComposition.exist)
            dispatchDesc.transparencyAndComposition = srTextureResourceToFfxResource(&desc->transparencyAndComposition);
        if (desc->output.exist)
            dispatchDesc.output = srTextureResourceToFfxResource(&desc->output);
        //return (SRReturnCode)SR_RETURN_CODE_OK;
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
            .pInit = (SRInitFunc)srFfxFsr3InitUpscaleContext,
            .pDestroy = (SRDestroyFunc)srFfxFsr3DestroyUpscaleContext,
            .pQuery = (SRQueryFunc)srFfxFsr3QueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc)srFfxFsr3DispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
