#ifdef ON_WIN64
#include <windows.h>
#include <string>
#include <vector>
#include <iostream>

#include "sr/sr_api.h"
#include "sr/sr_modules.h"

using GetProvidersCountFunc = SRReturnCode (*)(uint32_t *);
using GetProvidersFunc = SRReturnCode (*)(SRUpscaleProvider *);

std::vector<SRUpscaleProvider> srLoadUpscaleProvidersFromDll(
    const std::wstring &dllPath,
    const std::string &getProvidersFuncName,
    const std::string &getProvidersCountFuncName,
    SRMessageCallback messageCallback)
{
    std::vector<SRUpscaleProvider> providers;

    HMODULE dll = LoadLibraryW(dllPath.c_str());
    if (!dll)
    {
        if (messageCallback)
        {
            std::wstring msg = L"Failed to load DLL: " + dllPath;
            messageCallback(SR_MESSAGE_TYPE_ERROR, msg.c_str());
        }
        return providers;
    }

    auto getCount = reinterpret_cast<GetProvidersCountFunc>(
        GetProcAddress(dll, getProvidersCountFuncName.c_str()));
    auto getProviders = reinterpret_cast<GetProvidersFunc>(
        GetProcAddress(dll, getProvidersFuncName.c_str()));

    if (!getCount || !getProviders)
    {
        if (messageCallback)
        {
            std::string msg = "Failed to get exported function: " + getProvidersFuncName + " or " + getProvidersCountFuncName;
            std::wstring wmsg(msg.begin(), msg.end());
            messageCallback(SR_MESSAGE_TYPE_ERROR, wmsg.c_str());
        }
        FreeLibrary(dll);
        return providers;
    }

    uint32_t count = 0;
    if (getCount(&count) != SR_RETURN_CODE_OK || count == 0)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"getProvidersCount failed or count is 0");
        }
        FreeLibrary(dll);
        return providers;
    }

    providers.resize(count);
    if (getProviders(providers.data()) != SR_RETURN_CODE_OK)
    {
        if (messageCallback)
        {
            messageCallback(SR_MESSAGE_TYPE_ERROR, L"getProviders failed");
        }
        providers.clear();
    }

    return providers;
}

#endif