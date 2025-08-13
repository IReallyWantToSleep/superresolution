#include "sr/sr_api.h"
#include "sr/fsr/fsr2ogl.h"
#include "ffx-fsr2-api/ffx_fsr2.h"
#include "ffx-fsr2-api/gl/ffx_fsr2_gl.h"
#include "ffx-fsr2-api/ffx_fsr2_interface.h"

#include <cstring>
#include <cstdlib>
#include "sr/fsr/sr_provider.h"
struct SRFsr2PrivateData
{
    FfxFsr2Interface *ffxInterface;
    FfxFsr2Context *context;
    void *scratchBuffer;
};
#ifdef __cplusplus
extern "C"
{
#endif
    ffx_glGetProcAddress MakeAdapter(SRGetFuncAddress func)
    {
        static SRGetFuncAddress stored = nullptr;
        stored = func;
        return [](const char *name) -> ffx_glProc
        {
            return (ffx_glProc)stored(NULL, name);
        };
    }

    SR_API SRReturnCode srFfxFsr2OglCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        size_t scratchBufferSize = ffxFsr2GetScratchMemorySizeGL();
        void *scratchBuffer = calloc(1, scratchBufferSize);
        FfxFsr2Interface ffxInterface = *new FfxFsr2Interface();
        SRFSR_CHECK(ffxFsr2GetInterfaceGL(&ffxInterface, scratchBuffer, scratchBufferSize, MakeAdapter(desc->deviceProcAddr)));
        FfxFsr2ContextDescription fsrContexDesc = {};
        fsrContexDesc.flags = FFX_FSR2_ENABLE_DEBUG_CHECKING | FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST | desc->flags;
        fsrContexDesc.callbacks = ffxInterface;
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

    SR_API SRReturnCode srFfxFsr2OglDestroyUpscaleContext(SRUpscaleContext *context)
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

    SR_API SRReturnCode srFfxFsr2OglQueryUpscale(SRUpscaleContextQueryResult *result, SRUpscaleContext *context, SRUpscaleContextQueryType queryType)
    {
        SRUpscaleContextQueryResult *outResult = result;
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionId = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionNumber = SR_MAKE_VERSION(FFX_FSR2_VERSION_MAJOR, FFX_FSR2_VERSION_MINOR, FFX_FSR2_VERSION_PATCH);
            ((SRUpscaleContextQueryVersionInfoResult *)outResult)->versionName = const_cast<char *>("2.2.1");
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

    SR_API SRReturnCode srFfxFsr2OglDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        FfxFsr2Context *fsr2Context = ((SRFsr2PrivateData *)context->userContext)->context;

        FfxFsr2DispatchDescription dispatchDesc = *new FfxFsr2DispatchDescription();
        dispatchDesc.commandList = nullptr;

        if (desc->color.exist)
            dispatchDesc.color = SRTextureResourceToFfxResourceGL(&desc->color);
        if (desc->depth.exist)
            dispatchDesc.depth = SRTextureResourceToFfxResourceGL(&desc->depth);
        if (desc->motionVectors.exist)
            dispatchDesc.motionVectors = SRTextureResourceToFfxResourceGL(&desc->motionVectors);
        if (desc->exposure.exist)
            dispatchDesc.exposure = SRTextureResourceToFfxResourceGL(&desc->exposure);
        if (desc->reactive.exist)
            dispatchDesc.reactive = SRTextureResourceToFfxResourceGL(&desc->reactive);
        if (desc->transparencyAndComposition.exist)
            dispatchDesc.transparencyAndComposition = SRTextureResourceToFfxResourceGL(&desc->transparencyAndComposition);
        if (desc->output.exist)
            dispatchDesc.output = SRTextureResourceToFfxResourceGL(&desc->output);

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
    SR_API SRUpscaleContextCallbacks srGetFfxFSR2OglUpscaleCallbacks()
    {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc)srFfxFsr2OglCreateUpscaleContext,
            .pDestroy = (SRDestroyFunc)srFfxFsr2OglDestroyUpscaleContext,
            .pQuery = (SRQueryFunc)srFfxFsr2OglQueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc)srFfxFsr2OglDispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
