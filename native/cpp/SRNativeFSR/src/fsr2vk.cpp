#include "sr/sr_api.h"
#include "sr/fsr/fsr2.h"
#include "sr/fsr/fsr2_internal.h"
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

    SR_API SRReturnCode srFfxFsr2VkInitUpscaleContext(SRUpscaleContext *context)
    {
        const SRCreateUpscaleContextDesc *desc = &context->desc;
        SRFsr2PrivateData *privateData = (SRFsr2PrivateData *)context->userContext;

        FfxFsr2ContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR2_ENABLE_DEBUG_CHECKING;
        fsrContexDesc.backendInterface = *(privateData->ffxInterface);
        fsrContexDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        fsrContexDesc.displaySize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.fpMessage = desc->messageCallback ? reinterpret_cast<FfxFsr2Message>(desc->messageCallback) : nullptr;

        FfxErrorCode code = ffxFsr2ContextCreate(privateData->context, &fsrContexDesc);
        if (code != FFX_OK)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR2 Context init failed");
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(code).c_str());
            }
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2VkCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR2 Vulkan only supports Vulkan");
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

        FfxFsr2Context *fsr2Context = new FfxFsr2Context();

        SRFsr2PrivateData *privateData = new SRFsr2PrivateData();
        privateData->context = fsr2Context;
        privateData->ffxInterface = ffxInterface;
        privateData->scratchBuffer = scratchBuffer;

        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        context->userContext = privateData;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2VkDestroyUpscaleContext(SRUpscaleContext *context)
    {
        SRFsr2PrivateData *privateData = (SRFsr2PrivateData *)context->userContext;
        ffxFsr2ContextDestroy(privateData->context);
        free(privateData->scratchBuffer);
        delete privateData->context;
        delete privateData->ffxInterface;
        delete privateData;
        context->userContext = nullptr;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2VkQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType)
    {
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
        {
            SRQueryVersionResult outResult = {};
            outResult.versionId = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            outResult.versionNumber = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
        {
            FfxEffectMemoryUsage usage = {};
            ffxFsr2ContextGetGpuMemoryUsage(((SRFsr2PrivateData *)context->userContext)->context, &usage);
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

    SR_API SRReturnCode srFfxFsr2VkDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        FfxFsr2Context *fsr2Context = ((SRFsr2PrivateData *)context->userContext)->context;

        FfxFsr2DispatchDescription dispatchDesc = {};
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

#ifdef __cplusplus
}
#endif
