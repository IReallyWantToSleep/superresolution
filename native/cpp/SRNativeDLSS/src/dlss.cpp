#include "sr/sr_api.h"
#include "sr/dlss/dlss.h"
#include <cstring>
#include <cstdlib>
#include "sr/dlss/sr_provider.h"
#include <string>
#include <iostream>
#ifdef ON_WIN64
#include <windows.h>
#endif
#include "DLSS/include/nvsdk_ngx_vk.h"
#include "DLSS/include/nvsdk_ngx_helpers_vk.h"
#include "DLSS/include/nvsdk_ngx_defs.h"

struct SRDLSSPrivateData
{
    NVSDK_NGX_Handle *dlssHandle;
    NVSDK_NGX_Parameter *ngxParams;
    SRMessageCallback messageCallback;
};

static thread_local SRMessageCallback g_ngxLoggingCallback = nullptr;
static thread_local bool g_ngxInitialized = false;

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
        g_ngxLoggingCallback = desc->messageCallback;

        NVSDK_NGX_FeatureCommonInfo featureInfo = {};
        featureInfo.LoggingInfo = {};

        static auto ngxLogger = [](const char *message, NVSDK_NGX_Logging_Level loggingLevel, NVSDK_NGX_Feature sourceComponent) -> void
        {
            if (!g_ngxLoggingCallback || !message)
                return;

            size_t len = std::strlen(message) + 1;
            wchar_t *wideMessage = new wchar_t[len];
            std::mbstowcs(wideMessage, message, len);

            SRMessageType msgType = SR_MESSAGE_TYPE_INFO;
            if (loggingLevel <= NVSDK_NGX_LOGGING_LEVEL_ON)
            {
                msgType = SR_MESSAGE_TYPE_ERROR;
            }

            // NGX传进来的日志末尾有换行符，很不美观（划掉）
            size_t msgLen = std::wcslen(wideMessage);
            if (msgLen > 0 && wideMessage[msgLen - 1] == L'\n') {
                wideMessage[msgLen - 1] = L'\0';
            }

            g_ngxLoggingCallback(msgType, wideMessage);
            delete[] wideMessage;
        };

        featureInfo.LoggingInfo.LoggingCallback = desc->messageCallback ? ngxLogger : (NVSDK_NGX_AppLogCallback)nullptr;
        featureInfo.LoggingInfo.MinimumLoggingLevel = NVSDK_NGX_LOGGING_LEVEL_ON;
        if (srFindParam(&desc->extraParams, "NGX_FEATURE_DLL_PATH") != nullptr)
        {
            const SRContextExtraParam *param = srFindParam(&desc->extraParams, "NGX_FEATURE_DLL_PATH");
            if (param && param->valueType == SR_PARAM_VALUE_TYPE_STRING && param->value.stringValue)
            {
                size_t len = std::strlen(param->value.stringValue) + 1;
                wchar_t *widePath = new wchar_t[len];
                std::mbstowcs(widePath, param->value.stringValue, len);

                wchar_t const *const paths[] = {widePath};
                NVSDK_NGX_PathListInfo pathListInfo = {};
                pathListInfo.Path = paths;
                pathListInfo.Length = 1;
                featureInfo.PathListInfo = pathListInfo;
            }
        }
        NVSDK_NGX_FeatureDiscoveryInfo featureDiscoveryInfo = {};
        featureDiscoveryInfo.ApplicationDataPath = L".";
        featureDiscoveryInfo.FeatureID = NVSDK_NGX_Feature_SuperSampling;
        featureDiscoveryInfo.FeatureInfo = &featureInfo;
        featureDiscoveryInfo.SDKVersion = NVSDK_NGX_Version_API;

        if (!g_ngxInitialized)
        {
            g_ngxInitialized = true;
            const char *projectId = "3a799712-b54a-407c-82b0-eb3366f0f1e3";
            auto result = NVSDK_NGX_VULKAN_Init_with_ProjectID(
                projectId,
                NVSDK_NGX_ENGINE_TYPE_CUSTOM,
                "11.45.14",
                L".",
                (VkInstance)(vulkanDevice.instance),
                (VkPhysicalDevice)(vulkanDevice.physicalDevice),
                (VkDevice)(vulkanDevice.device),
                (PFN_vkGetInstanceProcAddr)(vulkanDevice.instanceProcAddr),
                (PFN_vkGetDeviceProcAddr)(vulkanDevice.deviceProcAddr),
                &featureInfo,
                NVSDK_NGX_Version_API);
            if (!NVSDK_NGX_SUCCEED(result))
            {
                std::wstring msg = L"NVSDK_NGX_VULKAN_Init failed. Code:";
                msg += GetNGXResultAsString(result);
                privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
                return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
            }
        }
        NVSDK_NGX_FeatureRequirement featureRequirement = {};
        auto result = NVSDK_NGX_VULKAN_GetFeatureRequirements(
            vulkanDevice.instance,
            vulkanDevice.physicalDevice,
            &featureDiscoveryInfo,
            &featureRequirement);
        if (!NVSDK_NGX_SUCCEED(result))
        {
            std::wstring msg = L"NVSDK_NGX_VULKAN_GetFeatureRequirements failed. Code:";
            msg += GetNGXResultAsString(result);
            privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
            // return (SRReturnCode)SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        if (NVSDK_NGX_SUCCEED(result))
        {
            if (featureRequirement.FeatureSupported != NVSDK_NGX_FeatureSupportResult_Supported)
            {
                /*
                NVSDK_NGX_FeatureSupportResult_Supported
                NVSDK_NGX_FeatureSupportResult_CheckNotPresent
                NVSDK_NGX_FeatureSupportResult_DriverVersionUnsupported
                NVSDK_NGX_FeatureSupportResult_AdapterUnsupported
                NVSDK_NGX_FeatureSupportResult_OSVersionBelowMinimumSupported
                NVSDK_NGX_FeatureSupportResult_NotImplemented
                */
                std::wstring msg;
                switch (featureRequirement.FeatureSupported)
                {
                case NVSDK_NGX_FeatureSupportResult_CheckNotPresent:
                    msg = L"DLSS not supported: Check Not Present.";
                    break;
                case NVSDK_NGX_FeatureSupportResult_DriverVersionUnsupported:
                    msg = L"DLSS not supported: Driver Version Unsupported.";
                    break;
                case NVSDK_NGX_FeatureSupportResult_AdapterUnsupported:
                    msg = L"DLSS not supported: Adapter Unsupported.";
                    break;
                case NVSDK_NGX_FeatureSupportResult_OSVersionBelowMinimumSupported:
                    msg = L"DLSS not supported: OS Version Below Minimum Supported.";
                    break;
                case NVSDK_NGX_FeatureSupportResult_NotImplemented:
                    msg = L"DLSS not supported: Not Implemented.";
                    break;
                }
                privateData->messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
                return (SRReturnCode)SR_RETURN_CODE_UNSUPPORTED;
            }
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
        // 如果已经初始化过，先释放旧的 handle
        if (privateData->dlssHandle)
        {
            NVSDK_NGX_VULKAN_ReleaseFeature(privateData->dlssHandle);
            privateData->dlssHandle = nullptr;
        }
        float upscaleRatio = desc->upscaledSize.x / desc->renderSize.x;
        NVSDK_NGX_DLSS_Create_Params dlssCreateParams = {};
        if (context->desc.flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE)
        {
            dlssCreateParams.InFeatureCreateFlags |= NVSDK_NGX_DLSS_Feature_Flags_AutoExposure;
        }

        if (context->desc.flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_MOTION_VECTORS_JITTERED)
        {
            dlssCreateParams.InFeatureCreateFlags |= NVSDK_NGX_DLSS_Feature_Flags_MVJittered;
        }
        if (context->desc.flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEPTH_INVERTED)
        {
            dlssCreateParams.InFeatureCreateFlags |= NVSDK_NGX_DLSS_Feature_Flags_DepthInverted;
        }
        if (context->desc.flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR)
        {
            dlssCreateParams.InFeatureCreateFlags |= NVSDK_NGX_DLSS_Feature_Flags_IsHDR;
        }
        dlssCreateParams.InFeatureCreateFlags |= NVSDK_NGX_DLSS_Feature_Flags_MVLowRes;
        dlssCreateParams.Feature.InWidth = desc->renderSize.x;
        dlssCreateParams.Feature.InHeight = desc->renderSize.y;
        dlssCreateParams.Feature.InTargetWidth = desc->upscaledSize.x;
        dlssCreateParams.Feature.InTargetHeight = desc->upscaledSize.y;
        SRVulkanDeviceInfo vulkanDevice = desc->renderDeviceInfo.vulkan;
        VkCommandBuffer cmd = (VkCommandBuffer)vulkanDevice.initCommandBuffer;
        if (srFindParam(&desc->extraParams, "DLSS_RENDER_PRESET") != nullptr)
        {
            const SRContextExtraParam *param = srFindParam(&desc->extraParams, "DLSS_RENDER_PRESET");
            if (param && param->valueType == SR_PARAM_VALUE_TYPE_INT32 && param->value.int32Value >= 0)
            {
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_DLAA, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Balanced, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Performance, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Quality, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_UltraPerformance, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
                privateData->ngxParams->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_UltraQuality, static_cast<NVSDK_NGX_DLSS_Hint_Render_Preset>(param->value.int32Value));
            }
        }
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
        if (privateData->ngxParams)
        {
            NVSDK_NGX_VULKAN_DestroyParameters(privateData->ngxParams);
            privateData->ngxParams = nullptr;
        }
        SRVulkanDeviceInfo vulkanDevice = context->desc.renderDeviceInfo.vulkan;
        if (srFindParam(&context->desc.extraParams, "ALWAYS_SHUTDOWN_NGX") != nullptr)
        {

            const SRContextExtraParam *param = srFindParam(&context->desc.extraParams, "ALWAYS_SHUTDOWN_NGX");
            if (param && param->valueType == SR_PARAM_VALUE_TYPE_BOOL && param->value.boolValue == true)
            {
                NVSDK_NGX_VULKAN_Shutdown1((VkDevice)vulkanDevice.device);
                g_ngxInitialized = false;
            }
        }
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
                .InSharpness = desc->sharpness,
            },
            .pInDepth = &depthAttachment,
            .pInMotionVectors = &motionVectors,
            .InJitterOffsetX = desc->jitterOffset.x,
            .InJitterOffsetY = desc->jitterOffset.y,
            .InRenderSubrectDimensions = {
                .Width = desc->renderSize.x,
                .Height = desc->renderSize.y,
            },
            .InReset = desc->reset ? 1 : 0,
            .InMVScaleX = desc->motionVectorScale.x,
            .InMVScaleY = desc->motionVectorScale.y,
            .InPreExposure = desc->preExposure,
            .InExposureScale = 1.f,
            .InFrameTimeDeltaInMsec = static_cast<float>(desc->frameTimeDelta),
        };
        auto result = NGX_VULKAN_EVALUATE_DLSS_EXT((VkCommandBuffer)desc->commandList.apiCommandBuffer.vulkan.commandBuffer, privateData->dlssHandle, params, &dlssEval);
        NVSDK_NGX_VULKAN_DestroyParameters(params);
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
