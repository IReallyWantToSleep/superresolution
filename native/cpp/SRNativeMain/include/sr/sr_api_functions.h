#pragma once
#include "sr_api_enums.h"
#include "sr_api_structs.h"
#include "glad/gl.h"
#include <string>

#if defined(_WIN32)
#define SR_API __declspec(dllexport)
#else
#define SR_API
#endif

#ifdef __cplusplus
extern "C"
{
#endif
    SR_API SRReturnCode srCreateUpscaleContext(
        SRUpscaleContext *outContext,
        SRUpscaleProvider *provider,
        const SRCreateUpscaleContextDesc *desc);

    SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context);

    SR_API SRReturnCode srQueryUpscaleContext(
        SRUpscaleContext *context,
        SRUpscaleContextQueryResult *outResult,
        SRUpscaleContextQueryType queryType);

    SR_API SRReturnCode srInitUpscaleContext(
        SRUpscaleContext *context);

    SR_API SRReturnCode srDispatchUpscale(
        SRUpscaleContext *context,
        const SRDispatchUpscaleDesc *desc);

    SR_API SRReturnCode srGetUpscaleProvider(
        SRUpscaleProvider *outProvider,
        uint64_t providerId);

    SR_API SRReturnCode srLoadUpscaleProvidersFromLibrary(
        const std::string &libPath,
        const std::string &getProvidersFuncName,      // SRUpscaleProviderSupplierFunc
        const std::string &getProvidersCountFuncName, // SRUpscaleProviderSupplierCountFunc
        SRMessageCallback messageCallback);

    SR_API GLenum srTextureFormatToGlFormat(SRTextureFormat fmt);
    SR_API VkFormat srTextureFormatToVkFormat(SRTextureFormat fmt);

    SR_API SRReturnCode srParamsSetBool(
        SRContextExtraParams *params,
        const char *name,
        bool value);

    SR_API SRReturnCode srParamsSetInt32(
        SRContextExtraParams *param,
        const char *name,
        int32_t value);

    SR_API SRReturnCode srParamsSetUint32(
        SRContextExtraParams *param,
        const char *name,
        uint32_t value);

    SR_API SRReturnCode srParamsSetFloat(
        SRContextExtraParams *param,
        const char *name,
        float value);

    SR_API SRReturnCode srParamsSetDouble(
        SRContextExtraParams *param,
        const char *name,
        double value);

    SR_API SRReturnCode srParamsSetString(
        SRContextExtraParams *param,
        const char *name,
        const char *value);

    SR_API SRReturnCode srParamsSetPointer(
        SRContextExtraParams *param,
        const char *name,
        void *value);

    SR_API const SRContextExtraParam *srFindParam(
        const SRContextExtraParams *params,
        const char *name);

    SR_API SRReturnCode srParamsGetBool(
        const SRContextExtraParams *params,
        const char *name,
        bool *outValue,
        bool defaultValue);

    SR_API SRReturnCode srParamsGetInt32(
        const SRContextExtraParams *params,
        const char *name,
        int32_t *outValue,
        int32_t defaultValue);

    SR_API SRReturnCode srParamsGetUint32(
        const SRContextExtraParams *params,
        const char *name,
        uint32_t *outValue,
        uint32_t defaultValue);

    SR_API SRReturnCode srParamsGetFloat(
        const SRContextExtraParams *params,
        const char *name,
        float *outValue,
        float defaultValue);

    SR_API SRReturnCode srParamsGetDouble(
        const SRContextExtraParams *params,
        const char *name,
        double *outValue,
        double defaultValue);

    SR_API SRReturnCode srParamsGetString(
        const SRContextExtraParams *params,
        const char *name,
        const char **outValue,
        const char *defaultValue);

    SR_API SRReturnCode srParamsGetPointer(
        const SRContextExtraParams *params,
        const char *name,
        void **outValue);

#ifdef __cplusplus
}
#endif
