#define SR_MODULES_FSR1_ID 0x8000001
#define SR_MODULES_FSR2_ID 0x8000002
#define SR_MODULES_FSR3_ID 0x8000003
#define SR_MODULES_XeSS_ID 0x8000004
#define SR_MODULES_DLSS_ID 0x8000005
#ifdef ON_WIN64
#include <windows.h>
#include <string>
#include <vector>
#include <iostream>

#include "sr/sr_api.h"
std::vector<SRUpscaleProvider> srLoadUpscaleProvidersFromDll(
    const std::wstring &dllPath,
    const std::string &getProvidersFuncName,
    const std::string &getProvidersCountFuncName,
    SRMessageCallback messageCallback);

#endif