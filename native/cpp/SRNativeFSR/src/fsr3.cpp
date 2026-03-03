#include "sr/sr_api.h"
#include "sr/fsr/fsr3.h"
#include "FidelityFX/host/backends/vk/ffx_vk.h"
#include "FidelityFX/host/ffx_fsr3.h"
#include <cstring>
#include <cstdlib>
#include <utility>
#include "sr/fsr/sr_provider.h"

struct SRFsr3PrivateData {
    FfxInterface *ffxInterface;
    FfxFsr3Context *context;
    void *scratchBuffer;
    uint64_t frameIndex;
};
#ifdef __cplusplus
extern "C" {
    #endif

    SR_API SRReturnCode srFfxFsr3InitUpscaleContext(SRUpscaleContext *context) {
        const SRCreateUpscaleContextDesc *desc = &context->desc;
        SRFsr3PrivateData *privateData = (SRFsr3PrivateData *) context->userContext;

        FfxFsr3ContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR3_ENABLE_UPSCALING_ONLY;
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEBUG) {
            fsrContexDesc.flags |= FFX_FSR3_ENABLE_DEBUG_CHECKING;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE) {
            fsrContexDesc.flags |= FFX_FSR3_ENABLE_AUTO_EXPOSURE;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEPTH_INVERTED) {
            fsrContexDesc.flags |= FFX_FSR3_ENABLE_DEPTH_INVERTED;
        }
        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_MOTION_VECTORS_JITTERED) {
            fsrContexDesc.flags |= FFX_FSR3_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION;
        }

        if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR) {
            fsrContexDesc.flags |= FFX_FSR3_ENABLE_HIGH_DYNAMIC_RANGE;
        }
        fsrContexDesc.backendInterfaceUpscaling = *(privateData->ffxInterface);
        fsrContexDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        fsrContexDesc.maxUpscaleSize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.displaySize = {desc->upscaledSize.x, desc->upscaledSize.y};
        fsrContexDesc.backBufferFormat = FFX_SURFACE_FORMAT_R16G16B16A16_FLOAT;
        fsrContexDesc.fpMessage = desc->messageCallback
                                      ? reinterpret_cast<FfxFsr3UpscalerMessage>(desc->messageCallback)
                                      : nullptr;

        FfxErrorCode code = ffxFsr3ContextCreate(privateData->context, &fsrContexDesc);
        if (code != FFX_OK) {
            if (desc->messageCallback) {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR3 Context init failed");
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(code).c_str());
            }
            return (SRReturnCode) SR_RETURN_CODE_ERROR;
        }
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode
    srFfxFsr3CreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN) {
            if (desc->messageCallback) {
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
        size_t scratchBufferSize = ffxGetScratchMemorySizeVK(
            (VkPhysicalDevice)(desc->renderDeviceInfo.vulkan.physicalDevice), 1);
        void *scratchBuffer = malloc(scratchBufferSize);
        memset(scratchBuffer, 0, scratchBufferSize);
        FfxInterface *ffxInterface = new FfxInterface();
        if (FfxErrorCode _rc = ffxGetInterfaceVK(ffxInterface, device, scratchBuffer, scratchBufferSize, 1);
            _rc != FFX_OK) {
            free(scratchBuffer);
            delete ffxInterface;
            return (SRReturnCode) SR_RETURN_CODE_ERROR;
        }

        FfxFsr3Context *fsr3Context = new FfxFsr3Context();

        SRFsr3PrivateData *privateData = new SRFsr3PrivateData();
        privateData->context = fsr3Context;
        privateData->ffxInterface = ffxInterface;
        privateData->scratchBuffer = scratchBuffer;
        privateData->frameIndex = 0;

        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        context->userContext = privateData;
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3DestroyUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }

        SRFsr3PrivateData *privateData = (SRFsr3PrivateData *) context->userContext;

        if (!privateData->context) {
            if (privateData->scratchBuffer) {
                free(privateData->scratchBuffer);
                privateData->scratchBuffer = nullptr;
            }
            delete privateData->ffxInterface;
            delete privateData;
            context->userContext = nullptr;
            return SR_RETURN_CODE_ERROR;
        }

        FfxErrorCode errorCode = ffxFsr3ContextDestroy(privateData->context);

        if (errorCode != FFX_OK) {
            if (context->desc.messageCallback) {
                context->desc.messageCallback(SR_MESSAGE_TYPE_ERROR, L"FSR3 Context destroy failed");
                context->desc.messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(errorCode).c_str());
            }
        }

        if (privateData->scratchBuffer) {
            free(privateData->scratchBuffer);
            privateData->scratchBuffer = nullptr;
        }

        delete privateData->context;
        privateData->context = nullptr;

        delete privateData->ffxInterface;
        privateData->ffxInterface = nullptr;

        delete privateData;
        context->userContext = nullptr;

        return (errorCode != FFX_OK) ? (SRReturnCode) SR_RETURN_CODE_ERROR : (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3QueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result,
                                              SRUpscaleContextQueryType queryType) {
        switch (queryType) {
            case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO: {
                static SRQueryVersionResult outResult = {};
                outResult.versionId = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR, FFX_FSR3UPSCALER_VERSION_MINOR,
                                                      FFX_FSR3UPSCALER_VERSION_PATCH);
                outResult.versionNumber = SR_MAKE_VERSION(FFX_FSR3UPSCALER_VERSION_MAJOR,
                                                          FFX_FSR3UPSCALER_VERSION_MINOR,
                                                          FFX_FSR3UPSCALER_VERSION_PATCH);
                result->data = &outResult;
                break;
            }
            case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO: {
                FfxEffectMemoryUsage usage = {};
                ffxFsr3ContextGetGpuMemoryUsage(((SRFsr3PrivateData *) context->userContext)->context, &usage, nullptr,
                                                nullptr);
                static SRQueryGpuMemoryResult outResult = {};
                outResult.gpuMemory = usage.totalUsageInBytes;
                result->data = &outResult;
                break;
            }
            case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE: {
                static SRQueryAvailabilityResult outResult = {};
                outResult.isAvailable = true;
                result->data = &outResult;
                break;
            }
            default:
                break;
        }
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3DispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
        FfxFsr3Context *fsr3Context = ((SRFsr3PrivateData *) context->userContext)->context;

        FfxFsr3DispatchUpscaleDescription dispatchDesc = {};
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
            dispatchDesc.upscaleOutput = srTextureResourceToFfxOutputResource(&desc->output);
        dispatchDesc.jitterOffset = {desc->jitterOffset.x, desc->jitterOffset.y};
        dispatchDesc.motionVectorScale = {desc->motionVectorScale.x, desc->motionVectorScale.y};
        dispatchDesc.renderSize = {desc->renderSize.x, desc->renderSize.y};
        dispatchDesc.upscaleSize = {desc->upscaleSize.x, desc->upscaleSize.y};
        dispatchDesc.enableSharpening = desc->enableSharpening;
        dispatchDesc.sharpness = desc->sharpness;
        dispatchDesc.frameTimeDelta = desc->frameTimeDelta;
        dispatchDesc.preExposure = desc->preExposure;
        dispatchDesc.reset = desc->reset;
        dispatchDesc.cameraNear = desc->cameraNear;
        dispatchDesc.cameraFar = desc->cameraFar;
        dispatchDesc.cameraFovAngleVertical = desc->cameraFovAngleVertical;
        dispatchDesc.viewSpaceToMetersFactor = desc->viewSpaceToMetersFactor;
        dispatchDesc.flags = desc->flags;
        dispatchDesc.frameID = ((SRFsr3PrivateData *) context->userContext)->frameIndex++;
        SRFSR_CHECK(ffxFsr3ContextDispatchUpscale(fsr3Context, &dispatchDesc));
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxFsr3Shutdown() {
        return (SRReturnCode) SR_RETURN_CODE_OK;
    }

    SR_API SRUpscaleContextCallbacks srGetFfxFSR3UpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc) srFfxFsr3CreateUpscaleContext,
            .pInit = (SRInitFunc) srFfxFsr3InitUpscaleContext,
            .pDestroy = (SRDestroyFunc) srFfxFsr3DestroyUpscaleContext,
            .pQuery = (SRQueryFunc) srFfxFsr3QueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc) srFfxFsr3DispatchUpscale,
            .pShutdown = (SRShutdownFunc) srFfxFsr3Shutdown,
        };
        return callbacks;
    }

    #ifdef __cplusplus
}
#endif