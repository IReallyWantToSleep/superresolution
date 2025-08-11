#pragma once
#include "sr_api_types.h"
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


#define SR_MAKE_VERSION(major, minor, patch) (((major) << 22) | ((minor) << 12) | (patch))

    typedef enum
    {
        SR_RETURN_CODE_OK = 0,
        SR_RETURN_CODE_ERROR = 1,
    } SRReturnCode;

    typedef enum
    {
        SR_MESSAGE_TYPE_ERROR = 0,
        SR_MESSAGE_TYPE_WARNING = 1,
        SR_MESSAGE_TYPE_INFO = 2,
    } SRMsgType;

    typedef void (*SRMessageCallback)(SRMsgType type, const wchar_t *message);

    typedef struct SRUpscaleContext SRUpscaleContext;

    typedef enum
    {
        SR_UPSCALE_CONTEXT_QUERY_VERSION_INFO = 0,
        SR_UPSCALE_CONTEXT_QUERY_GPU_MEMORY_INFO = 1,
    } SRUpscaleContextQueryType;

    typedef struct
    {
        SRUpscaleContextQueryType type;
        void *data;
    } SRUpscaleContextQueryResult;

    typedef SRReturnCode (*SRCreateFunc)(SRUpscaleContext *, const struct SRCreateUpscaleContextDesc *desc);
    typedef SRReturnCode (*SRDestroyFunc)(SRUpscaleContext *context);
    typedef SRReturnCode (*SRQueryFunc)(SRUpscaleContextQueryResult *, SRUpscaleContext *, int queryType);
    typedef SRReturnCode (*SRDispatchUpscaleFunc)(SRUpscaleContext *context, const struct SRDispatchUpscaleDesc *desc);
    typedef void *(*SRGetFuncAddress)(void *device, const char *pName);

    typedef struct
    {
        uint64_t versionNumber;
        uint64_t versionId;
        char *versionName;
    } SRUpscaleContextQueryVersionInfoResult;

    typedef struct
    {
        uint64_t gpuMemory;
    } SRUpscaleContextQueryGpuMemoryInfoResult;

    typedef struct SRCreateUpscaleContextDesc
    {
        void *device;
        void *phyDevice;
        SRGetFuncAddress deviceProcAddr;
        SRVectorUint2 upscaledSize;
        SRVectorUint2 renderSize;
        SRMessageCallback messageCallback;
        uint32_t flags;
    } SRCreateUpscaleContextDesc;

    typedef struct SRTextureResource
    {
        bool exist;
        SRTextureResourceDescription desc;
        void *handle;

    } SRTextureResource;

    typedef struct SRDispatchUpscaleDesc
    {
        void *commandList;

        SRTextureResource color;
        SRTextureResource depth;
        SRTextureResource motionVectors;
        SRTextureResource exposure;
        SRTextureResource reactive;
        SRTextureResource transparencyAndComposition;
        SRTextureResource output;

        SRVectorFloat2 jitterOffset;
        SRVectorFloat2 motionVectorScale;
        SRVectorUint2 renderSize;
        SRVectorUint2 upscaleSize;

        float frameTimeDelta;
        bool enableSharpening;
        float sharpness;
        float preExposure;

        float cameraNear;
        float cameraFar;
        float cameraFovAngleVertical;
        float viewSpaceToMetersFactor;

        bool reset;
        uint32_t flags;
    } SRDispatchUpscaleDesc;

    typedef struct SRUpscaleContextCallbacks
    {
        SRCreateFunc pCreate;
        SRDestroyFunc pDestroy;
        SRQueryFunc pQuery;
        SRDispatchUpscaleFunc pDispatchUpscale;
    } SRUpscaleContextCallbacks;

    struct SRUpscaleContext
    {
        SRUpscaleContextCallbacks callbacks; // SRAPI内部设置的，外部模块别动
        SRCreateUpscaleContextDesc desc;
        void *userContext;
    };

    typedef struct SRUpscaleProvider
    {
        SRUpscaleContextCallbacks callbacks;
        uint64_t providerId;
    } SRUpscaleProvider;
    typedef SRReturnCode (*SRUpscaleProviderSupplierFunc)(SRUpscaleProvider *outProviders);
    typedef SRReturnCode (*SRUpscaleProviderSupplierCountFunc)(uint32_t *outCount);

    // 主要函数
    SR_API SRReturnCode srCreateUpscaleContext(
        SRUpscaleContext *outContext,
        SRUpscaleProvider *provider,
        const SRCreateUpscaleContextDesc *desc);

    SR_API SRReturnCode srDestroyUpscaleContext(SRUpscaleContext *context);

    SR_API SRReturnCode srQueryUpscaleContext(
        SRUpscaleContext *context,
        SRUpscaleContextQueryResult *outResult,
        SRUpscaleContextQueryType queryType);

    SR_API SRReturnCode srDispatchUpscale(
        SRUpscaleContext *context,
        const SRDispatchUpscaleDesc *desc);

    SR_API SRReturnCode srGetUpscaleProvider(
        SRUpscaleProvider *outProvider,
        uint64_t providerId);

    SR_API SRReturnCode srLoadUpscaleProvidersFromLibrary(
        const std::wstring &libPath,
        const std::string &getProvidersFuncName,      // SRUpscaleProviderSupplierFunc
        const std::string &getProvidersCountFuncName, // SRUpscaleProviderSupplierCountFunc
        SRMessageCallback messageCallback);

#ifdef __cplusplus
}
#endif
