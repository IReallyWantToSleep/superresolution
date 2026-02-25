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
        fsrContexDesc.flags = 0;
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEBUG)
        {
            fsrContexDesc.flags |= FFX_FSR2_ENABLE_DEBUG_CHECKING;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE)
        {
            fsrContexDesc.flags |= FFX_FSR2_ENABLE_AUTO_EXPOSURE;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEPTH_INVERTED)
        {
            fsrContexDesc.flags |= FFX_FSR2_ENABLE_DEPTH_INVERTED;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_MOTION_VECTORS_JITTERED)
        {
            fsrContexDesc.flags |= FFX_FSR2_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR)
        {
            fsrContexDesc.flags |= FFX_FSR2_ENABLE_HIGH_DYNAMIC_RANGE;
        }
        
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
        if (FfxErrorCode _rc = ffxGetInterfaceVK(ffxInterface, device, scratchBuffer, scratchBufferSize, 1); _rc != FFX_OK)
        {
            free(scratchBuffer);
            delete ffxInterface;
            return (SRReturnCode)SR_RETURN_CODE_ERROR;
        }

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
        if (!context || !context->userContext)
        {
            return SR_RETURN_CODE_NULL_POINTER;
        }

        SRFsr2PrivateData *privateData = (SRFsr2PrivateData *)context->userContext;
        
        if (!privateData->context)
        {
            if (privateData->scratchBuffer)
            {
                free(privateData->scratchBuffer);
                privateData->scratchBuffer = nullptr;
            }
            delete privateData->ffxInterface;
            delete privateData;
            context->userContext = nullptr;
            return SR_RETURN_CODE_ERROR;
        }

        FfxErrorCode errorCode = ffxFsr2ContextDestroy(privateData->context);
        
        if (errorCode != FFX_OK)
        {
            if (context->desc.messageCallback)
            {
                context->desc.messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR2 Context destroy failed");
            }
        }
        
        if (privateData->scratchBuffer)
        {
            free(privateData->scratchBuffer);
            privateData->scratchBuffer = nullptr;
        }
        
        delete privateData->context;
        privateData->context = nullptr;
        
        delete privateData->ffxInterface;
        privateData->ffxInterface = nullptr;
        
        delete privateData;
        context->userContext = nullptr;
        
        return (errorCode != FFX_OK) ? (SRReturnCode)SR_RETURN_CODE_ERROR : (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr2VkQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType)
    {
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
        {
            static SRQueryVersionResult outResult = {};
            outResult.versionId = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            outResult.versionNumber = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
        {
            FfxEffectMemoryUsage usage = {};
            ffxFsr2ContextGetGpuMemoryUsage(((SRFsr2PrivateData *)context->userContext)->context, &usage);
            static SRQueryGpuMemoryResult outResult = {};
            outResult.gpuMemory = usage.totalUsageInBytes;
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE:
        {
            static SRQueryAvailabilityResult outResult = {};
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
            dispatchDesc.color = {
                .resource = (void *)(desc->color.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->color.desc.format),
                        .width = desc->color.desc.width,
                        .height = desc->color.desc.height,
                        .depth = 1,
                        .mipCount = desc->color.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->color.desc.usage),
                    },
                     .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->depth.exist)
            dispatchDesc.depth = {
                .resource = (void *)(desc->depth.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->depth.desc.format),
                        .width = desc->depth.desc.width,
                        .height = desc->depth.desc.height,
                        .depth = 1,
                        .mipCount = desc->depth.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->depth.desc.usage),
                    },
                     .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->motionVectors.exist)
            dispatchDesc.motionVectors = {
                .resource = (void *)(desc->motionVectors.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->motionVectors.desc.format),
                        .width = desc->motionVectors.desc.width,
                        .height = desc->motionVectors.desc.height,
                        .depth = 1,
                        .mipCount = desc->motionVectors.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->motionVectors.desc.usage),
                    },
                     .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->exposure.exist)
            dispatchDesc.exposure = {
                .resource = (void *)(desc->exposure.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->exposure.desc.format),
                        .width = desc->exposure.desc.width,
                        .height = desc->exposure.desc.height,
                        .depth = 1,
                        .mipCount = desc->exposure.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->exposure.desc.usage),
                    },
                     .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->reactive.exist)
            dispatchDesc.reactive = {
                .resource = (void *)(desc->reactive.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->reactive.desc.format),
                        .width = desc->reactive.desc.width,
                        .height = desc->reactive.desc.height,
                        .depth = 1,
                        .mipCount = desc->reactive.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->reactive.desc.usage),
                    },
                     .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->transparencyAndComposition.exist)
            dispatchDesc.transparencyAndComposition = {
                .resource = (void *)(desc->transparencyAndComposition.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->transparencyAndComposition.desc.format),
                        .width = desc->transparencyAndComposition.desc.width,
                        .height = desc->transparencyAndComposition.desc.height,
                        .depth = 1,
                        .mipCount = desc->transparencyAndComposition.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->transparencyAndComposition.desc.usage),
                    },
                    .state = FFX_RESOURCE_STATE_GENERIC_READ
            };
        if (desc->output.exist)
            dispatchDesc.output = {
                .resource = (void *)(desc->output.handle),
                .description =
                    {
                        .type = FFX_RESOURCE_TYPE_TEXTURE2D,
                        .format = srTextureFormatToFfxSurfaceFormat(desc->output.desc.format),
                        .width = desc->output.desc.width,
                        .height = desc->output.desc.height,
                        .depth = 1,
                        .mipCount = desc->output.desc.mipmapCount,
                        .flags = FFX_RESOURCE_FLAGS_NONE,
                        .usage = srTextureResourceUsageToFfx(desc->output.desc.usage),
                    },
                .state = FFX_RESOURCE_STATE_UNORDERED_ACCESS
            };

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
