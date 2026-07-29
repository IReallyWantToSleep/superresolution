/*
 * Minimal ABI declarations for the AMD FSR SDK 2.3 FFX API.
 *
 * These declarations intentionally cover only the signed DX12 upscaler DLL
 * surface used by SRNativeFSR. They mirror AMD's MIT-licensed ffx_api.h,
 * ffx_api_types.h, ffx_api_dx12.h, and ffx_upscale.h structures without
 * requiring the full SDK as a build dependency.
 *
 * Copyright (C) 2026 Advanced Micro Devices, Inc.
 * Copyright (C) 2026 Super Resolution contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#pragma once

#include <cstdint>

typedef void *ffxContext;
typedef uint32_t ffxReturnCode_t;
typedef uint64_t ffxStructType_t;

enum FfxApiReturnCodes {
    FFX_API_RETURN_OK = 0,
    FFX_API_RETURN_ERROR = 1,
    FFX_API_RETURN_ERROR_UNKNOWN_DESCTYPE = 2,
    FFX_API_RETURN_ERROR_RUNTIME_ERROR = 3,
    FFX_API_RETURN_NO_PROVIDER = 4,
    FFX_API_RETURN_ERROR_MEMORY = 5,
    FFX_API_RETURN_ERROR_PARAMETER = 6,
    FFX_API_RETURN_PROVIDER_NO_SUPPORT_NEW_DESCTYPE = 7,
};

struct ffxApiHeader {
    ffxStructType_t type;
    ffxApiHeader *pNext;
};

typedef ffxApiHeader ffxCreateContextDescHeader;
typedef ffxApiHeader ffxQueryDescHeader;
typedef ffxApiHeader ffxDispatchDescHeader;
typedef void (*ffxApiMessage)(uint32_t type, const wchar_t *message);

struct FfxApiDimensions2D {
    uint32_t width;
    uint32_t height;
};

struct FfxApiFloatCoords2D {
    float x;
    float y;
};

enum FfxApiSurfaceFormat {
    FFX_API_SURFACE_FORMAT_UNKNOWN,
    FFX_API_SURFACE_FORMAT_R32G32B32A32_TYPELESS,
    FFX_API_SURFACE_FORMAT_R32G32B32A32_UINT,
    FFX_API_SURFACE_FORMAT_R32G32B32A32_FLOAT,
    FFX_API_SURFACE_FORMAT_R16G16B16A16_FLOAT,
    FFX_API_SURFACE_FORMAT_R32G32B32_FLOAT,
    FFX_API_SURFACE_FORMAT_R32G32_FLOAT,
    FFX_API_SURFACE_FORMAT_R8_UINT,
    FFX_API_SURFACE_FORMAT_R32_UINT,
    FFX_API_SURFACE_FORMAT_R8G8B8A8_TYPELESS,
    FFX_API_SURFACE_FORMAT_R8G8B8A8_UNORM,
    FFX_API_SURFACE_FORMAT_R8G8B8A8_SNORM,
    FFX_API_SURFACE_FORMAT_R8G8B8A8_SRGB,
    FFX_API_SURFACE_FORMAT_B8G8R8A8_TYPELESS,
    FFX_API_SURFACE_FORMAT_B8G8R8A8_UNORM,
    FFX_API_SURFACE_FORMAT_B8G8R8A8_SRGB,
    FFX_API_SURFACE_FORMAT_R11G11B10_FLOAT,
    FFX_API_SURFACE_FORMAT_R10G10B10A2_UNORM,
    FFX_API_SURFACE_FORMAT_R16G16_FLOAT,
    FFX_API_SURFACE_FORMAT_R16G16_UINT,
    FFX_API_SURFACE_FORMAT_R16G16_SINT,
    FFX_API_SURFACE_FORMAT_R16_FLOAT,
    FFX_API_SURFACE_FORMAT_R16_UINT,
    FFX_API_SURFACE_FORMAT_R16_UNORM,
    FFX_API_SURFACE_FORMAT_R16_SNORM,
    FFX_API_SURFACE_FORMAT_R8_UNORM,
    FFX_API_SURFACE_FORMAT_R8G8_UNORM,
    FFX_API_SURFACE_FORMAT_R8G8_UINT,
    FFX_API_SURFACE_FORMAT_R32_FLOAT,
    FFX_API_SURFACE_FORMAT_R9G9B9E5_SHAREDEXP,
    FFX_API_SURFACE_FORMAT_R16G16B16A16_TYPELESS,
    FFX_API_SURFACE_FORMAT_R32G32_TYPELESS,
    FFX_API_SURFACE_FORMAT_R10G10B10A2_TYPELESS,
    FFX_API_SURFACE_FORMAT_R16G16_TYPELESS,
    FFX_API_SURFACE_FORMAT_R16_TYPELESS,
    FFX_API_SURFACE_FORMAT_R8_TYPELESS,
    FFX_API_SURFACE_FORMAT_R8G8_TYPELESS,
    FFX_API_SURFACE_FORMAT_R32_TYPELESS,
    FFX_API_SURFACE_FORMAT_R32G32_UINT,
    FFX_API_SURFACE_FORMAT_R8_SNORM,
};

enum FfxApiResourceFlags {
    FFX_API_RESOURCE_FLAGS_NONE = 0,
};

enum FfxApiResourceType {
    FFX_API_RESOURCE_TYPE_BUFFER,
    FFX_API_RESOURCE_TYPE_TEXTURE1D,
    FFX_API_RESOURCE_TYPE_TEXTURE2D,
    FFX_API_RESOURCE_TYPE_TEXTURE_CUBE,
    FFX_API_RESOURCE_TYPE_TEXTURE3D,
};

struct FfxApiResourceDescription {
    uint32_t type;
    uint32_t format;
    union {
        uint32_t width;
        uint32_t size;
    };
    union {
        uint32_t height;
        uint32_t stride;
    };
    union {
        uint32_t depth;
        uint32_t alignment;
    };
    uint32_t mipCount;
    uint32_t flags;
    uint32_t usage;
};

struct FfxApiResource {
    void *resource;
    FfxApiResourceDescription description;
    uint32_t state;
};

typedef void *(*ffxAlloc)(void *pUserData, uint64_t size);
typedef void (*ffxDealloc)(void *pUserData, void *pMem);

struct ffxAllocationCallbacks {
    void *pUserData;
    ffxAlloc alloc;
    ffxDealloc dealloc;
};

typedef ffxReturnCode_t (*PfnFfxCreateContext)(
    ffxContext *context,
    ffxCreateContextDescHeader *desc,
    const ffxAllocationCallbacks *memCb);
typedef ffxReturnCode_t (*PfnFfxDestroyContext)(
    ffxContext *context,
    const ffxAllocationCallbacks *memCb);
typedef ffxReturnCode_t (*PfnFfxQuery)(
    ffxContext *context,
    ffxQueryDescHeader *desc);
typedef ffxReturnCode_t (*PfnFfxDispatch)(
    ffxContext *context,
    const ffxDispatchDescHeader *desc);

struct FfxApiFunctions {
    PfnFfxCreateContext createContext;
    PfnFfxDestroyContext destroyContext;
    PfnFfxQuery query;
    PfnFfxDispatch dispatch;
};

#define FFX_API_EFFECT_MASK 0x00ff0000u
#define FFX_API_BACKEND_MASK 0xff000000u
#define FFX_API_EFFECT_ID_UPSCALE 0x00010000u
#define FFX_API_BACKEND_ID_DX12 0x00000000u
#define FFX_API_MAKE_EFFECT_SUB_ID(effectId, subversion) \
    ((effectId & FFX_API_EFFECT_MASK) | (subversion & ~FFX_API_EFFECT_MASK))
#define FFX_API_MAKE_BACKEND_SUB_ID(backendId, subversion) \
    ((backendId & FFX_API_BACKEND_MASK) | (subversion & ~FFX_API_BACKEND_MASK))

#define FFX_API_CREATE_CONTEXT_DESC_TYPE_UPSCALE \
    FFX_API_MAKE_EFFECT_SUB_ID(FFX_API_EFFECT_ID_UPSCALE, 0x00)
#define FFX_API_DISPATCH_DESC_TYPE_UPSCALE \
    FFX_API_MAKE_EFFECT_SUB_ID(FFX_API_EFFECT_ID_UPSCALE, 0x01)
#define FFX_API_CREATE_CONTEXT_DESC_TYPE_UPSCALE_VERSION \
    FFX_API_MAKE_EFFECT_SUB_ID(FFX_API_EFFECT_ID_UPSCALE, 0x0b)
#define FFX_API_CREATE_CONTEXT_DESC_TYPE_BACKEND_DX12 \
    FFX_API_MAKE_BACKEND_SUB_ID(FFX_API_BACKEND_ID_DX12, 0x02)
#define FFX_API_QUERY_DESC_TYPE_GET_PROVIDER_VERSION 6u

#define FFX_UPSCALER_VERSION_MAJOR 4
#define FFX_UPSCALER_VERSION_MINOR 1
#define FFX_UPSCALER_VERSION_PATCH 1
#define FFX_UPSCALER_MAKE_VERSION(major, minor, patch) \
    (((major) << 22) | ((minor) << 12) | (patch))
#define FFX_UPSCALER_VERSION \
    FFX_UPSCALER_MAKE_VERSION( \
        FFX_UPSCALER_VERSION_MAJOR, \
        FFX_UPSCALER_VERSION_MINOR, \
        FFX_UPSCALER_VERSION_PATCH)

enum FfxApiCreateContextUpscaleFlags {
    FFX_UPSCALE_ENABLE_HIGH_DYNAMIC_RANGE = (1 << 0),
    FFX_UPSCALE_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION = (1 << 2),
    FFX_UPSCALE_ENABLE_DEPTH_INVERTED = (1 << 3),
    FFX_UPSCALE_ENABLE_AUTO_EXPOSURE = (1 << 5),
    FFX_UPSCALE_ENABLE_DEBUG_CHECKING = (1 << 7),
};

struct ffxCreateContextDescUpscale {
    ffxCreateContextDescHeader header;
    uint32_t flags;
    FfxApiDimensions2D maxRenderSize;
    FfxApiDimensions2D maxUpscaleSize;
    ffxApiMessage fpMessage;
};

struct ffxCreateContextDescUpscaleVersion {
    ffxCreateContextDescHeader header;
    uint32_t version;
};

struct ffxCreateBackendDX12Desc {
    ffxCreateContextDescHeader header;
    void *device;
};

struct ffxDispatchDescUpscale {
    ffxDispatchDescHeader header;
    void *commandList;
    FfxApiResource color;
    FfxApiResource depth;
    FfxApiResource motionVectors;
    FfxApiResource exposure;
    FfxApiResource reactive;
    FfxApiResource transparencyAndComposition;
    FfxApiResource output;
    FfxApiFloatCoords2D jitterOffset;
    FfxApiFloatCoords2D motionVectorScale;
    FfxApiDimensions2D renderSize;
    FfxApiDimensions2D upscaleSize;
    bool enableSharpening;
    float sharpness;
    float frameTimeDelta;
    float preExposure;
    bool reset;
    float cameraNear;
    float cameraFar;
    float cameraFovAngleVertical;
    float viewSpaceToMetersFactor;
    uint32_t flags;
};

struct ffxQueryGetProviderVersion {
    ffxQueryDescHeader header;
    uint64_t versionId;
    const char *versionName;
};
