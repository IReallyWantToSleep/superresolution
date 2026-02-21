#pragma once
#include "sr_api_enums.h"
#include "sr_api_types.h"
#include <vulkan/vulkan.h>

#ifdef __cplusplus
extern "C"
{
#endif

#define SR_MAKE_VERSION(major, minor, patch) (((major) << 22) | ((minor) << 12) | (patch))
#define SR_API_CONTEXT_MAX_PARAMS 16

    typedef void (*SRMessageCallback)(SRMessageType type, const wchar_t *message);
    typedef void *(*SRGetFuncAddress)(void *device, const char *pName);

    typedef struct SRUpscaleContext SRUpscaleContext;

    typedef struct
    {
        SRUpscaleContextQueryType type;
        void *data;
    } SRUpscaleContextQueryResult;

    typedef struct
    {
        uint64_t versionNumber;
        uint64_t versionId;
    } SRQueryVersionResult;

    typedef struct
    {
        uint64_t gpuMemory;
    } SRQueryGpuMemoryResult;

    typedef struct
    {
        bool isAvailable;
    } SRQueryAvailabilityResult;

    typedef union SRParamValue
    {
        bool boolValue;
        int32_t int32Value;
        uint32_t uint32Value;
        int64_t int64Value;
        uint64_t uint64Value;
        float floatValue;
        double doubleValue;
        const char *stringValue;
        void *ptrValue;

        struct
        {
            void *data;
            size_t size;
        } binaryValue;
    } SRParamValue;

    typedef struct SRContextExtraParam
    {
        const char *name;
        SRParamValueType valueType;
        SRParamValue value;
		bool exist;
    } SRContextExtraParam;

    typedef struct SROpenGLDeviceInfo
    {
        SRGetFuncAddress deviceProcAddr;
    } SROpenGLDeviceInfo;

    typedef struct SRVulkanDeviceInfo
    {
        VkInstance instance;
        VkPhysicalDevice physicalDevice;
        VkDevice device;
        VkCommandBuffer initCommandBuffer;
        SRGetFuncAddress deviceProcAddr;
        SRGetFuncAddress instanceProcAddr;
    } SRVulkanDeviceInfo;

    typedef struct SRCommandBufferOpenGL
    {
    } SRCommandBufferOpenGL;

    typedef struct SRCommandBufferVulkan
    {
        VkCommandBuffer commandBuffer;
    } SRCommandBufferVulkan;

    typedef struct SRDispatchCommandBufferInfo
    {
        SRRenderApiType renderApiType;
        union
        {
            SRCommandBufferOpenGL opengl;
            SRCommandBufferVulkan vulkan;
        } apiCommandBuffer;
    } SRDispatchCommandBufferInfo;

    typedef struct SRContextExtraParams
    {
        SRContextExtraParam extraParams[SR_API_CONTEXT_MAX_PARAMS];
        uint32_t extraParamCount;
    } SRContextExtraParams;

    typedef struct SRCreateUpscaleContextDesc
    {
        SRRenderApiType renderApiType;
        union
        {
            SROpenGLDeviceInfo opengl;
            SRVulkanDeviceInfo vulkan;
        } renderDeviceInfo;
        SRVectorUint2 upscaledSize;
        SRVectorUint2 renderSize;
        SRMessageCallback messageCallback;
        SRContextExtraParams extraParams;
        uint32_t flags;
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
        SRDispatchCommandBufferInfo commandList;

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
        SRContextExtraParams extraParams;
        uint32_t flags;
    } SRDispatchUpscaleDesc;

    typedef SRReturnCode (*SRCreateFunc)(SRUpscaleContext *, const struct SRCreateUpscaleContextDesc *desc);
    typedef SRReturnCode (*SRInitFunc)(SRUpscaleContext *);
    typedef SRReturnCode (*SRDestroyFunc)(SRUpscaleContext *context);
    typedef SRReturnCode (*SRQueryFunc)(SRUpscaleContext *, SRUpscaleContextQueryResult *, int queryType);
    typedef SRReturnCode (*SRDispatchUpscaleFunc)(SRUpscaleContext *context, const struct SRDispatchUpscaleDesc *desc);

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

#ifdef __cplusplus
}
#endif
