#pragma once
#include "sr_api_types.h"
#include <vulkan/vulkan.h>
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

#define SR_MAKE_VERSION(major, minor, patch) (((major) << 22) | ((minor) << 12) | (patch))

#define SR_API_MAX_CONTEXT_CREATE_PARAMS 16
    typedef enum
    {
        SR_UPSCALE_CONTEXT_CREATE_FLAG_NONE = 0,
        SR_UPSCALE_CONTEXT_CREATE_FLAG_ENABLE_DEBUG = 1 << 0,
        SR_UPSCALE_CONTEXT_CREATE_FLAG_OPENGL = 1 << 1,
        SR_UPSCALE_CONTEXT_CREATE_FLAG_VULKAN = 1 << 2,
    } SRUpscaleContextCreateFlags;

    typedef enum
    {
        SR_RETURN_CODE_OK = 0,           // ok
        SR_RETURN_CODE_NULL_POINTER = 1, // null pointer error
        SR_RETURN_CODE_ERROR = 2,
        SR_RETURN_CODE_CANNOT_FIND_PROVIDER = 3,
        SR_RETURN_CODE_UNEXPECTED_ERROR = 4,
        SR_RETURN_CODE_CANNOT_FIND_LIBRARY = 5,
        SR_RETURN_CODE_INVALID_PROVIDER_LIBRARY = 6,
        SR_RETURN_CODE_INVALID_ARGUMENT = 7,
        SR_RETURN_CODE_UNSUPPORTED = 8,
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
        SR_UPSCALE_CONTEXT_QUERY_AVAILABLE = 2,

    } SRUpscaleContextQueryType;

    typedef struct
    {
        SRUpscaleContextQueryType type;
        void *data;
    } SRUpscaleContextQueryResult;

    typedef SRReturnCode (*SRCreateFunc)(SRUpscaleContext *, const struct SRCreateUpscaleContextDesc *desc);
    typedef SRReturnCode (*SRInitFunc)(SRUpscaleContext *);
    typedef SRReturnCode (*SRDestroyFunc)(SRUpscaleContext *context);
    typedef SRReturnCode (*SRQueryFunc)(SRUpscaleContextQueryResult *, SRUpscaleContext *, int queryType);
    typedef SRReturnCode (*SRDispatchUpscaleFunc)(SRUpscaleContext *context, const struct SRDispatchUpscaleDesc *desc);
    typedef void *(*SRGetFuncAddress)(void *device, const char *pName);

    typedef struct
    {
        uint64_t versionNumber;
        uint64_t versionId;
    } SRUpscaleContextQueryVersionInfoResult;

    typedef struct
    {
        uint64_t gpuMemory;
    } SRUpscaleContextQueryGpuMemoryInfoResult;

    typedef struct
    {
        bool isAvailable;
    } SRUpscaleContextQueryAvailableInfoResult;

    typedef struct SRCreateUpscaleContextDescExtraParam
    {
        uint32_t key;
        void *value;
    } SRCreateUpscaleContextDescExtraParam;

    typedef struct SRCreateUpscaleContextDesc
    {
        void *instance;
        void *device;
        void *phyDevice;
        void *commandBuffer;
        SRGetFuncAddress deviceProcAddr;
        SRVectorUint2 upscaledSize;
        SRVectorUint2 renderSize;
        SRMessageCallback messageCallback;
        uint32_t flags;
        SRCreateUpscaleContextDescExtraParam extraParams[SR_API_MAX_CONTEXT_CREATE_PARAMS];
        uint32_t extraParamCount;
    } SRCreateUpscaleContextDesc;

    typedef struct SRTextureResource
    {
        bool exist;
        SRTextureResourceDescription desc;
        void *handle;
        void *imageView; // 可选
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
        SRInitFunc pInit;
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

#ifdef __cplusplus
}
#endif
