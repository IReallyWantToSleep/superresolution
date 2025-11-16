#include "sr/sr_api.h"
#include <mutex>
#include <vector>

#ifdef ON_WIN64
#include <windows.h>
#include <string>
#include <iostream>
#elif defined(ON_LINUX64)
#include <dlfcn.h>
#include <string>
#include <iostream>
#include <codecvt>
#include <locale>
#endif

#include <unordered_set>

static std::unordered_set<std::string> g_loadedLibraries;

static std::vector<SRUpscaleProvider> g_srLoadedUpscaleProviders;
static std::mutex g_providerMutex;

SR_API SRReturnCode srGetUpscaleProvider(
    SRUpscaleProvider *outProvider,
    uint64_t providerId)
{
    if (!outProvider)
    {
        return SR_RETURN_CODE_NULL_POINTER;
    }

    std::lock_guard<std::mutex> lock(g_providerMutex);

    for (const auto &provider : g_srLoadedUpscaleProviders)
    {
        if (provider.providerId == providerId)
        {
            *outProvider = provider;
            return SR_RETURN_CODE_OK;
        }
    }

    return SR_RETURN_CODE_CANNOT_FIND_PROVIDER;
}

SR_API SRReturnCode srCreateUpscaleContext(
    SRUpscaleContext *outContext,
    SRUpscaleProvider *provider,
    const SRCreateUpscaleContextDesc *desc)
{
    if (!outContext || !provider || !desc)
    {
        return (SRReturnCode)SR_RETURN_CODE_NULL_POINTER;
    }
    outContext->callbacks = provider->callbacks;
    return provider->callbacks.pCreate(outContext, desc);
}

SR_API SRReturnCode srInitUpscaleContext(
    SRUpscaleContext *context)
{
    if (!context || !context->callbacks.pInit)
    {
        return (SRReturnCode)SR_RETURN_CODE_NULL_POINTER;
    }
    return context->callbacks.pInit(context);
}

SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context)
{
    if (!context || !context->callbacks.pDestroy)
    {
        return (SRReturnCode)SR_RETURN_CODE_NULL_POINTER;
    }
    return context->callbacks.pDestroy(context);
}

SR_API SRReturnCode srQueryUpscaleContext(
    SRUpscaleContext *context,
    SRUpscaleContextQueryResult *outResult,
    SRUpscaleContextQueryType queryType)
{
    if (!context || !outResult || !context->callbacks.pQuery)
    {
        return (SRReturnCode)SR_RETURN_CODE_NULL_POINTER;
    }
    outResult->type = queryType;
    return context->callbacks.pQuery(outResult, context, queryType);
}
SR_API SRReturnCode srDispatchUpscale(
    SRUpscaleContext *context,
    const SRDispatchUpscaleDesc *desc)
{
    if (!context || !desc || !context->callbacks.pDispatchUpscale)
    {
        return (SRReturnCode)SR_RETURN_CODE_NULL_POINTER;
    }
    return context->callbacks.pDispatchUpscale(context, desc);
}
SR_API SRReturnCode srLoadUpscaleProvidersFromLibrary(
    const std::string &libPath,
    const std::string &getProvidersFuncName,
    const std::string &getProvidersCountFuncName,
    SRMessageCallback messageCallback)
{
    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        if (g_loadedLibraries.find(libPath) != g_loadedLibraries.end())
        {
            if (messageCallback)
            {
                messageCallback(SR_MESSAGE_TYPE_INFO, L"Library already loaded, skipping.");
            }
            return SR_RETURN_CODE_OK;
        }
    }

#ifdef ON_WIN64
    // 首先将UTF-8字符串（jstring->GetStringUTFChars+reinterpet_cast->libPath(std::string)）转换为Windows Wide Char.
    // 计算目标缓冲区大小
    size_t wideLen = MultiByteToWideChar(CP_UTF8, 0, libPath.c_str(), -1, NULL, 0);
    // 为0则返回error
    if (wideLen == 0)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to load DLL,libPath is empty.");
        }
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }
    // 否则分配内存并执行实际转换(在Windows上使用Windows API)
    wchar_t *widePath = new wchar_t[wideLen];
    MultiByteToWideChar(CP_UTF8, 0, libPath.c_str(), -1, widePath, wideLen);
    HMODULE dll = LoadLibraryW(widePath);
    // 调用完回收内存（Should we use try{}finally{} here?）
    delete[] widePath;
    if (!dll)
    {
        if (messageCallback)
        {
            std::wstring error = L"Failed to load DLL: ";
            error += std::to_wstring(GetLastError());
            error += L" Path: ";
            error += widePath;
            messageCallback(SR_MESSAGE_TYPE_ERROR, error.c_str());
        }
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    auto getProvidersCount = (SRUpscaleProviderSupplierCountFunc)GetProcAddress(dll, getProvidersCountFuncName.c_str());
    auto getProviders = (SRUpscaleProviderSupplierFunc)GetProcAddress(dll, getProvidersFuncName.c_str());

#elif defined(ON_LINUX64)
    // 路径不用转换，本来就是UTF-8
    // 这个converter用来转换messageCallback
    // FSR为什么要用wchar_t呢
    std::wstring_convert<std::codecvt_utf8<wchar_t>> converter;
    void *handle = dlopen(libPath.c_str(), RTLD_NOW);
    if (!handle)
    {
        if (messageCallback)
        {
            // 这里必须使用wchar_t，以与FSR的CallBack兼容
            std::wstring error = L"Failed to load .so: " + converter.from_bytes(dlerror());
            messageCallback(SR_MESSAGE_TYPE_ERROR, error.c_str());
        }
        return SR_RETURN_CODE_CANNOT_FIND_LIBRARY;
    }

    auto getProvidersCount = (SRUpscaleProviderSupplierCountFunc)dlsym(handle, getProvidersCountFuncName.c_str());
    auto getProviders = (SRUpscaleProviderSupplierFunc)dlsym(handle, getProvidersFuncName.c_str());

#endif

    if (!getProviders || !getProvidersCount)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to resolve provider functions.");
        }
#ifdef ON_WIN64
        FreeLibrary(dll);
#elif defined(ON_LINUX64)
        dlclose(handle);
#endif
        return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
    }

    uint32_t count = 0;
    if (getProvidersCount(&count) != SR_RETURN_CODE_OK || count == 0)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_WARNING, L"No upscale providers found.");
        }
#ifdef ON_WIN64
        FreeLibrary(dll);
#elif defined(ON_LINUX64)
        dlclose(handle);
#endif
        return SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY;
    }

    std::vector<SRUpscaleProvider> providers(count);
    if (getProviders(providers.data()) != SR_RETURN_CODE_OK)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to get providers.");
        }
#ifdef ON_WIN64
        FreeLibrary(dll);
#elif defined(ON_LINUX64)
        dlclose(handle);
#endif
        return SR_RETURN_CODE_UNEXPECTED_ERROR;
    }

    {
        std::lock_guard<std::mutex> lock(g_providerMutex);
        g_srLoadedUpscaleProviders.insert(g_srLoadedUpscaleProviders.end(), providers.begin(), providers.end());
        g_loadedLibraries.insert(libPath);
    }

    if (messageCallback)
    {
        messageCallback(SR_MESSAGE_TYPE_INFO, L"Successfully loaded upscale providers.");
    }

    return SR_RETURN_CODE_OK;
}

SR_API GLenum srTextureFormatToGlFormat(SRTextureFormat fmt)
{
    switch (fmt)
    {
    case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
        return GL_RGBA32F;
    case SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT:
        return GL_RGBA32F;
    case SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT:
        return GL_RGBA16F;
    case SR_TEXTURE_FORMAT_R32G32_FLOAT:
        return GL_RG32F;
    case SR_TEXTURE_FORMAT_R32_UINT:
        return GL_R32UI;
    case SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS:
        return GL_RGBA8;
    case SR_TEXTURE_FORMAT_R8G8B8A8_UNORM:
        return GL_RGBA8;
    case SR_TEXTURE_FORMAT_R11G11B10_FLOAT:
        return GL_R11F_G11F_B10F;
    case SR_TEXTURE_FORMAT_R16G16_FLOAT:
        return GL_RG16F;
    case SR_TEXTURE_FORMAT_R16G16_UINT:
        return GL_RG16UI;
    case SR_TEXTURE_FORMAT_R16_FLOAT:
        return GL_R16F;
    case SR_TEXTURE_FORMAT_R16_UINT:
        return GL_R16UI;
    case SR_TEXTURE_FORMAT_R16_UNORM:
        return GL_R16;
    case SR_TEXTURE_FORMAT_R16_SNORM:
        return GL_R16_SNORM;
    case SR_TEXTURE_FORMAT_R8_UNORM:
        return GL_R8;
    case SR_TEXTURE_FORMAT_R8G8_UNORM:
        return GL_RG8;
    case SR_TEXTURE_FORMAT_R32_FLOAT:
        return GL_R32F;
    case SR_TEXTURE_FORMAT_R8_UINT:
        return GL_R8UI;
    default:
        return 0;
    }
}
SR_API VkFormat srTextureFormatToVkFormat(SRTextureFormat fmt)
{
    switch (fmt)
    {
    case (SR_TEXTURE_FORMAT_UNKNOWN):
        return VK_FORMAT_UNDEFINED;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS):
        return VK_FORMAT_R32G32B32A32_SFLOAT;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_UINT):
        return VK_FORMAT_R32G32B32A32_UINT;
    case (SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT):
        return VK_FORMAT_R32G32B32A32_SFLOAT;
    case (SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT):
        return VK_FORMAT_R16G16B16A16_SFLOAT;
    case (SR_TEXTURE_FORMAT_R32G32B32_FLOAT):
        return VK_FORMAT_R32G32B32_SFLOAT;
    case (SR_TEXTURE_FORMAT_R32G32_FLOAT):
        return VK_FORMAT_R32G32_SFLOAT;
    case (SR_TEXTURE_FORMAT_R8_UINT):
        return VK_FORMAT_R8_UINT;
    case (SR_TEXTURE_FORMAT_R32_UINT):
        return VK_FORMAT_R32_UINT;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS):
        return VK_FORMAT_R8G8B8A8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_UNORM):
        return VK_FORMAT_R8G8B8A8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_SNORM):
        return VK_FORMAT_R8G8B8A8_SNORM;
    case (SR_TEXTURE_FORMAT_R8G8B8A8_SRGB):
        return VK_FORMAT_R8G8B8A8_SRGB;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS):
        return VK_FORMAT_B8G8R8A8_UNORM;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_UNORM):
        return VK_FORMAT_B8G8R8A8_UNORM;
    case (SR_TEXTURE_FORMAT_B8G8R8A8_SRGB):
        return VK_FORMAT_B8G8R8A8_SRGB;
    case (SR_TEXTURE_FORMAT_R11G11B10_FLOAT):
        return VK_FORMAT_B10G11R11_UFLOAT_PACK32;
    case (SR_TEXTURE_FORMAT_R10G10B10A2_UNORM):
        return VK_FORMAT_A2B10G10R10_UNORM_PACK32;
    case (SR_TEXTURE_FORMAT_R16G16_FLOAT):
        return VK_FORMAT_R16G16_SFLOAT;
    case (SR_TEXTURE_FORMAT_R16G16_UINT):
        return VK_FORMAT_R16G16_UINT;
    case (SR_TEXTURE_FORMAT_R16G16_SINT):
        return VK_FORMAT_R16G16_SINT;
    case (SR_TEXTURE_FORMAT_R16_FLOAT):
        return VK_FORMAT_R16_SFLOAT;
    case (SR_TEXTURE_FORMAT_R16_UINT):
        return VK_FORMAT_R16_UINT;
    case (SR_TEXTURE_FORMAT_R16_UNORM):
        return VK_FORMAT_R16_UNORM;
    case (SR_TEXTURE_FORMAT_R16_SNORM):
        return VK_FORMAT_R16_SNORM;
    case (SR_TEXTURE_FORMAT_R8_UNORM):
        return VK_FORMAT_R8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8_UNORM):
        return VK_FORMAT_R8G8_UNORM;
    case (SR_TEXTURE_FORMAT_R8G8_UINT):
        return VK_FORMAT_R8G8_UINT;
    case (SR_TEXTURE_FORMAT_R32_FLOAT):
        return VK_FORMAT_R32_SFLOAT;
    case (SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP):
        return VK_FORMAT_E5B9G9R9_UFLOAT_PACK32;
    default:
        return VK_FORMAT_UNDEFINED;
    }
}