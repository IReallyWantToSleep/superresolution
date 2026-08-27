#include "sr/sr_api.h"
#include "sr/sr_api_functions.h"
#include "sr/dlssnr/dlssnr.h"
#include "ngx_params.h"

#include "nvsdk_ngx_defs_vk.h"

#include <cstring>
#include <mutex>
#include <new>
#include <string>
#include <windows.h>

// DLSSNR (DLSS Neural Rendering) NGX feature id。公开 SDK 头中该槽位为 Reserved18。
static constexpr NVSDK_NGX_Feature kFeatureDLSSNR = static_cast<NVSDK_NGX_Feature>(18);

// ---------------------------------------------------------------------------
// nvngx_dlssnr.dll 函数表(运行期 GetProcAddress,不经过 ngx core)
// 注意:需使用已抹除"调用者路径须含 nvngx.dll"检查的补丁版 DLL。
// ---------------------------------------------------------------------------
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_GetAPIVersion)(NVSDK_NGX_Version *);
typedef unsigned int (NVSDK_CONV *PFN_NVGX_GetSnippetVersion)();
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_Init_Ext2)(
    unsigned long long, const wchar_t *, VkInstance, VkPhysicalDevice, VkDevice,
    PFN_vkGetInstanceProcAddr, PFN_vkGetDeviceProcAddr, NVSDK_NGX_Version, const NVSDK_NGX_Parameter *);
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_Shutdown1)(VkDevice);
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_CreateFeature1)(
    VkDevice, VkCommandBuffer, NVSDK_NGX_Feature, NVSDK_NGX_Parameter *, NVSDK_NGX_Handle **);
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_EvaluateFeature)(
    VkCommandBuffer, const NVSDK_NGX_Handle *, const NVSDK_NGX_Parameter *, void *);
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_ReleaseFeature)(NVSDK_NGX_Handle *);
typedef NVSDK_NGX_Result(NVSDK_CONV *PFN_NVGX_VULKAN_GetFeatureRequirements)(
    VkInstance, VkPhysicalDevice, const NVSDK_NGX_FeatureDiscoveryInfo *, NVSDK_NGX_FeatureRequirement *);

struct SRDLSSNRFunctionsTable {
    PFN_NVGX_GetAPIVersion GetAPIVersion;
    PFN_NVGX_GetSnippetVersion GetSnippetVersion;
    PFN_NVGX_VULKAN_Init_Ext2 InitExt2;
    PFN_NVGX_VULKAN_Shutdown1 Shutdown1;
    PFN_NVGX_VULKAN_CreateFeature1 CreateFeature1;
    PFN_NVGX_VULKAN_EvaluateFeature EvaluateFeature;
    PFN_NVGX_VULKAN_ReleaseFeature ReleaseFeature;
    PFN_NVGX_VULKAN_GetFeatureRequirements GetFeatureRequirements;
};

static SRDLSSNRFunctionsTable g_ngx = {};
static bool g_ngxFunctionsLoaded = false;
static HMODULE g_ngxModule = nullptr;
static size_t g_contextCount = 0;
static bool g_ngxInited = false;
static VkDevice g_ngxDevice = nullptr;
static std::mutex g_ngxMutex;

// SRGetFuncAddress (void*(*)(void*,const char*)) 到 vkGet*ProcAddr 的适配
static SRGetFuncAddress g_srInstanceProcAddr = nullptr;
static SRGetFuncAddress g_srDeviceProcAddr = nullptr;
static void *g_srInstanceForGipa = nullptr;
static void *g_srDeviceForGdpa = nullptr;

static void *VKAPI_CALL srGipaTrampoline(VkInstance, const char *pName) {
    return g_srInstanceProcAddr ? g_srInstanceProcAddr(g_srInstanceForGipa, pName) : nullptr;
}
static void *VKAPI_CALL srGdpaTrampoline(VkDevice, const char *pName) {
    return g_srDeviceProcAddr ? g_srDeviceProcAddr(g_srDeviceForGdpa, pName) : nullptr;
}

static void srDLSSNRLog(SRMessageCallback cb, SRMessageType type, const wchar_t *msg) {
    if (cb && msg) cb(type, msg);
}

template<typename T>
static bool srDLSSNRResolve(T &fn, const char *name, std::wstring &errorMessage) {
    fn = reinterpret_cast<T>(GetProcAddress(g_ngxModule, name));
    if (!fn) {
        if (errorMessage.empty()) {
            std::wstring wideName(name, name + std::strlen(name));
            errorMessage = L"Failed to resolve DLSSNR symbol: ";
            errorMessage += wideName;
        }
        return false;
    }
    return true;
}

SR_API SRReturnCode srDLSSNRLoadFunctionsFromDll(const char *dllPath, SRMessageCallback messageCallback) {
    std::unique_lock<std::mutex> lock(g_ngxMutex);
    if (g_ngxFunctionsLoaded) {
        return SR_RETURN_CODE_OK;
    }
    if (g_contextCount != 0) {
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }
    if (g_ngxModule) {
        if (!FreeLibrary(g_ngxModule)) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        g_ngxModule = nullptr;
        g_ngx = {};
    }

    const char *effectivePath = (dllPath && std::strlen(dllPath) > 0) ? dllPath : "nvngx_dlssnr.dll";

    int wideLen = MultiByteToWideChar(CP_UTF8, 0, effectivePath, -1, nullptr, 0);
    if (wideLen <= 0) {
        lock.unlock();
        srDLSSNRLog(messageCallback, SR_MESSAGE_TYPE_ERROR, L"Failed to convert DLSSNR dll path to wide string.");
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    std::wstring widePath;
    widePath.resize(static_cast<size_t>(wideLen));
    MultiByteToWideChar(CP_UTF8, 0, effectivePath, -1, widePath.data(), wideLen);
    g_ngxModule = LoadLibraryW(widePath.c_str());

    if (!g_ngxModule) {
        std::wstring msg = L"Failed to load DLSSNR library (需要使用抹除 nvngx.dll 路径检查的补丁版): ";
        msg += widePath;
        lock.unlock();
        srDLSSNRLog(messageCallback, SR_MESSAGE_TYPE_ERROR, msg.c_str());
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    bool resolved = true;
    std::wstring resolveError;
    resolved &= srDLSSNRResolve(g_ngx.GetAPIVersion, "NVSDK_NGX_GetAPIVersion", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.GetSnippetVersion, "NVSDK_NGX_GetSnippetVersion", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.InitExt2, "NVSDK_NGX_VULKAN_Init_Ext2", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.Shutdown1, "NVSDK_NGX_VULKAN_Shutdown1", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.CreateFeature1, "NVSDK_NGX_VULKAN_CreateFeature1", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.EvaluateFeature, "NVSDK_NGX_VULKAN_EvaluateFeature", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.ReleaseFeature, "NVSDK_NGX_VULKAN_ReleaseFeature", resolveError);
    resolved &= srDLSSNRResolve(g_ngx.GetFeatureRequirements, "NVSDK_NGX_VULKAN_GetFeatureRequirements", resolveError);

    if (!resolved) {
        SRReturnCode result = SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
        if (!FreeLibrary(g_ngxModule)) {
            result = SR_RETURN_CODE_UNEXPECTED_ERROR;
        } else {
            g_ngxModule = nullptr;
            g_ngx = {};
        }
        lock.unlock();
        srDLSSNRLog(messageCallback, SR_MESSAGE_TYPE_ERROR, resolveError.c_str());
        return result;
    }

    g_ngxFunctionsLoaded = true;
    return SR_RETURN_CODE_OK;
}

struct SRDLSSNRPrivateData {
    NVSDK_NGX_Handle *ngxHandle = nullptr;
    SRMessageCallback messageCallback = nullptr;
    bool featureCreated = false;
    bool isAvailable = false;
};

static NVSDK_NGX_Resource_VK srTextureResourceToNgxResource(const SRTextureResource *resource, bool readWrite) {
    NVSDK_NGX_Resource_VK out = {};
    out.Resource.ImageViewInfo.ImageView = static_cast<VkImageView>(resource->imageView);
    out.Resource.ImageViewInfo.Image = static_cast<VkImage>(resource->handle);
    out.Resource.ImageViewInfo.SubresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    out.Resource.ImageViewInfo.SubresourceRange.baseMipLevel = 0;
    out.Resource.ImageViewInfo.SubresourceRange.levelCount = 1;
    out.Resource.ImageViewInfo.SubresourceRange.baseArrayLayer = 0;
    out.Resource.ImageViewInfo.SubresourceRange.layerCount = 1;
    out.Resource.ImageViewInfo.Format = srTextureFormatToVkFormat(resource->desc.format);
    out.Resource.ImageViewInfo.Width = resource->desc.width;
    out.Resource.ImageViewInfo.Height = resource->desc.height;
    out.Type = NVSDK_NGX_RESOURCE_VK_TYPE_VK_IMAGEVIEW;
    out.ReadWrite = readWrite;
    return out;
}

#ifdef __cplusplus
extern "C" {
    #endif

    SR_API SRReturnCode srDLSSNRCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
        if (!context || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN) {
            srDLSSNRLog(desc->messageCallback, SR_MESSAGE_TYPE_ERROR, L"DLSSNR only supports Vulkan");
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }

        const char *dllPath = "nvngx_dlssnr.dll";
        const SRContextExtraParam *dllPathParam = srFindParam(&desc->extraParams, "DLSSNR_DLL_PATH");
        if (dllPathParam && dllPathParam->valueType == SR_PARAM_VALUE_TYPE_STRING && dllPathParam->value.stringValue) {
            dllPath = dllPathParam->value.stringValue;
        }

        if (srDLSSNRLoadFunctionsFromDll(dllPath, desc->messageCallback) != SR_RETURN_CODE_OK) {
            return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
        }

        auto *privateData = new(std::nothrow) SRDLSSNRPrivateData{};
        if (!privateData) {
            return SR_RETURN_CODE_ERROR;
        }
        privateData->messageCallback = desc->messageCallback;
        privateData->isAvailable = true;
        context->desc = *desc;
        context->userContext = privateData;
        ++g_contextCount;

        srDLSSNRLog(desc->messageCallback, SR_MESSAGE_TYPE_INFO, L"DLSSNR context created");
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSNRInitUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = static_cast<SRDLSSNRPrivateData *>(context->userContext);
        if (privateData->featureCreated) {
            return SR_RETURN_CODE_OK;
        }

        const SRCreateUpscaleContextDesc *desc = &context->desc;
        std::unique_lock<std::mutex> lock(g_ngxMutex);
        if (!g_ngxFunctionsLoaded || !g_ngx.InitExt2 || !g_ngx.CreateFeature1) {
            return SR_RETURN_CODE_UNSUPPORTED;
        }

        const SRVulkanDeviceInfo &vk = desc->renderDeviceInfo.vulkan;
        g_srInstanceProcAddr = vk.instanceProcAddr;
        g_srDeviceProcAddr = vk.deviceProcAddr;
        g_srInstanceForGipa = (void *) vk.instance;
        g_srDeviceForGdpa = (void *) vk.device;

        if (!g_ngxInited) {
            NVSDK_NGX_Result r = g_ngx.InitExt2(
                0, L".",
                (VkInstance) vk.instance, (VkPhysicalDevice) vk.physicalDevice, (VkDevice) vk.device,
                (PFN_vkGetInstanceProcAddr) srGipaTrampoline, (PFN_vkGetDeviceProcAddr) srGdpaTrampoline,
                NVSDK_NGX_Version_API, nullptr);
            if (NVSDK_NGX_FAILED(r)) {
                lock.unlock();
                srDLSSNRLog(desc->messageCallback, SR_MESSAGE_TYPE_ERROR, L"DLSSNR NVSDK_NGX_VULKAN_Init_Ext2 failed");
                return SR_RETURN_CODE_ERROR;
            }
            g_ngxInited = true;
            g_ngxDevice = (VkDevice) vk.device;
        }

        // create 参数:core 缺席时没人跑 ScalingRatio 回调,需显式给出缩放比,否则按 1.0 不放大
        SRNgxParams params;
        params.Set("DLSSNR.Width", (unsigned int) desc->upscaledSize.x);
        params.Set("DLSSNR.Height", (unsigned int) desc->upscaledSize.y);

        float scalingRatio = 0.0f;
        srParamsGetFloat(&desc->extraParams, "DLSSNR_SCALING_RATIO", &scalingRatio, 0.0f);
        if (scalingRatio <= 0.0f && desc->renderSize.x > 0) {
            scalingRatio = static_cast<float>(desc->upscaledSize.x) / static_cast<float>(desc->renderSize.x);
        }
        if (scalingRatio > 0.0f) {
            params.Set("DLSSNR.ScalingRatio", scalingRatio);
        }

        int32_t preset = 0;
        if (srParamsGetInt32(&desc->extraParams, "DLSSNR_PRESET", &preset, 0) == SR_RETURN_CODE_OK) {
            params.Set("DLSSNR.Hint.Render.Preset", (int) preset);
        }
        uint32_t perfQuality = 0;
        if (srParamsGetUint32(&desc->extraParams, "DLSSNR_PERF_QUALITY", &perfQuality, 0) == SR_RETURN_CODE_OK) {
            params.Set("PerfQualityValue", (unsigned int) perfQuality);
        }

        NVSDK_NGX_Result r = g_ngx.CreateFeature1(
            (VkDevice) vk.device,
            (VkCommandBuffer) vk.initCommandBuffer,
            kFeatureDLSSNR,
            &params,
            &privateData->ngxHandle);
        if (NVSDK_NGX_FAILED(r) || !privateData->ngxHandle) {
            lock.unlock();
            srDLSSNRLog(desc->messageCallback, SR_MESSAGE_TYPE_ERROR, L"DLSSNR CreateFeature1 failed");
            return SR_RETURN_CODE_ERROR;
        }
        privateData->featureCreated = true;
        lock.unlock();
        srDLSSNRLog(desc->messageCallback, SR_MESSAGE_TYPE_INFO, L"DLSSNR feature created (feature id 18)");
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSNRDestroyUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = static_cast<SRDLSSNRPrivateData *>(context->userContext);
        std::unique_lock<std::mutex> lock(g_ngxMutex);
        if (privateData->featureCreated && privateData->ngxHandle) {
            if (!g_ngx.ReleaseFeature) {
                return SR_RETURN_CODE_ERROR;
            }
            NVSDK_NGX_Result r = g_ngx.ReleaseFeature(privateData->ngxHandle);
            privateData->ngxHandle = nullptr;
            privateData->featureCreated = false;
            if (NVSDK_NGX_FAILED(r)) {
                lock.unlock();
                srDLSSNRLog(privateData->messageCallback, SR_MESSAGE_TYPE_ERROR, L"DLSSNR ReleaseFeature failed");
                return SR_RETURN_CODE_ERROR;
            }
        }
        if (g_contextCount != 0) {
            --g_contextCount;
        }
        lock.unlock();
        delete privateData;
        context->userContext = nullptr;
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSNRQueryUpscale(SRUpscaleContext *context, SRUpscaleContextQueryResult *result,
                                             SRUpscaleContextQueryType queryType) {
        if (!context || !context->userContext || !result) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = static_cast<SRDLSSNRPrivateData *>(context->userContext);
        std::lock_guard<std::mutex> lock(g_ngxMutex);
        if (!g_ngxFunctionsLoaded) {
            return SR_RETURN_CODE_UNSUPPORTED;
        }
        switch (queryType) {
            case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO: {
                static thread_local SRQueryVersionResult outResult = {};
                uint64_t packed = 0;
                if (g_ngx.GetSnippetVersion) {
                    packed = g_ngx.GetSnippetVersion(); // 返回值即打包版本号,如 0x01360800 = 310.8.0
                }
                outResult.versionNumber = packed;
                outResult.versionId = packed;
                result->data = &outResult;
                break;
            }
            case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO: {
                static thread_local SRQueryGpuMemoryResult outResult = {};
                outResult.gpuMemory = 0; // NGX 无对应查询接口
                result->data = &outResult;
                break;
            }
            case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE: {
                bool available = privateData->isAvailable;
                if (available && g_ngx.GetFeatureRequirements) {
                    // 进一步查询 GPU/驱动是否支持 DLSSNR(minHWArch=0x1b0,Blackwell 级)
                    const SRVulkanDeviceInfo &vk = context->desc.renderDeviceInfo.vulkan;
                    NVSDK_NGX_FeatureDiscoveryInfo disc = {};
                    disc.SDKVersion = NVSDK_NGX_Version_API;
                    disc.FeatureID = kFeatureDLSSNR;
                    disc.Identifier.IdentifierType = NVSDK_NGX_Application_Identifier_Type_Application_Id;
                    disc.Identifier.v.ApplicationId = 0;
                    disc.ApplicationDataPath = L".";
                    disc.FeatureInfo = nullptr;
                    NVSDK_NGX_FeatureRequirement req = {};
                    NVSDK_NGX_Result r = g_ngx.GetFeatureRequirements(
                        (VkInstance) vk.instance, (VkPhysicalDevice) vk.physicalDevice, &disc, &req);
                    available = NVSDK_NGX_SUCCEED(r) &&
                                (req.FeatureSupported == NVSDK_NGX_FeatureSupportResult_Supported);
                }
                static thread_local SRQueryAvailabilityResult outResult = {};
                outResult.isAvailable = available;
                result->data = &outResult;
                break;
            }
            default:
                break;
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSNRDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
        if (!context || !context->userContext || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = static_cast<SRDLSSNRPrivateData *>(context->userContext);
        if (!privateData->featureCreated || !privateData->ngxHandle) {
            return SR_RETURN_CODE_ERROR;
        }
        std::unique_lock<std::mutex> lock(g_ngxMutex);
        if (!g_ngxFunctionsLoaded || !g_ngx.EvaluateFeature) {
            return SR_RETURN_CODE_UNSUPPORTED;
        }

        // SRAPI 有什么给什么;UI/UIAlpha/Backbuffer/ControlMask 等不传
        NVSDK_NGX_Resource_VK colorRes = {};
        NVSDK_NGX_Resource_VK depthRes = {};
        NVSDK_NGX_Resource_VK mvecRes = {};
        NVSDK_NGX_Resource_VK outputRes = {};

        SRNgxParams params;
        if (desc->color.exist) {
            colorRes = srTextureResourceToNgxResource(&desc->color, false);
            params.Set("DLSSNR.Color", (void *) &colorRes);
        }
        if (desc->depth.exist) {
            depthRes = srTextureResourceToNgxResource(&desc->depth, false);
            params.Set("DLSSNR.Depth", (void *) &depthRes);
        }
        if (desc->motionVectors.exist) {
            mvecRes = srTextureResourceToNgxResource(&desc->motionVectors, false);
            params.Set("DLSSNR.MVec", (void *) &mvecRes);
        }
        if (desc->output.exist) {
            outputRes = srTextureResourceToNgxResource(&desc->output, true);
            params.Set("DLSSNR.Output", (void *) &outputRes);
        }

        params.Set("DLSSNR.Reset", (int) (desc->reset ? 1 : 0));
        params.Set("DLSSNR.Enabled", (int) 1);
        params.Set("DLSSNR.MVecScaleX", desc->motionVectorScale.x);
        params.Set("DLSSNR.MVecScaleY", desc->motionVectorScale.y);
        params.Set("DLSSNR.Width", (unsigned int) desc->upscaleSize.x);
        params.Set("DLSSNR.Height", (unsigned int) desc->upscaleSize.y);

        // dispatch 级 extra params 透传(类型按插件 Evaluate 时实际 Get 的类型)
        float f32;
        int32_t i32;
        uint32_t u32;
        if (srParamsGetFloat(&desc->extraParams, "DLSSNR_INTENSITY", &f32, 0.0f) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.Intensity", f32);
        if (srParamsGetFloat(&desc->extraParams, "DLSSNR_LOCAL_TONE_STRENGTH", &f32, 0.0f) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.LocalToneStrength", f32);
        if (srParamsGetFloat(&desc->extraParams, "DLSSNR_LOCAL_STRUCTURE_STRENGTH", &f32, 0.0f) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.LocalStructureStrength", f32);
        if (srParamsGetFloat(&desc->extraParams, "DLSSNR_SKIN_STRUCTURE_STRENGTH", &f32, 0.0f) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.SkinStructureStrength", f32);
        if (srParamsGetUint32(&desc->extraParams, "DLSSNR_STYLE", &u32, 0) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.Style", (unsigned int) u32);
        if (srParamsGetInt32(&desc->extraParams, "DLSSNR_USE_AUTO_MASK", &i32, 0) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.UseAutoMask", (int) i32);
        if (srParamsGetInt32(&desc->extraParams, "DLSSNR_UI_CORRECTION", &i32, 0) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.UICorrection", (int) i32);
        if (srParamsGetInt32(&desc->extraParams, "DLSSNR_DEPTH_INVERTED", &i32, 0) == SR_RETURN_CODE_OK)
            params.Set("DLSSNR.DepthInverted", (int) i32);

        NVSDK_NGX_Result r = g_ngx.EvaluateFeature(
            desc->commandList.apiCommandBuffer.vulkan.commandBuffer,
            privateData->ngxHandle,
            &params,
            nullptr);
        if (NVSDK_NGX_FAILED(r)) {
            lock.unlock();
            srDLSSNRLog(privateData->messageCallback, SR_MESSAGE_TYPE_ERROR, L"DLSSNR EvaluateFeature failed");
            return SR_RETURN_CODE_ERROR;
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srDLSSNRShutdown() {
        std::lock_guard<std::mutex> lock(g_ngxMutex);
        if (g_contextCount != 0) {
            return SR_RETURN_CODE_UNEXPECTED_ERROR;
        }
        if (g_ngxInited) {
            if (g_ngx.Shutdown1 && g_ngxDevice) {
                g_ngx.Shutdown1(g_ngxDevice);
            }
            g_ngxInited = false;
            g_ngxDevice = nullptr;
        }
        if (g_ngxModule) {
            if (!FreeLibrary(g_ngxModule)) {
                return SR_RETURN_CODE_UNEXPECTED_ERROR;
            }
            g_ngxModule = nullptr;
        }
        g_ngx = {};
        g_ngxFunctionsLoaded = false;
        return SR_RETURN_CODE_OK;
    }

    SR_API SRUpscaleContextCallbacks srGetDLSSNRUpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = static_cast<SRCreateFunc>(srDLSSNRCreateUpscaleContext),
            .pInit = static_cast<SRInitFunc>(srDLSSNRInitUpscaleContext),
            .pDestroy = static_cast<SRDestroyFunc>(srDLSSNRDestroyUpscaleContext),
            .pQuery = reinterpret_cast<SRQueryFunc>(srDLSSNRQueryUpscale),
            .pDispatchUpscale = static_cast<SRDispatchUpscaleFunc>(srDLSSNRDispatchUpscale),
            .pShutdown = static_cast<SRShutdownFunc>(srDLSSNRShutdown),
        };
        return callbacks;
    }

    #ifdef __cplusplus
}
#endif
