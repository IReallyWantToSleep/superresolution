#include "sr/sr_api.h"
#include "sr/dlss/dlss.h"
#include <cstring>
#include <cstdlib>
#include "sr/dlss/sr_provider.h"
#include <windows.h>
#include <string>
#include <iostream>
#include "DLSS/include/nvsdk_ngx_vk.h"
#include "DLSS/include/nvsdk_ngx_helpers_vk.h"

struct SRDLSSPrivateData
{
    NVSDK_NGX_Handle *dlssHandle;
    NVSDK_NGX_Parameter *ngxParams;
    SRMessageCallback messageCallback;
};
#ifdef __cplusplus
extern "C"
{
#endif
    static bool slInitialized = false;
    SR_API SRReturnCode srDLSSCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc)
    {
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN)
        {
            if (desc->messageCallback)
            {
                desc->messageCallback(SR_MESSAGE_TYPE_ERROR, L"DLSS only supports Vulkan render API.");
            }
            return (SRReturnCode)SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }
        ///////////////
        context->desc = *const_cast<SRCreateUpscaleContextDesc *>(desc);
        SRDLSSPrivateData *privateData = new SRDLSSPrivateData();
        SRVulkanDeviceInfo vulkanDevice = desc->renderDeviceInfo.vulkan;
        privateData->messageCallback = desc->messageCallback;
        context->userContext = privateData;
        auto result = NVSDK_NGX_VULKAN_Init(
            0x0000000000000000,
            L".",
            (VkInstance)(vulkanDevice.instance),
            (VkPhysicalDevice)(vulkanDevice.physicalDevice),
            (VkDevice)(vulkanDevice.device),
            (PFN_vkGetInstanceProcAddr)(vulkanDevice.instanceProcAddr),
            (PFN_vkGetDeviceProcAddr)(vulkanDevice.deviceProcAddr),
            nullptr,
            NVSDK_NGX_Version_API);
        if (!NVSDK_NGX_SUCCEED(result))
        {
            std::wstring msg = L"NVSDK_NGX_VULKAN_Init failed. Code:";
            msg += GetNGXResultAsString(result);
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
            return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        result = NVSDK_NGX_VULKAN_GetCapabilityParameters(&privateData->ngxParams);
        if (!NVSDK_NGX_SUCCEED(result))
        {
            std::wstring msg = L"NVSDK_NGX_VULKAN_GetCapabilityParameters failed. Code:";
            msg += GetNGXResultAsString(result);
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
            return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        ///////////////
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSInitUpscaleContext(SRUpscaleContext *context)
    {
        SRDLSSPrivateData *privateData = (SRDLSSPrivateData *)context->userContext;
        const SRCreateUpscaleContextDesc *desc = &context->desc;
        float upscaleRatio = desc->upscaledSize.x / desc->renderSize.x;
        NVSDK_NGX_DLSS_Create_Params dlssCreateParams = {};
        dlssCreateParams.Feature.InWidth = desc->renderSize.x;
        dlssCreateParams.Feature.InHeight = desc->renderSize.y;
        dlssCreateParams.Feature.InTargetWidth = desc->upscaledSize.x;
        dlssCreateParams.Feature.InTargetHeight = desc->upscaledSize.y;
        SRVulkanDeviceInfo vulkanDevice = desc->renderDeviceInfo.vulkan;
        VkCommandBuffer cmd = (VkCommandBuffer)vulkanDevice.initCommandBuffer;
        auto result = NGX_VULKAN_CREATE_DLSS_EXT1(
            vulkanDevice.device,
            cmd,
            1,
            1,
            &privateData->dlssHandle,
            privateData->ngxParams,
            &dlssCreateParams);

        if (!NVSDK_NGX_SUCCEED(result))
        {
            std::wstring msg = L"NGX_VULKAN_CREATE_DLSS_EXT1 failed. Code:";
            msg += GetNGXResultAsString(result);
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
            return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSDestroyUpscaleContext(SRUpscaleContext *context)
    {
        SRDLSSPrivateData *privateData = (SRDLSSPrivateData *)context->userContext;
        if (privateData->dlssHandle)
        {
            NVSDK_NGX_VULKAN_ReleaseFeature(privateData->dlssHandle);
            privateData->dlssHandle = nullptr;
        }
        SRVulkanDeviceInfo vulkanDevice = context->desc.renderDeviceInfo.vulkan;
        NVSDK_NGX_VULKAN_Shutdown1((VkDevice)vulkanDevice.device);
        delete privateData;
        context->userContext = nullptr;
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result, SRUpscaleContextQueryType queryType)
    {
        SRUpscaleContextQueryResult *outResult = result;
        switch (queryType)
        {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO:
            ((SRQueryVersionResult *)outResult)->versionId = SR_MAKE_VERSION(
                1,
                5,
                0);
            ((SRQueryVersionResult *)outResult)->versionNumber = SR_MAKE_VERSION(
                1,
                5,
                0);
            break;
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO:
            ((SRQueryGpuMemoryResult *)outResult)->gpuMemory = 0;
            return (SRReturnCode)SR_RETURN_CODE_UNSUPPORTED;
            break;
        case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE:
        {
            SRDLSSPrivateData *privateData = (SRDLSSPrivateData *)context->userContext;
            int support = 0;
            privateData->ngxParams->Get(NVSDK_NGX_Parameter_SuperSampling_Available, &support);
            ((SRQueryAvailabilityResult *)outResult)->isAvailable = NVSDK_NGX_SUCCEED(support) && static_cast<bool>(support);
            break;
        }
        default:
            break;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc)
    {
        SRDLSSPrivateData *privateData = (SRDLSSPrivateData *)context->userContext;
        const SRCreateUpscaleContextDesc *createDesc = &context->desc;
        if (!privateData->dlssHandle)
        {
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, L"DLSS handle is null in srDLSSDispatchUpscale.");
            return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        if (desc->motionVectors.exist == false)
        {
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, L"DLSS requires motion vectors input.");
            return (SRReturnCode)SR_RETURN_CODE_INVALID_ARGUMENT;
        }
        NVSDK_NGX_Parameter *params = nullptr;
        NVSDK_NGX_VULKAN_AllocateParameters(&params);
        NVSDK_NGX_Resource_VK colorInput{
            .Resource = {
                .ImageViewInfo = {
                    .ImageView = (VkImageView)desc->color.imageView,
                    .Image = (VkImage)desc->color.handle,
                    .SubresourceRange = {
                        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                        .levelCount = 1,
                        .layerCount = 1,
                    },
                    .Format = srTextureFormatToVkFormat(desc->color.desc.format),
                    .Width = desc->color.desc.width,
                    .Height = desc->color.desc.height,
                }},
            .Type = NVSDK_NGX_RESOURCE_VK_TYPE_VK_IMAGEVIEW,
            .ReadWrite = true,
        };
        NVSDK_NGX_Resource_VK colorOutput{
            .Resource = {
                .ImageViewInfo = {
                    .ImageView = (VkImageView)desc->output.imageView,
                    .Image = (VkImage)desc->output.handle,
                    .SubresourceRange = {
                        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                        .levelCount = 1,
                        .layerCount = 1,
                    },
                    .Format = srTextureFormatToVkFormat(desc->output.desc.format),
                    .Width = desc->output.desc.width,
                    .Height = desc->output.desc.height,
                }},
            .Type = NVSDK_NGX_RESOURCE_VK_TYPE_VK_IMAGEVIEW,
            .ReadWrite = true,
        };
        NVSDK_NGX_Resource_VK depthAttachment{
            .Resource = {
                .ImageViewInfo = {
                    .ImageView = (VkImageView)desc->depth.imageView,
                    .Image = (VkImage)desc->depth.handle,
                    .SubresourceRange = {
                        .aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT,
                        .levelCount = 1,
                        .layerCount = 1,
                    },
                    .Format = srTextureFormatToVkFormat(desc->depth.desc.format),
                    .Width = desc->depth.desc.width,
                    .Height = desc->depth.desc.height,
                }},
            .Type = NVSDK_NGX_RESOURCE_VK_TYPE_VK_IMAGEVIEW,
            .ReadWrite = false,
        };
        NVSDK_NGX_Resource_VK motionVectors{
            .Resource = {
                .ImageViewInfo = {
                    .ImageView = (VkImageView)desc->motionVectors.imageView,
                    .Image = (VkImage)desc->motionVectors.handle,
                    .SubresourceRange = {
                        .aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                        .levelCount = 1,
                        .layerCount = 1,
                    },
                    .Format = srTextureFormatToVkFormat(desc->motionVectors.desc.format),
                    .Width = desc->motionVectors.desc.width,
                    .Height = desc->motionVectors.desc.height,
                }},
            .Type = NVSDK_NGX_RESOURCE_VK_TYPE_VK_IMAGEVIEW,
            .ReadWrite = true,
        };

        NVSDK_NGX_VK_DLSS_Eval_Params dlssEval{
            .Feature = {
                .pInColor = &colorInput,
                .pInOutput = &colorOutput,
                .InSharpness = 0.35f, // Some random value that works?
            },
            .pInDepth = &depthAttachment,
            .pInMotionVectors = &motionVectors,
            .InRenderSubrectDimensions = {
                .Width = desc->renderSize.x,
                .Height = desc->renderSize.y,
            },
            .InMVScaleX = 1.f,
            .InMVScaleY = 1.f,
            .InPreExposure = 1.f,
            .InExposureScale = 1.f,
            .InFrameTimeDeltaInMsec = static_cast<float>(desc->frameTimeDelta),
        };
        auto result = NGX_VULKAN_EVALUATE_DLSS_EXT((VkCommandBuffer)desc->commandList.apiCommandBuffer.vulkan.commandBuffer, privateData->dlssHandle, params, &dlssEval);
        if (!NVSDK_NGX_SUCCEED(result))
        {
            std::wstring msg = L"NVSDK_NGX_VULKAN_EVALUATE_DLSS_EXT failed. Code:";
            msg += GetNGXResultAsString(result);
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
            return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        return (SRReturnCode)SR_RETURN_CODE_OK;
    }
    SR_API SRUpscaleContextCallbacks srGetDLSSUpscaleCallbacks()
    {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = (SRCreateFunc)srDLSSCreateUpscaleContext,
            .pInit = (SRInitFunc)srDLSSInitUpscaleContext,
            .pDestroy = (SRDestroyFunc)srDLSSDestroyUpscaleContext,
            .pQuery = (SRQueryFunc)srDLSSQueryUpscale,
            .pDispatchUpscale = (SRDispatchUpscaleFunc)srDLSSDispatchUpscale,
        };
        return callbacks;
    }

#ifdef __cplusplus
}
#endif
