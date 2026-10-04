#include "sr/sr_api.h"
#include "sr/nss/nss.h"
#include "sr/nss/sr_provider.h"

#include <ffx_api/ffx_api.h>
#include <ffx_api/ffx_nss.h>
#include <ffx_api/vk/ffx_api_vk.h>

#include <cstdlib>
#include <cwchar>
#include <memory>
#include <new>
#include <string>

#if defined(_WIN32)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
using SRNssModuleHandle = HMODULE;
#else
#include <dlfcn.h>
using SRNssModuleHandle = void *;
#endif

namespace {

struct SRFfxApiFunctions {
    PfnFfxCreateContext createContext = nullptr;
    PfnFfxDestroyContext destroyContext = nullptr;
    PfnFfxQuery query = nullptr;
    PfnFfxDispatch dispatch = nullptr;
};

struct SRNssPrivateData {
    SRNssModuleHandle module = nullptr;
    SRFfxApiFunctions functions = {};
    ffxContext context = nullptr;
    SRMessageCallback messageCallback = nullptr;
    uint32_t dispatchFlags = 0;
    bool contextCreated = false;
};

thread_local SRMessageCallback g_activeMessageCallback = nullptr;

std::wstring utf8ToWide(const char *value) {
    if (!value || !*value) {
        return {};
    }

#if defined(_WIN32)
    const int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, nullptr, 0);
    if (length <= 0) {
        return {};
    }
    std::wstring result(static_cast<size_t>(length), L'\0');
    if (MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, result.data(), length) <= 0) {
        return {};
    }
    result.pop_back();
    return result;
#else
    const size_t length = std::mbstowcs(nullptr, value, 0);
    if (length == static_cast<size_t>(-1)) {
        return {};
    }
    std::wstring result(length, L'\0');
    std::mbstowcs(result.data(), value, length);
    return result;
#endif
}

void report(
    const SRCreateUpscaleContextDesc *desc,
    SRMessageType type,
    const std::wstring &message) {
    if (desc && desc->messageCallback && !message.empty()) {
        desc->messageCallback(type, message.c_str());
    }
}

void reportText(
    const SRCreateUpscaleContextDesc *desc,
    SRMessageType type,
    const char *message) {
    report(desc, type, utf8ToWide(message));
}

void ffxMessageBridge(uint32_t type, const char *message) {
    SRMessageCallback callback = g_activeMessageCallback;
    if (!callback) {
        return;
    }

    const std::wstring wideMessage = utf8ToWide(message);
    if (wideMessage.empty()) {
        return;
    }
    callback(
        type == FFX_API_MESSAGE_TYPE_ERROR
            ? SR_MESSAGE_TYPE_ERROR
            : SR_MESSAGE_TYPE_WARNING,
        wideMessage.c_str());
}

void *getSymbol(SRNssModuleHandle module, const char *name) {
#if defined(_WIN32)
    return reinterpret_cast<void *>(GetProcAddress(module, name));
#else
    return dlsym(module, name);
#endif
}

SRNssModuleHandle loadModule(const char *path) {
#if defined(_WIN32)
    const std::wstring widePath = utf8ToWide(path);
    if (widePath.empty()) {
        return nullptr;
    }

    HMODULE module = LoadLibraryExW(
        widePath.c_str(),
        nullptr,
        LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR | LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
    if (!module && !std::wcschr(widePath.c_str(), L'\\') &&
        !std::wcschr(widePath.c_str(), L'/')) {
        module = LoadLibraryExW(
            widePath.c_str(),
            nullptr,
            LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
    }
    return module;
#else
    return dlopen(path, RTLD_NOW | RTLD_LOCAL);
#endif
}

bool unloadModule(SRNssModuleHandle module) {
    if (!module) {
        return true;
    }
#if defined(_WIN32)
    return FreeLibrary(module) != 0;
#else
    return dlclose(module) == 0;
#endif
}

bool loadFunctions(
    SRNssModuleHandle module,
    SRFfxApiFunctions *outFunctions) {
    if (!module || !outFunctions) {
        return false;
    }

    outFunctions->createContext =
        reinterpret_cast<PfnFfxCreateContext>(getSymbol(module, "ffxCreateContext"));
    outFunctions->destroyContext =
        reinterpret_cast<PfnFfxDestroyContext>(getSymbol(module, "ffxDestroyContext"));
    outFunctions->query =
        reinterpret_cast<PfnFfxQuery>(getSymbol(module, "ffxQuery"));
    outFunctions->dispatch =
        reinterpret_cast<PfnFfxDispatch>(getSymbol(module, "ffxDispatch"));

    return outFunctions->createContext &&
           outFunctions->destroyContext &&
           outFunctions->query &&
           outFunctions->dispatch;
}

SRReturnCode fromFfxReturnCode(ffxReturnCode_t code) {
    switch (code) {
        case FFX_API_RETURN_OK:
            return SR_RETURN_CODE_OK;
        case FFX_API_RETURN_ERROR_PARAMETER:
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        case FFX_API_RETURN_NO_PROVIDER:
            return SR_RETURN_CODE_UNSUPPORTED;
        case FFX_API_RETURN_ERROR_UNKNOWN_DESCTYPE:
            return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
        default:
            return SR_RETURN_CODE_ERROR;
    }
}

const char *defaultNssDllPath() {
#if defined(_WIN32)
    return "ngsdk_windows_x64.dll";
#else
    return "libngsdk_linux_x64.so";
#endif
}

const SRContextExtraParam *findParam(
    const SRCreateUpscaleContextDesc *desc,
    const char *name) {
    return desc ? srFindParam(&desc->extraParams, name) : nullptr;
}

bool getVulkanQueue(
    const SRCreateUpscaleContextDesc *desc,
    VkQueue *outQueue,
    uint32_t *outQueueFamilyIndex) {
    if (!desc || !outQueue || !outQueueFamilyIndex) {
        return false;
    }

    *outQueue = VK_NULL_HANDLE;
    *outQueueFamilyIndex = 0;

    const SRContextExtraParam *queueParam = findParam(desc, "NSS_VK_QUEUE");
    if (queueParam && queueParam->valueType == SR_PARAM_VALUE_TYPE_POINTER) {
        *outQueue = reinterpret_cast<VkQueue>(queueParam->value.ptrValue);
    }

    const SRContextExtraParam *familyParam =
        findParam(desc, "NSS_VK_QUEUE_FAMILY_INDEX");
    if (familyParam && familyParam->valueType == SR_PARAM_VALUE_TYPE_UINT32) {
        *outQueueFamilyIndex = familyParam->value.uint32Value;
    }

    return *outQueue != VK_NULL_HANDLE;
}

}  // namespace

extern "C" {

SR_API SRReturnCode srArmNSSVkCreateUpscaleContext(
    SRUpscaleContext *context,
    const SRCreateUpscaleContextDesc *desc) {
    if (!context || !desc) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    if (desc->renderApiType != SR_RENDER_API_TYPE_VULKAN) {
        reportText(desc, SR_MESSAGE_TYPE_ERROR, "NSS only supports Vulkan.");
        return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
    }

    VkQueue queue = VK_NULL_HANDLE;
    uint32_t queueFamilyIndex = 0;
    if (!getVulkanQueue(desc, &queue, &queueFamilyIndex)) {
        reportText(
            desc,
            SR_MESSAGE_TYPE_ERROR,
            "NSS requires the Vulkan queue and queue family extra parameters "
            "NSS_VK_QUEUE and NSS_VK_QUEUE_FAMILY_INDEX.");
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    const SRContextExtraParam *dllPathParam = findParam(desc, "NSS_DLL_PATH");
    const char *dllPath = defaultNssDllPath();
    if (dllPathParam &&
        dllPathParam->valueType == SR_PARAM_VALUE_TYPE_STRING &&
        dllPathParam->value.stringValue &&
        *dllPathParam->value.stringValue) {
        dllPath = dllPathParam->value.stringValue;
    }

    SRNssModuleHandle module = loadModule(dllPath);
    if (!module) {
        std::wstring message = L"Failed to load NSS FFX API library: ";
        const std::wstring widePath = utf8ToWide(dllPath);
        message += widePath.empty() ? L"<invalid path>" : widePath;
        report(desc, SR_MESSAGE_TYPE_ERROR, message);
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    auto privateData = std::unique_ptr<SRNssPrivateData>(
        new (std::nothrow) SRNssPrivateData());
    if (!privateData) {
        unloadModule(module);
        return SR_RETURN_CODE_ERROR;
    }

    privateData->module = module;
    privateData->messageCallback = desc->messageCallback;
    privateData->dispatchFlags =
        (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEBUG)
            ? FFX_API_NSS_DISPATCH_FLAG_ENABLE_DEBUG_CHECKING
            : 0;
    if (!loadFunctions(module, &privateData->functions)) {
        reportText(
            desc,
            SR_MESSAGE_TYPE_ERROR,
            "NSS FFX API library is missing one or more generic FFX API exports.");
        unloadModule(module);
        return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
    }

    context->desc = *desc;
    context->userContext = privateData.release();
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srArmNSSVkInitUpscaleContext(SRUpscaleContext *context) {
    if (!context || !context->userContext) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    auto *privateData = static_cast<SRNssPrivateData *>(context->userContext);
    if (privateData->contextCreated) {
        return SR_RETURN_CODE_OK;
    }

    const SRCreateUpscaleContextDesc *desc = &context->desc;
    VkQueue queue = VK_NULL_HANDLE;
    uint32_t queueFamilyIndex = 0;
    if (!getVulkanQueue(desc, &queue, &queueFamilyIndex)) {
        reportText(desc, SR_MESSAGE_TYPE_ERROR, "NSS Vulkan queue parameters are missing.");
        return SR_RETURN_CODE_INVALID_ARGUMENT;
    }

    ffxCreateBackendVKDesc backendDesc = {};
    backendDesc.header.type = FFX_API_CREATE_CONTEXT_DESC_TYPE_BACKEND_VK;
    backendDesc.vkDevice = desc->renderDeviceInfo.vulkan.device;
    backendDesc.vkPhysicalDevice = desc->renderDeviceInfo.vulkan.physicalDevice;
    backendDesc.vkInstance = desc->renderDeviceInfo.vulkan.instance;
    backendDesc.vkDeviceProcAddr =
        reinterpret_cast<PFN_vkGetDeviceProcAddr>(
            desc->renderDeviceInfo.vulkan.deviceProcAddr);
    backendDesc.vkGetInstanceProcAddr =
        reinterpret_cast<PFN_vkGetInstanceProcAddr>(
            desc->renderDeviceInfo.vulkan.instanceProcAddr);
    backendDesc.vkQueue = queue;
    backendDesc.queueFamilyIndex = queueFamilyIndex;

    ffxApiCreateContextDescNss nssDesc = {};
    nssDesc.header.type = FFX_API_CREATE_CONTEXT_DESC_TYPE_NSS;
    nssDesc.header.pNext = &backendDesc.header;
    nssDesc.flags =
        FFX_API_NSS_CONTEXT_FLAG_QUANTIZED |
        FFX_API_NSS_CONTEXT_FLAG_MANAGE_HISTORY |
        // The Arm SDK currently requires HDR input for NSS.
        FFX_API_NSS_CONTEXT_FLAG_HIGH_DYNAMIC_RANGE;
    if (desc->flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEPTH_INVERTED) {
        nssDesc.flags |= FFX_API_NSS_CONTEXT_FLAG_DEPTH_INVERTED;
    }
    // NSS currently has no SR-side auto-exposure or jittered-motion-vector
    // cancellation equivalent. Those two SR flags are intentionally ignored.
    nssDesc.maxRenderSize = {
        desc->renderSize.x,
        desc->renderSize.y,
    };
    nssDesc.maxUpscaleSize = {
        desc->upscaledSize.x,
        desc->upscaledSize.y,
    };
    nssDesc.fpMessage = ffxMessageBridge;
    nssDesc.qualityMode = FFX_API_NSS_SHADER_QUALITY_MODE_QUALITY;

    g_activeMessageCallback = privateData->messageCallback;

    const ffxReturnCode_t code = privateData->functions.createContext(
        &privateData->context,
        &nssDesc.header,
        nullptr);
    if (code != FFX_API_RETURN_OK) {
        reportText(desc, SR_MESSAGE_TYPE_ERROR, "FFX API NSS context creation failed.");
        return fromFfxReturnCode(code);
    }

    privateData->contextCreated = true;
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srArmNSSVkDestroyUpscaleContext(SRUpscaleContext *context) {
    if (!context || !context->userContext) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    auto *privateData = static_cast<SRNssPrivateData *>(context->userContext);
    if (privateData->contextCreated) {
        const ffxReturnCode_t code = privateData->functions.destroyContext(
            &privateData->context,
            nullptr);
        if (code != FFX_API_RETURN_OK) {
            reportText(
                &context->desc,
                SR_MESSAGE_TYPE_ERROR,
                "FFX API NSS context destruction failed.");
            return fromFfxReturnCode(code);
        }
        privateData->context = nullptr;
        privateData->contextCreated = false;
    }

    const bool unloaded = unloadModule(privateData->module);
    privateData->module = nullptr;
    g_activeMessageCallback = nullptr;
    delete privateData;
    context->userContext = nullptr;
    return unloaded ? SR_RETURN_CODE_OK : SR_RETURN_CODE_UNEXPECTED_ERROR;
}

SR_API SRReturnCode srArmNSSVkQueryUpscale(
    SRUpscaleContext *context,
    SRUpscaleContextQueryResult *result,
    SRUpscaleContextQueryType queryType) {
    if (!context || !context->userContext || !result) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    auto *privateData = static_cast<SRNssPrivateData *>(context->userContext);
    switch (queryType) {
        case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO: {
            uint64_t versionCount = 1;
            uint64_t versionId = 0;
            ffxQueryDescGetVersions query = {};
            query.header.type = FFX_API_QUERY_DESC_TYPE_GET_VERSIONS;
            query.createDescType = FFX_API_CREATE_CONTEXT_DESC_TYPE_NSS;
            query.outputCount = &versionCount;
            query.versionIds = &versionId;

            const ffxReturnCode_t code = privateData->functions.query(
                nullptr,
                &query.header);
            if (code != FFX_API_RETURN_OK) {
                return fromFfxReturnCode(code);
            }
            if (versionCount == 0) {
                return SR_RETURN_CODE_UNSUPPORTED;
            }

            static thread_local SRQueryVersionResult outResult = {};
            outResult.versionId = versionId;
            outResult.versionNumber = versionId & 0xffffffffu;
            result->data = &outResult;
            return SR_RETURN_CODE_OK;
        }
        case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO: {
            // The NSS provider does not expose a GPU-memory query descriptor.
            static thread_local SRQueryGpuMemoryResult outResult = {};
            outResult.gpuMemory = 0;
            result->data = &outResult;
            return SR_RETURN_CODE_OK;
        }
        case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE: {
            static thread_local SRQueryAvailabilityResult outResult = {};
            outResult.isAvailable = privateData->contextCreated;
            result->data = &outResult;
            return SR_RETURN_CODE_OK;
        }
        default:
            return SR_RETURN_CODE_INVALID_ARGUMENT;
    }
}

SR_API SRReturnCode srArmNSSVkDispatchUpscale(
    SRUpscaleContext *context,
    const SRDispatchUpscaleDesc *desc) {
    if (!context || !context->userContext || !desc) {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    auto *privateData = static_cast<SRNssPrivateData *>(context->userContext);
    if (!privateData->contextCreated) {
        return SR_RETURN_CODE_UNSUPPORTED;
    }

    ffxApiDispatchDescNss dispatchDesc = {};
    dispatchDesc.header.type = FFX_API_DISPATCH_DESC_TYPE_NSS;
    dispatchDesc.commandList = reinterpret_cast<void *>(
        desc->commandList.apiCommandBuffer.vulkan.commandBuffer);
    if (desc->color.exist) {
        dispatchDesc.color = srTextureResourceToFfxResource(&desc->color);
    }
    if (desc->depth.exist) {
        dispatchDesc.depth = srTextureResourceToFfxResource(&desc->depth);
    }
    if (desc->motionVectors.exist) {
        dispatchDesc.motionVectors = srTextureResourceToFfxResource(&desc->motionVectors);
    }
    // NSS uses a scalar exposure value in its dispatch descriptor. The SR API
    // exposure texture is still converted and owned by the interop layer, but
    // must not be assigned to the NSS descriptor (its ABI has no exposure
    // resource field).
    if (desc->output.exist) {
        dispatchDesc.output = srTextureResourceToFfxOutputResource(&desc->output);
    }

    dispatchDesc.jitterOffset = {
        desc->jitterOffset.x,
        desc->jitterOffset.y,
    };
    dispatchDesc.motionVectorScale = {
        desc->motionVectorScale.x,
        desc->motionVectorScale.y,
    };
    dispatchDesc.renderSize = {
        desc->renderSize.x,
        desc->renderSize.y,
    };
    dispatchDesc.upscaleSize = {
        desc->upscaleSize.x,
        desc->upscaleSize.y,
    };
    dispatchDesc.frameTimeDelta = desc->frameTimeDelta;
    // NSS interprets this as the HDR tonemapping exposure.  Unlike FSR3's
    // preExposure, zero is meaningful here: the NSS SDK uses it to select its
    // documented default exposure (e^2).  Do not replace that fallback with
    // 1.0, which compresses the model input and produces a soft image.
    dispatchDesc.exposure = desc->preExposure;
    dispatchDesc.reset = desc->reset;
    dispatchDesc.cameraNear = desc->cameraNear;
    dispatchDesc.cameraFar = desc->cameraFar;
    dispatchDesc.cameraFovAngleVertical = desc->cameraFovAngleVertical;
    dispatchDesc.flags = privateData->dispatchFlags;

    g_activeMessageCallback = privateData->messageCallback;
    return fromFfxReturnCode(privateData->functions.dispatch(
        &privateData->context,
        reinterpret_cast<ffxDispatchDescHeader*>(&dispatchDesc)));
}

SR_API SRReturnCode srArmNSSVkShutdown() {
    return SR_RETURN_CODE_OK;
}

}  // extern "C"
