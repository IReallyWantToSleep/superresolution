#include "sr/sr_api.h"
#include "sr/xess/xess.h"
#include <cstring>
#include <cstdlib>
#include "sr/xess/sr_provider.h"
#include "XeSS/inc/xess/xess_vk.h"
struct SRXeSSPrivateData
{
    xess_context_handle_t xessContext;
    xess_coord_t renderSize;
    SRMessageCallback messageCallback;
    bool isAvailable;
};
#ifdef __cplusplus
extern "C"
{
#endif
    SR_API SRReturnCode srXeSSInitUpscaleContext(SRUpscaleContext *context)
    {
        SRXeSSPrivateData *privateData = (SRXeSSPrivateData *)context->userContext;
        xess_result_t status;
        const SRCreateUpscaleContextDesc *desc = &context->desc;
        float upscaleRatio = static_cast<float>(desc->upscaledSize.x) / static_cast<float>(desc->renderSize.x);
        xess_quality_settings_t quality_settings = XESS_QUALITY_SETTING_AA;
        if (upscaleRatio < 1.0f)
            upscaleRatio = 1.0f;
        //[0,1]
        if (upscaleRatio <= 1.0f)
            quality_settings = XESS_QUALITY_SETTING_AA;
        //(1,1.3]
        if (upscaleRatio > 1.0f && upscaleRatio <= 1.3f)
            quality_settings = XESS_QUALITY_SETTING_ULTRA_QUALITY_PLUS;
        //(1.3,1.5]
        if (upscaleRatio > 1.3f && upscaleRatio <= 1.5f)
            quality_settings = XESS_QUALITY_SETTING_ULTRA_QUALITY;
        //(1.5,1.7]
        if (upscaleRatio > 1.5f && upscaleRatio <= 1.7f)
            quality_settings = XESS_QUALITY_SETTING_QUALITY;
        //(1.7,2.0]
        if (upscaleRatio > 1.7f && upscaleRatio <= 2.0f)
            quality_settings = XESS_QUALITY_SETTING_BALANCED;
        //(2.0,2.3]
        if (upscaleRatio > 2.0f && upscaleRatio <= 2.3f)
            quality_settings = XESS_QUALITY_SETTING_PERFORMANCE;
        //(2.3,3.0]
        if (upscaleRatio > 2.3f)
            quality_settings = XESS_QUALITY_SETTING_ULTRA_PERFORMANCE;
        xess_2d_t upscale_size;
        upscale_size.x = desc->upscaledSize.x;
        upscale_size.y = desc->upscaledSize.y;
        xessGetInputResolution(
            privateData->xessContext,
            &upscale_size,
            quality_settings,
            &privateData->renderSize);

        xess_vk_init_params_t params = {
            {desc->upscaledSize.x, desc->upscaledSize.y},
            quality_settings,
            XESS_INIT_FLAG_LDR_INPUT_COLOR,
            0,
            0,
            nullptr,
            0,
            nullptr,
            0,
            NULL};
        /*
        status = xessVKBuildPipelines(privateData->xessContext, nullptr, true, XESS_INIT_FLAG_LDR_INPUT_COLOR);
        if (status != XESS_RESULT_SUCCESS)
        {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS build pipeline failed");
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(status).c_str());
            return SR_RETURN_CODE_ERROR;
        }
        else
        {
            desc->messageCallback(SR_MESSAGE_TYPE_INFO, L"XeSS build pipeline successful");
        }
        status = xessGetPipelineBuildStatus(privateData->xessContext);
        if (status != XESS_RESULT_SUCCESS)
        {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS build pipeline failed");
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(status).c_str());
            return SR_RETURN_CODE_ERROR;
        }
        else
        {
            desc->messageCallback(SR_MESSAGE_TYPE_INFO, L"XeSS build pipeline successful");
        }*/
        status = xessVKInit(privateData->xessContext, &params);
        if (status != XESS_RESULT_SUCCESS)
        {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS Context init failed");
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(status).c_str());
            return SR_RETURN_CODE_ERROR;
        }
        else
        {
            desc->messageCallback(SR_MESSAGE_TYPE_INFO, L"XeSS Context init successful");
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srXeSSCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS only supports Vulkan");
            }
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }

        ///////////////
        SRXeSSPrivateData *privateData = new SRXeSSPrivateData();
        auto status = xessVKCreateContext(
            (VkInstance)desc->renderDeviceInfo.vulkan.instance,
            (VkPhysicalDevice)desc->renderDeviceInfo.vulkan.physicalDevice,
            (VkDevice)desc->renderDeviceInfo.vulkan.device,
            &privateData->xessContext);
        if (status != XESS_RESULT_SUCCESS && status != XESS_RESULT_ERROR_UNSUPPORTED_DEVICE)
        {
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS Context create failed");
            desc->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(status).c_str());
            return SR_RETURN_CODE_ERROR;
        }
        else
        {
            desc->messageCallback(SR_MESSAGE_TYPE_INFO, L"XeSS Context create successful");
        }
        if (status == XESS_RESULT_SUCCESS)
        {
            privateData->isAvailable = true;
        }
        else
        {
            privateData->isAvailable = false;
            return (SRReturnCode)SR_RETURN_CODE_OK;
        }
        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        privateData->messageCallback = desc->messageCallback;
        context->userContext = privateData;

        xessSetLoggingCallback(
            privateData->xessContext,
            XESS_LOGGING_LEVEL_DEBUG,
            [](const char *msg, xess_logging_level_t level)
            {
                printf("[XeSS %d]: %s\n", level, msg);
            });
        ///////////////
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srXeSSDestroyUpscaleContext(SRUpscaleContext *context)
    {
        SRXeSSPrivateData *privateData = (SRXeSSPrivateData *)context->userContext;
        xessDestroyContext(privateData->xessContext);
        context->userContext = nullptr;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srXeSSQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType)
    {
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
        {
            xess_version_t version;
            xessGetVersion(&version);
            SRQueryVersionResult outResult = {};
            outResult.versionId = SR_MAKE_VERSION(version.major, version.minor, version.patch);
            outResult.versionNumber = SR_MAKE_VERSION(version.major, version.minor, version.patch);
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
        {
            xess_2d_t pOutputResolution = {};
            xess_properties_t pBindingProperties = {};
            xessGetProperties(
                ((SRXeSSPrivateData *)(context->userContext))->xessContext,
                &pOutputResolution,
                &pBindingProperties);
            SRQueryGpuMemoryResult outResult = {};
            outResult.gpuMemory = pBindingProperties.tempBufferHeapSize + pBindingProperties.tempTextureHeapSize;
            result->data = &outResult;
            break;
        }
        case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE:
        {
            SRQueryAvailabilityResult outResult = {};
            outResult.isAvailable = ((SRXeSSPrivateData *)(context->userContext))->isAvailable;
            result->data = &outResult;
            break;
        }
        default:
            break;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    xess_vk_image_view_info SRTextureResourceToXeSSResource(const SRTextureResource *resource)
    {
        xess_vk_image_view_info info = {};
        info.imageView = (VkImageView)(resource->imageView);
        info.image = (VkImage)resource->handle;
        info.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        info.subresourceRange.baseMipLevel = 0;
        info.subresourceRange.levelCount = 1;
        info.subresourceRange.baseArrayLayer = 0;
        info.subresourceRange.layerCount = 1;
        info.format = srTextureFormatToVkFormat(resource->desc.format);
        info.width = resource->desc.width;
        info.height = resource->desc.height;
        return info;
    }

    SR_API SRReturnCode srXeSSDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        xess_context_handle_t xessContext = ((SRXeSSPrivateData *)context->userContext)->xessContext;
        xess_coord_t renderSize = ((SRXeSSPrivateData *)context->userContext)->renderSize;

        xess_vk_execute_params_t *execute_params = new xess_vk_execute_params_t();
        if (desc->color.exist)
        {
            execute_params->colorTexture = SRTextureResourceToXeSSResource(&desc->color);
            // execute_params.inputColorBase = {0,0};
        }
        if (desc->depth.exist)
        {
            execute_params->depthTexture = SRTextureResourceToXeSSResource(&desc->depth);
            // execute_params.inputDepthBase = {renderSize.x,renderSize.y};
        }
        if (desc->motionVectors.exist)
        {
            execute_params->velocityTexture = SRTextureResourceToXeSSResource(&desc->motionVectors);
            // execute_params.inputMotionVectorBase = {renderSize.x,renderSize.y};
        }
        if (desc->exposure.exist)
        {
            execute_params->exposureScaleTexture = SRTextureResourceToXeSSResource(&desc->exposure);
        }
        if (desc->reactive.exist)
        {
            execute_params->responsivePixelMaskTexture = SRTextureResourceToXeSSResource(&desc->reactive);
        }
        if (desc->output.exist)
        {
            execute_params->outputTexture = SRTextureResourceToXeSSResource(&desc->output);
        }
        execute_params->jitterOffsetX = desc->jitterOffset.x;
        execute_params->jitterOffsetY = desc->jitterOffset.y;
        execute_params->exposureScale = desc->preExposure;
        execute_params->resetHistory = desc->reset ? 1 : 0;
        execute_params->inputWidth = desc->renderSize.x;
        execute_params->inputHeight = desc->renderSize.y;
        auto status = xessVKExecute(xessContext, desc->commandList.apiCommandBuffer.vulkan.commandBuffer, execute_params);
        if (status != XESS_RESULT_SUCCESS)
        {
            ((SRXeSSPrivateData *)context->userContext)->messageCallback(SR_MESSAGE_TYPE_ERROR, L"XeSS execute failed");
            ((SRXeSSPrivateData *)context->userContext)->messageCallback(SR_MESSAGE_TYPE_ERROR, std::to_wstring(status).c_str());
            return SR_RETURN_CODE_ERROR;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
    SR_API SRUpscaleContextCallbacks srGetXeSSUpscaleCallbacks()
    {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc)srXeSSCreateUpscaleContext,
            .pInit = (SRInitFunc)srXeSSInitUpscaleContext,
            .pDestroy = (SRDestroyFunc)srXeSSDestroyUpscaleContext,
            .pQuery = (SRQueryFunc)srXeSSQueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc)srXeSSDispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
