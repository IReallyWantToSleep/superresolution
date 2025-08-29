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
        return SR_RETURN_CODE_ERROR;
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

    return SR_RETURN_CODE_ERROR;
}

SR_API SRReturnCode srCreateUpscaleContext(
    SRUpscaleContext *outContext,
    SRUpscaleProvider *provider,
    const SRCreateUpscaleContextDesc *desc)
{
    if (!outContext || !provider || !desc)
    {
        return (SRReturnCode)SR_RETURN_CODE_ERROR;
    }
    outContext->callbacks = provider->callbacks;
    return provider->callbacks.pCreate(outContext, desc);
}

SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context)
{
    if (!context || !context->callbacks.pDestroy)
    {
        return (SRReturnCode)SR_RETURN_CODE_ERROR;
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
        return (SRReturnCode)SR_RETURN_CODE_ERROR;
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
        return (SRReturnCode)SR_RETURN_CODE_ERROR;
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
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to load DLL.");
        }
        return SR_RETURN_CODE_ERROR;
    }
    // 否则分配内存并执行实际转换(在Windows上使用Windows API)
    wchar_t* widePath = new wchar_t[wideLen];
    MultiByteToWideChar(CP_UTF8, 0, libPath.c_str(), -1, widePath, wideLen);
    HMODULE dll = LoadLibraryW(widePath);
    // 调用完回收内存（Should we use try{}finally{} here?）
    delete[] widePath;
    if (!dll)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"Failed to load DLL.");
        }
        return SR_RETURN_CODE_ERROR;
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
        return SR_RETURN_CODE_ERROR;
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
        return SR_RETURN_CODE_ERROR;
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
        return SR_RETURN_CODE_ERROR;
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
        return SR_RETURN_CODE_ERROR;
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
