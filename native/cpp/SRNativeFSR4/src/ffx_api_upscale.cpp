#include "sr/fsr4/ffx_api_upscale.h"

#if defined(ON_WIN64)

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

#include <ffx_api.h>
#include <ffx_api_types.h>
#include <dx12/ffx_api_dx12.h>
#include <ffx_upscale.h>

#include <cwchar>
#include <new>
#include <string>

namespace {
    struct SRFfxApiFunctions {
        PfnFfxCreateContext createContext;
        PfnFfxDestroyContext destroyContext;
        PfnFfxQuery query;
        PfnFfxDispatch dispatch;
    };

    struct SRFfxApiPrivateData {
        HMODULE module = nullptr;
        SRFfxApiFunctions functions = {};
        ffxContext context = nullptr;
        ffxCreateContextDescUpscale createDesc = {};
        ffxCreateBackendDX12Desc backendDesc = {};
        ffxCreateContextDescUpscaleVersion versionDesc = {};
    };

    std::wstring utf8ToWide(const char *value) {
        if (!value || !*value) {
            return {};
        }
        const int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, nullptr, 0);
        if (length <= 0) {
            return {};
        }
        std::wstring result(static_cast<size_t>(length), L'\0');
        MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, result.data(), length);
        result.pop_back();
        return result;
    }

    void report(
        const SRCreateUpscaleContextDesc *desc,
        SRMessageType type,
        const wchar_t *message) {
        if (desc && desc->messageCallback) {
            desc->messageCallback(type, message);
        }
    }

    bool loadFunctions(HMODULE module, SRFfxApiFunctions *outFunctions) {
        outFunctions->createContext = reinterpret_cast<PfnFfxCreateContext>(
            GetProcAddress(module, "ffxCreateContext"));
        outFunctions->destroyContext = reinterpret_cast<PfnFfxDestroyContext>(
            GetProcAddress(module, "ffxDestroyContext"));
        outFunctions->query = reinterpret_cast<PfnFfxQuery>(
            GetProcAddress(module, "ffxQuery"));
        outFunctions->dispatch = reinterpret_cast<PfnFfxDispatch>(
            GetProcAddress(module, "ffxDispatch"));
        return outFunctions->createContext &&
               outFunctions->destroyContext &&
               outFunctions->query &&
               outFunctions->dispatch;
    }

    uint32_t toFfxCreateFlags(uint32_t flags) {
        uint32_t result = 0;
        if (flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEBUG) {
            result |= FFX_UPSCALE_ENABLE_DEBUG_CHECKING;
        }
        if (flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_AUTO_EXPOSURE) {
            result |= FFX_UPSCALE_ENABLE_AUTO_EXPOSURE;
        }
        if (flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEPTH_INVERTED) {
            result |= FFX_UPSCALE_ENABLE_DEPTH_INVERTED;
        }
        if (flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_MOTION_VECTORS_JITTERED) {
            result |= FFX_UPSCALE_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION;
        }
        if (flags & SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_HDR) {
            result |= FFX_UPSCALE_ENABLE_HIGH_DYNAMIC_RANGE;
        }
        return result;
    }

    FfxApiSurfaceFormat toFfxSurfaceFormat(SRTextureFormat format) {
        switch (format) {
            case (SR_TEXTURE_FORMAT_UNKNOWN):
                return FFX_API_SURFACE_FORMAT_UNKNOWN;
            case (SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS):
                return FFX_API_SURFACE_FORMAT_R32G32B32A32_TYPELESS;
            case (SR_TEXTURE_FORMAT_R32G32B32A32_UINT):
                return FFX_API_SURFACE_FORMAT_R32G32B32A32_UINT;
            case (SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT):
                return FFX_API_SURFACE_FORMAT_R32G32B32A32_FLOAT;
            case (SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT):
                return FFX_API_SURFACE_FORMAT_R16G16B16A16_FLOAT;
            case (SR_TEXTURE_FORMAT_R32G32B32_FLOAT):
                return FFX_API_SURFACE_FORMAT_R32G32B32_FLOAT;
            case (SR_TEXTURE_FORMAT_R32G32_FLOAT):
                return FFX_API_SURFACE_FORMAT_R32G32_FLOAT;
            case (SR_TEXTURE_FORMAT_R8_UINT):
                return FFX_API_SURFACE_FORMAT_R8_UINT;
            case (SR_TEXTURE_FORMAT_R32_UINT):
                return FFX_API_SURFACE_FORMAT_R32_UINT;
            case (SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS):
                return FFX_API_SURFACE_FORMAT_R8G8B8A8_UNORM;
            case (SR_TEXTURE_FORMAT_R8G8B8A8_UNORM):
                return FFX_API_SURFACE_FORMAT_R8G8B8A8_UNORM;
            case (SR_TEXTURE_FORMAT_R8G8B8A8_SNORM):
                return FFX_API_SURFACE_FORMAT_R8G8B8A8_SNORM;
            case (SR_TEXTURE_FORMAT_R8G8B8A8_SRGB):
                return FFX_API_SURFACE_FORMAT_R8G8B8A8_SRGB;
            case (SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS):
                return FFX_API_SURFACE_FORMAT_B8G8R8A8_UNORM;
            case (SR_TEXTURE_FORMAT_B8G8R8A8_UNORM):
                return FFX_API_SURFACE_FORMAT_B8G8R8A8_UNORM;
            case (SR_TEXTURE_FORMAT_B8G8R8A8_SRGB):
                return FFX_API_SURFACE_FORMAT_B8G8R8A8_SRGB;
            case (SR_TEXTURE_FORMAT_R11G11B10_FLOAT):
                return FFX_API_SURFACE_FORMAT_R11G11B10_FLOAT;
            case (SR_TEXTURE_FORMAT_R10G10B10A2_UNORM):
                return FFX_API_SURFACE_FORMAT_R10G10B10A2_UNORM;
            case (SR_TEXTURE_FORMAT_R16G16_FLOAT):
                return FFX_API_SURFACE_FORMAT_R16G16_FLOAT;
            case (SR_TEXTURE_FORMAT_R16G16_UINT):
                return FFX_API_SURFACE_FORMAT_R16G16_UINT;
            case (SR_TEXTURE_FORMAT_R16G16_SINT):
                return FFX_API_SURFACE_FORMAT_R16G16_SINT;
            case (SR_TEXTURE_FORMAT_R16_FLOAT):
                return FFX_API_SURFACE_FORMAT_R16_FLOAT;
            case (SR_TEXTURE_FORMAT_R16_UINT):
                return FFX_API_SURFACE_FORMAT_R16_UINT;
            case (SR_TEXTURE_FORMAT_R16_UNORM):
                return FFX_API_SURFACE_FORMAT_R16_UNORM;
            case (SR_TEXTURE_FORMAT_R16_SNORM):
                return FFX_API_SURFACE_FORMAT_R16_SNORM;
            case (SR_TEXTURE_FORMAT_R8_UNORM):
                return FFX_API_SURFACE_FORMAT_R8_UNORM;
            case (SR_TEXTURE_FORMAT_R8G8_UNORM):
                return FFX_API_SURFACE_FORMAT_R8G8_UNORM;
            case (SR_TEXTURE_FORMAT_R8G8_UINT):
                return FFX_API_SURFACE_FORMAT_R8G8_UINT;
            case (SR_TEXTURE_FORMAT_R32_FLOAT):
                return FFX_API_SURFACE_FORMAT_R32_FLOAT;
            case (SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP):
                return FFX_API_SURFACE_FORMAT_R9G9B9E5_SHAREDEXP;
            case (SR_TEXTURE_FORMAT_D32_SFLOAT):
                return FFX_API_SURFACE_FORMAT_R32_FLOAT;
            default:
                return FFX_API_SURFACE_FORMAT_UNKNOWN;
        }
    }


    FfxApiResource toFfxResource(
        const SRTextureResource &resource,
        SRResourceStates defaultState) {
        if (!resource.exist) {
            return {};
        }

        FfxApiResource result = {};
        result.resource = resource.handle;
        result.description.type = FFX_API_RESOURCE_TYPE_TEXTURE2D;
        result.description.format = toFfxSurfaceFormat(resource.desc.format);
        result.description.width = resource.desc.width;
        result.description.height = resource.desc.height;
        result.description.depth = 1;
        result.description.mipCount = resource.desc.mipmapCount;
        result.description.flags = FFX_API_RESOURCE_FLAGS_NONE;
        result.description.usage = static_cast<uint32_t>(resource.desc.usage);
        result.state = resource.state != 0
                           ? static_cast<uint32_t>(resource.state)
                           : static_cast<uint32_t>(defaultState);
        return result;
    }

    SRReturnCode fromFfxReturnCode(ffxReturnCode_t code) {
        switch (code) {
            case FFX_API_RETURN_OK:
                return SR_RETURN_CODE_OK;
            case FFX_API_RETURN_ERROR_PARAMETER:
                return SR_RETURN_CODE_INVALID_ARGUMENT;
            case FFX_API_RETURN_NO_PROVIDER:
            case FFX_API_RETURN_PROVIDER_NO_SUPPORT_NEW_DESCTYPE:
                return SR_RETURN_CODE_UNSUPPORTED;
            default:
                return SR_RETURN_CODE_ERROR;
        }
    }
}

extern "C" {
    SR_API SRReturnCode srFfxApiCreateUpscaleContext(
        SRUpscaleContext *context,
        const SRCreateUpscaleContextDesc *desc) {
        if (!context || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        if (desc->renderApiType != SR_RENDER_API_TYPE_D3D12) {
            report(desc, SR_MESSAGE_TYPE_ERROR, L"FFX API upscaling requires D3D12.");
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }
        if (!desc->renderDeviceInfo.d3d12.device) {
            report(desc, SR_MESSAGE_TYPE_ERROR, L"FFX API upscaling requires an ID3D12Device.");
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        const char *dllPath = nullptr;
        srParamsGetString(
            &desc->extraParams,
            SR_FFX_API_DLL_PATH_PARAM,
            &dllPath,
            "amd_fidelityfx_upscaler_dx12.dll");
        std::wstring wideDllPath = utf8ToWide(dllPath);
        if (wideDllPath.empty()) {
            report(desc, SR_MESSAGE_TYPE_ERROR, L"The FFX API DLL path is not valid UTF-8.");
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        HMODULE module = LoadLibraryExW(
            wideDllPath.c_str(),
            nullptr,
            LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR | LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
        if (!module && std::wcschr(wideDllPath.c_str(), L'\\') == nullptr &&
            std::wcschr(wideDllPath.c_str(), L'/') == nullptr) {
            // LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR requires a qualified path.
            module = LoadLibraryExW(
                wideDllPath.c_str(),
                nullptr,
                LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
        }
        if (!module) {
            report(desc, SR_MESSAGE_TYPE_ERROR, L"Could not load amd_fidelityfx_upscaler_dx12.dll.");
            return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
        }

        auto *privateData = new(std::nothrow) SRFfxApiPrivateData();
        if (!privateData) {
            FreeLibrary(module);
            return SR_RETURN_CODE_ERROR;
        }
        privateData->module = module;

        if (!loadFunctions(module, &privateData->functions)) {
            report(desc, SR_MESSAGE_TYPE_ERROR, L"The FFX API DLL is missing required exports.");
            FreeLibrary(module);
            delete privateData;
            return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
        }

        privateData->createDesc.header.type = FFX_API_CREATE_CONTEXT_DESC_TYPE_UPSCALE;
        privateData->createDesc.header.pNext = &privateData->backendDesc.header;
        privateData->createDesc.flags = toFfxCreateFlags(desc->flags);
        privateData->createDesc.maxRenderSize = {desc->renderSize.x, desc->renderSize.y};
        privateData->createDesc.maxUpscaleSize = {desc->upscaledSize.x, desc->upscaledSize.y};
        privateData->createDesc.fpMessage = reinterpret_cast<ffxApiMessage>(desc->messageCallback);

        privateData->backendDesc.header.type = FFX_API_CREATE_CONTEXT_DESC_TYPE_BACKEND_DX12;
        privateData->backendDesc.header.pNext = &privateData->versionDesc.header;
        privateData->backendDesc.device = static_cast<ID3D12Device *>(
            desc->renderDeviceInfo.d3d12.device);

        privateData->versionDesc.header.type = FFX_API_CREATE_CONTEXT_DESC_TYPE_UPSCALE_VERSION;
        privateData->versionDesc.header.pNext = nullptr;
        privateData->versionDesc.version = FFX_UPSCALER_VERSION;

        const ffxReturnCode_t code = privateData->functions.createContext(
            &privateData->context,
            &privateData->createDesc.header,
            nullptr);
        if (code != FFX_API_RETURN_OK) {
            const std::wstring message =
                    L"FFX API failed to create an upscaling context. Code=" +
                    std::to_wstring(code);
            report(desc, SR_MESSAGE_TYPE_ERROR, message.c_str());
            FreeLibrary(module);
            delete privateData;
            return fromFfxReturnCode(code);
        }

        context->desc = *desc;
        context->userContext = privateData;
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxApiInitUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srFfxApiDestroyUpscaleContext(SRUpscaleContext *context) {
        if (!context || !context->userContext) {
            return SR_RETURN_CODE_NULL_POINTER;
        }

        auto *privateData = static_cast<SRFfxApiPrivateData *>(context->userContext);
        const ffxReturnCode_t code = privateData->functions.destroyContext(
            &privateData->context,
            nullptr);
        FreeLibrary(privateData->module);
        delete privateData;
        context->userContext = nullptr;
        return fromFfxReturnCode(code);
    }

    SR_API SRReturnCode srFfxApiQueryUpscale(
        SRUpscaleContext *context,
        SRUpscaleContextQueryResult *result,
        SRUpscaleContextQueryType queryType) {
        if (!context || !context->userContext || !result) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        auto *privateData = static_cast<SRFfxApiPrivateData *>(context->userContext);

        switch (queryType) {
            case SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO: {
                ffxQueryGetProviderVersion query = {};
                query.header.type = FFX_API_QUERY_DESC_TYPE_GET_PROVIDER_VERSION;
                const ffxReturnCode_t code = privateData->functions.query(
                    &privateData->context,
                    &query.header);
                if (code != FFX_API_RETURN_OK) {
                    return fromFfxReturnCode(code);
                }
                static thread_local SRQueryVersionResult versionResult = {};
                versionResult.versionId = query.versionId;
                versionResult.versionNumber = FFX_UPSCALER_VERSION;
                result->type = queryType;
                result->data = &versionResult;
                return SR_RETURN_CODE_OK;
            }
            case SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO: {
                static thread_local SRQueryGpuMemoryResult memoryResult = {};
                FfxApiEffectMemoryUsage usage = {};
                ffxQueryDescUpscaleGetGPUMemoryUsage query = {};
                query.header.type =
                        FFX_API_QUERY_DESC_TYPE_UPSCALE_GPU_MEMORY_USAGE;
                query.gpuMemoryUsageUpscaler = &usage;
                const ffxReturnCode_t code = privateData->functions.query(
                    &privateData->context,
                    &query.header);
                if (code != FFX_API_RETURN_OK) {
                    return fromFfxReturnCode(code);
                }
                memoryResult.gpuMemory = usage.totalUsageInBytes;
                result->type = queryType;
                result->data = &memoryResult;
                return SR_RETURN_CODE_OK;
            }
            case SR_UPSCALE_CONTEXT_QUERY_AVAILABLE: {
                static thread_local SRQueryAvailabilityResult availabilityResult = {};
                availabilityResult.isAvailable = true;
                result->type = queryType;
                result->data = &availabilityResult;
                return SR_RETURN_CODE_OK;
            }
            default:
                return SR_RETURN_CODE_UNSUPPORTED;
        }
    }

    SR_API SRReturnCode srFfxApiDispatchUpscale(
        SRUpscaleContext *context,
        const SRDispatchUpscaleDesc *desc) {
        if (!context || !context->userContext || !desc) {
            return SR_RETURN_CODE_NULL_POINTER;
        }
        if (desc->commandList.renderApiType != SR_RENDER_API_TYPE_D3D12) {
            return SR_RETURN_CODE_UNSUPPORTED_RENDER_API;
        }
        if (!desc->commandList.apiCommandBuffer.d3d12.commandList) {
            return SR_RETURN_CODE_INVALID_ARGUMENT;
        }

        auto *privateData = static_cast<SRFfxApiPrivateData *>(context->userContext);
        ffxDispatchDescUpscale dispatchDesc = {};
        dispatchDesc.header.type = FFX_API_DISPATCH_DESC_TYPE_UPSCALE;
        dispatchDesc.commandList = desc->commandList.apiCommandBuffer.d3d12.commandList;
        dispatchDesc.color = toFfxResource(desc->color, SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.depth = toFfxResource(desc->depth, SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.motionVectors = toFfxResource(desc->motionVectors, SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.exposure = toFfxResource(desc->exposure, SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.reactive = toFfxResource(desc->reactive, SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.transparencyAndComposition = toFfxResource(
            desc->transparencyAndComposition,
            SR_RESOURCE_STATE_COMPUTE_READ);
        dispatchDesc.output = toFfxResource(desc->output, SR_RESOURCE_STATE_UNORDERED_ACCESS);
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

        return fromFfxReturnCode(privateData->functions.dispatch(
            &privateData->context,
            &dispatchDesc.header));
    }

    SR_API SRReturnCode srFfxApiShutdown() {
        return SR_RETURN_CODE_OK;
    }

    SR_API SRUpscaleContextCallbacks srGetFfxApiUpscaleCallbacks() {
        static SRUpscaleContextCallbacks callbacks = {
            .pCreate = srFfxApiCreateUpscaleContext,
            .pInit = srFfxApiInitUpscaleContext,
            .pDestroy = srFfxApiDestroyUpscaleContext,
            .pQuery = reinterpret_cast<SRQueryFunc>(srFfxApiQueryUpscale),
            .pDispatchUpscale = srFfxApiDispatchUpscale,
            .pShutdown = srFfxApiShutdown,
        };
        return callbacks;
    }
}

#endif