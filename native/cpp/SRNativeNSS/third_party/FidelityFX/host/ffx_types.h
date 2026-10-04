// Copyright  © 2023 Advanced Micro Devices, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// SPDX-FileCopyrightText: Copyright 2025-2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
// SPDX-License-Identifier: MIT

#pragma once

#include <cstddef>
#include <stdint.h>
#include <cwchar>  // for the wide-character string functions
#include <cstring>

///
/// @defgroup ffxSDK SDK
/// The FidelityFX SDK
///

/// @defgroup ffxHost Host
/// The FidelityFX SDK host (CPU-side) References
///
/// @ingroup ffxSDK

/// @defgroup Defines Defines
/// Top level defines used by the FidelityFX SDK
///
/// @ingroup ffxHost

// When defining custom mutex you have to also define:
// FFX_MUTEX_LOCK - for exclusive locking of mutex
// FFX_MUTEX_LOCK_SHARED - for shared locking of mutex
// FFX_MUTEX_UNLOCK - for exclusive unlocking of mutex
// FFX_MUTEX_UNLOCK_SHARED - for shared unlocking of mutex
//
// If your mutex type doesn't support shared locking mechanism you can rely
// on exclusive locks only (define _SHARED variants to the same exclusive operation).
#ifndef FFX_MUTEX
#if __cplusplus >= 201703L
#include <shared_mutex>
/// FidelityFX mutex wrapper.
///
/// @ingroup SDKTypes
#define FFX_MUTEX std::shared_mutex
#define FFX_MUTEX_IMPL_SHARED
#else
#include <mutex>
/// FidelityFX mutex wrapper.
///
/// @ingroup SDKTypes
#define FFX_MUTEX std::mutex
#define FFX_MUTEX_IMPL_STANDARD
#endif  // #if __cplusplus >= 201703L
#endif  // #ifndef FFX_MUTEX

#if defined(FFX_GCC) || !defined(FFX_BUILD_AS_DLL)
/// FidelityFX exported functions
///
/// @ingroup Defines
#define FFX_API
#else
/// FidelityFX exported functions
///
/// @ingroup Defines
#if defined(_WIN32)
#define FFX_API __declspec(dllexport)
#else
#define FFX_API __attribute__((visibility("default")))
#endif
#endif  // #if defined (FFX_GCC)

#define FFX_SDK_DEFAULT_CONTEXT_SIZE (1024 * 256)

/// Maximum supported number of simultaneously bound SRVs.
///
/// @ingroup Defines
#define FFX_MAX_NUM_SRVS 16

/// Maximum supported number of simultaneously bound UAVs.
///
/// @ingroup Defines
#define FFX_MAX_NUM_UAVS 16

/// Maximum supported number of simultaneously bound RenderTargets.
///
/// @ingroup Defines
#define FFX_MAX_NUM_RTS 8

/// Maximum supported number of simultaneously bound tensors.
///
/// @ingroup Defines
#define FFX_MAX_NUM_TENSORS 8

/// Maximum number of constant buffers bound.
///
/// @ingroup Defines
#define FFX_MAX_NUM_CONST_BUFFERS 3

/// Maximum size of bound constant buffers.
#define FFX_MAX_CONST_SIZE 256

/// Maximum number of characters in a resource name
///
/// @ingroup Defines
#define FFX_RESOURCE_NAME_SIZE 64

/// Maximum number of queued frames in the backend
///
/// @ingroup Defines
#define FFX_MAX_QUEUED_FRAMES (4)

/// Maximum number of resources per effect context
///
/// @ingroup Defines
#define FFX_MAX_RESOURCE_COUNT (64)

/// Maximum number of passes per effect component
///
/// @ingroup Defines
#define FFX_MAX_PASS_COUNT (50)

/// Total ring buffer size needed for a single effect context
///
/// @ingroup Defines
#define FFX_RING_BUFFER_SIZE (FFX_MAX_QUEUED_FRAMES * FFX_MAX_PASS_COUNT * FFX_MAX_RESOURCE_COUNT)

/// Size of constant buffer entry in the ring buffer table
///
/// @ingroup Defines
#define FFX_BUFFER_SIZE (768)

/// Total ring buffer memory size for a single effect context
///
/// @ingroup Defines
#define FFX_CONSTANT_BUFFER_RING_BUFFER_SIZE (FFX_MAX_QUEUED_FRAMES * FFX_MAX_PASS_COUNT * FFX_BUFFER_SIZE)

/// Maximum number of barriers per flush
///
/// @ingroup Defines
#define FFX_MAX_BARRIERS (16)

/// Maximum number of GPU jobs per submission
///
/// @ingroup Defines
#define FFX_MAX_GPU_JOBS (64)

/// Maximum number of samplers supported
///
/// @ingroup Defines
#define FFX_MAX_SAMPLERS (16)

/// Maximum number of simultaneous upload jobs
///
/// @ingroup Defines
#define UPLOAD_JOB_COUNT (16)

// Off by default warnings
#pragma warning(disable : 4365 4710 4820 5039)

#ifdef __cplusplus
extern "C" {
#endif  // #ifdef __cplusplus

/// @defgroup CPUTypes CPU Types
/// CPU side type defines for all commonly used variables
///
/// @ingroup ffxHost

/// A typedef for version numbers returned from functions in the FidelityFX SDK.
///
/// @ingroup CPUTypes
typedef uint32_t FfxVersionNumber;

/// A typedef for a boolean value.
///
/// @ingroup CPUTypes
typedef bool FfxBoolean;

/// A typedef for a unsigned 8bit integer.
///
/// @ingroup CPUTypes
typedef uint8_t FfxUInt8;

/// A typedef for a unsigned 16bit integer.
///
/// @ingroup CPUTypes
typedef uint16_t FfxUInt16;

/// A typedef for a unsigned 32bit integer.
///
/// @ingroup CPUTypes
typedef uint32_t FfxUInt32;

/// A typedef for a unsigned 64bit integer.
///
/// @ingroup CPUTypes
typedef uint64_t FfxUInt64;

/// A typedef for a signed 8bit integer.
///
/// @ingroup CPUTypes
typedef int8_t FfxInt8;

/// A typedef for a signed 16bit integer.
///
/// @ingroup CPUTypes
typedef int16_t FfxInt16;

/// A typedef for a signed 32bit integer.
///
/// @ingroup CPUTypes
typedef int32_t FfxInt32;

/// A typedef for a signed 64bit integer.
///
/// @ingroup CPUTypes
typedef int64_t FfxInt64;

/// A typedef for a floating point value.
///
/// @ingroup CPUTypes
typedef float FfxFloat32;

/// A typedef for a 2-dimensional floating point value.
///
/// @ingroup CPUTypes
typedef float FfxFloat32x2[2];

/// A typedef for a 3-dimensional floating point value.
///
/// @ingroup CPUTypes
typedef float FfxFloat32x3[3];

/// A typedef for a 4-dimensional floating point value.
///
/// @ingroup CPUTypes
typedef float FfxFloat32x4[4];

/// A typedef for a 4x4 floating point matrix.
///
/// @ingroup CPUTypes
typedef float FfxFloat32x4x4[16];

/// A typedef for a 2-dimensional 32bit unsigned integer.
///
/// @ingroup CPUTypes
typedef uint32_t FfxUInt32x2[2];

/// A typedef for a 3-dimensional 32bit unsigned integer.
///
/// @ingroup CPUTypes
typedef uint32_t FfxUInt32x3[3];

/// A typedef for a 4-dimensional 32bit unsigned integer.
///
/// @ingroup CPUTypes
typedef uint32_t FfxUInt32x4[4];

/// A typedef for a 2-dimensional 32bit signed integer.
///
/// @ingroup CPUTypes
typedef int32_t FfxInt32x2[2];

/// A typedef for a 3-dimensional 32bit signed integer.
///
/// @ingroup CPUTypes
typedef int32_t FfxInt32x3[3];

/// A typedef for a 4-dimensional 32bit signed integer.
///
/// @ingroup CPUTypes
typedef int32_t FfxInt32x4[4];

/// @defgroup SDKTypes SDK Types
/// Structure and Enumeration definitions used by the FidelityFX SDK
///
/// @ingroup ffxHost

/// An enumeration of surface formats. Needs to match enum FfxApiSurfaceFormat
///
/// @ingroup SDKTypes
typedef enum FfxSurfaceFormat
{

    FFX_SURFACE_FORMAT_UNKNOWN,                ///< Unknown format
    FFX_SURFACE_FORMAT_R32G32B32A32_TYPELESS,  ///< 32 bit per channel, 4 channel typeless format
    FFX_SURFACE_FORMAT_R32G32B32A32_UINT,      ///< 32 bit per channel, 4 channel uint format
    FFX_SURFACE_FORMAT_R32G32B32A32_FLOAT,     ///< 32 bit per channel, 4 channel float format
    FFX_SURFACE_FORMAT_R16G16B16A16_FLOAT,     ///< 16 bit per channel, 4 channel float format
    FFX_SURFACE_FORMAT_R32G32B32_FLOAT,        ///< 32 bit per channel, 3 channel float format
    FFX_SURFACE_FORMAT_R32G32_FLOAT,           ///< 32 bit per channel, 2 channel float format
    FFX_SURFACE_FORMAT_R8_UINT,                ///< 8 bit per channel, 1 channel float format
    FFX_SURFACE_FORMAT_R8_SINT,                ///< 8 bit per channel, 1 channel float format
    FFX_SURFACE_FORMAT_R32_UINT,               ///< 32 bit per channel, 1 channel float format
    FFX_SURFACE_FORMAT_R8G8B8A8_TYPELESS,      ///<  8 bit per channel, 4 channel typeless format
    FFX_SURFACE_FORMAT_R8G8B8A8_UNORM,         ///<  8 bit per channel, 4 channel unsigned normalized format
    FFX_SURFACE_FORMAT_R8G8B8A8_SNORM,         ///<  8 bit per channel, 4 channel signed normalized format
    FFX_SURFACE_FORMAT_R8G8B8A8_SRGB,          ///<  8 bit per channel, 4 channel srgb normalized
    FFX_SURFACE_FORMAT_B8G8R8A8_TYPELESS,      ///<  8 bit per channel, 4 channel typeless format
    FFX_SURFACE_FORMAT_B8G8R8A8_UNORM,         ///<  8 bit per channel, 4 channel unsigned normalized format
    FFX_SURFACE_FORMAT_B8G8R8A8_SRGB,          ///<  8 bit per channel, 4 channel srgb normalized
    FFX_SURFACE_FORMAT_R11G11B10_FLOAT,        ///< 32 bit 3 channel float format
    FFX_SURFACE_FORMAT_R10G10B10A2_UNORM,      ///< 10 bit per 3 channel, 2 bit for 1 channel normalized format
    FFX_SURFACE_FORMAT_R16G16_FLOAT,           ///< 16 bit per channel, 2 channel float format
    FFX_SURFACE_FORMAT_R16G16_UINT,            ///< 16 bit per channel, 2 channel unsigned int format
    FFX_SURFACE_FORMAT_R16G16_SINT,            ///< 16 bit per channel, 2 channel signed int format
    FFX_SURFACE_FORMAT_R16_FLOAT,              ///< 16 bit per channel, 1 channel float format
    FFX_SURFACE_FORMAT_R16_UINT,               ///< 16 bit per channel, 1 channel unsigned int format
    FFX_SURFACE_FORMAT_R16_UNORM,              ///< 16 bit per channel, 1 channel unsigned normalized format
    FFX_SURFACE_FORMAT_R16_SNORM,              ///< 16 bit per channel, 1 channel signed normalized format
    FFX_SURFACE_FORMAT_R8_UNORM,               ///<  8 bit per channel, 1 channel unsigned normalized format
    FFX_SURFACE_FORMAT_R8_SNORM,               ///<  8 bit per channel, 1 channel signed normalized format
    FFX_SURFACE_FORMAT_R8G8_UNORM,             ///<  8 bit per channel, 2 channel unsigned normalized format
    FFX_SURFACE_FORMAT_R8G8_UINT,              ///<  8 bit per channel, 2 channel unsigned integer format
    FFX_SURFACE_FORMAT_R8G8_SINT,              ///<  8 bit per channel, 2 channel signed integer format
    FFX_SURFACE_FORMAT_R32_FLOAT,              ///< 32 bit per channel, 1 channel float format
    FFX_SURFACE_FORMAT_R9G9B9E5_SHAREDEXP,     ///<  9 bit per channel, 5 bit exponent format

    FFX_SURFACE_FORMAT_R16G16B16A16_TYPELESS,  ///< 16 bit per channel, 4 channel typeless format
    FFX_SURFACE_FORMAT_R32G32_TYPELESS,        ///< 32 bit per channel, 2 channel typeless format
    FFX_SURFACE_FORMAT_R10G10B10A2_TYPELESS,   ///< 10 bit per 3 channel, 2 bit for 1 channel typeless format
    FFX_SURFACE_FORMAT_R16G16_TYPELESS,        ///< 16 bit per channel, 2 channel typeless format
    FFX_SURFACE_FORMAT_R16_TYPELESS,           ///< 16 bit per channel, 1 channel typeless format
    FFX_SURFACE_FORMAT_R8_TYPELESS,            ///<  8 bit per channel, 1 channel typeless format
    FFX_SURFACE_FORMAT_R8G8_TYPELESS,          ///<  8 bit per channel, 2 channel typeless format
    FFX_SURFACE_FORMAT_R32_TYPELESS,           ///< 32 bit per channel, 1 channel typeless format
} FfxSurfaceFormat;

typedef enum FfxIndexFormat
{
    FFX_INDEX_TYPE_UINT32,
    FFX_INDEX_TYPE_UINT16
} FfxIndexFormat;

/// An enumeration of resource usage.
///
/// @ingroup SDKTypes
typedef enum FfxResourceUsage
{

    FFX_RESOURCE_USAGE_READ_ONLY        = 0,          ///< No usage flags indicate a resource is read only.
    FFX_RESOURCE_USAGE_RENDERTARGET     = (1 << 0),   ///< Indicates a resource will be used as render target.
    FFX_RESOURCE_USAGE_UAV              = (1 << 1),   ///< Indicates a resource will be used as UAV.
    FFX_RESOURCE_USAGE_DEPTHTARGET      = (1 << 2),   ///< Indicates a resource will be used as depth target.
    FFX_RESOURCE_USAGE_INDIRECT         = (1 << 3),   ///< Indicates a resource will be used as indirect argument buffer
    FFX_RESOURCE_USAGE_ARRAYVIEW        = (1 << 4),   ///< Indicates a resource that will generate array views. Works on 2D and cubemap textures
    FFX_RESOURCE_USAGE_STENCILTARGET    = (1 << 5),   ///< Indicates a resource will be used as stencil target.
    FFX_RESOURCE_USAGE_DCC_RENDERTARGET = (1 << 15),  ///< Indicates a resource that should specify optimal render target memory access flags (for console use)
} FfxResourceUsage;

/// An enumeration of resource states. These are exclusive states and should not be combined.
///
/// @ingroup SDKTypes
typedef enum FfxResourceStates
{

    FFX_RESOURCE_STATE_COMMON       = (1 << 0),
    FFX_RESOURCE_STATE_COMPUTE_UAV  = (1 << 1),  ///< Indicates a resource is in the state to be used as UAV in compute shaders.
    FFX_RESOURCE_STATE_PIXEL_UAV    = (1 << 2),  ///< Indicates a resource is in the state to be used as UAV in pixel shaders.
    FFX_RESOURCE_STATE_GENERIC_UAV  = (FFX_RESOURCE_STATE_COMPUTE_UAV |
                                      FFX_RESOURCE_STATE_PIXEL_UAV),  ///< Indicates a resource is in the state to be used as UAV in compute or pixel shaders.
    FFX_RESOURCE_STATE_COMPUTE_READ = (1 << 3),                        ///< Indicates a resource is in the state to be read by compute shaders.
    FFX_RESOURCE_STATE_PIXEL_READ   = (1 << 4),                        ///< Indicates a resource is in the state to be read by pixel shaders.
    FFX_RESOURCE_STATE_PIXEL_COMPUTE_READ =
        (FFX_RESOURCE_STATE_PIXEL_READ | FFX_RESOURCE_STATE_COMPUTE_READ),  ///< Indicates a resource is in the state to be read by pixel or compute shaders.
    FFX_RESOURCE_STATE_COPY_SRC  = (1 << 5),                                ///< Indicates a resource is in the state to be used as source in a copy command.
    FFX_RESOURCE_STATE_COPY_DEST = (1 << 6),  ///< Indicates a resource is in the state to be used as destination in a copy command.
    FFX_RESOURCE_STATE_GENERIC_READ =
        (FFX_RESOURCE_STATE_COPY_SRC | FFX_RESOURCE_STATE_COMPUTE_READ),  ///< Indicates a resource is in generic (slow) read state.
    FFX_RESOURCE_STATE_INDIRECT_ARGUMENT = (1 << 7),                      ///< Indicates a resource is in the state to be used as an indirect command argument
    FFX_RESOURCE_STATE_PRESENT           = (1 << 8),                      ///< Indicates a resource is in the state to be used to present to the swap chain
    FFX_RESOURCE_STATE_RENDER_TARGET     = (1 << 9),                      ///< Indicates a resource is in the state to be used as render target
    FFX_RESOURCE_STATE_DATA_GRAPH_READ   = (1 << 10),                     ///< Indicates a resource is in the state to be read by data graph
    FFX_RESOURCE_STATE_DATA_GRAPH_WRITE  = (1 << 11),                     ///< Indicates a resource is in the state to be written by data graph
    // used by FrameInterpolationSwapchain
    FFX_RESOURCE_STATE_DEPTH_ATTACHEMENT = (1 << 12),  ///< Indicates a resource is in the state to be used as depth attachment

} FfxResourceStates;

/// An enumeration of surface dimensions.
///
/// @ingroup SDKTypes
typedef enum FfxResourceDimension
{

    FFX_RESOURCE_DIMENSION_TEXTURE_1D,  ///< A resource with a single dimension.
    FFX_RESOURCE_DIMENSION_TEXTURE_2D,  ///< A resource with two dimensions.
} FfxResourceDimension;

/// An enumeration of resource view dimensions.
///
/// @ingroup SDKTypes
typedef enum FfxResourceViewDimension
{
    FFX_RESOURCE_VIEW_DIMENSION_BUFFER,            ///< A resource view on a buffer.
    FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_1D,        ///< A resource view on a single dimension.
    FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_1D_ARRAY,  ///< A resource view on a single dimensional array.
    FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_2D,        ///< A resource view on two dimensions.
    FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_2D_ARRAY,  ///< A resource view on two dimensional array.
    FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_3D,        ///< A resource view on three dimensions.
} FfxResourceViewDimension;

/// An enumeration of surface dimensions.
///
/// @ingroup SDKTypes
typedef enum FfxResourceFlags
{

    FFX_RESOURCE_FLAGS_NONE           = 0,         ///< No flags.
    FFX_RESOURCE_FLAGS_ALIASABLE      = (1 << 0),  ///< A bit indicating a resource does not need to persist across frames.
    FFX_RESOURCE_FLAGS_IMAGE_ALIASED  = (1 << 1),  ///< A bit indicating a resource's (tensor or buffer) memory will be aliased to an image
    FFX_RESOURCE_FLAGS_BUFFER_ALIASED = (1 << 2),  ///< A bit indicating a resource creates an aliased buffer
    FFX_RESOURCE_FLAGS_UNDEFINED      = (1 << 3),  ///< Special case flag used internally when importing resources that require additional setup
} FfxResourceFlags;

/// An enumeration of all resource view types.
///
/// @ingroup SDKTypes
typedef enum FfxResourceViewType
{

    FFX_RESOURCE_VIEW_UNORDERED_ACCESS,  ///< The resource view is an unordered access view (UAV).
    FFX_RESOURCE_VIEW_SHADER_READ,       ///< The resource view is a shader resource view (SRV).
} FfxResourceViewType;

/// The type of filtering to perform when reading a texture.
///
/// @ingroup SDKTypes
typedef enum FfxFilterType
{

    FFX_FILTER_TYPE_MINMAGMIP_POINT,        ///< Point sampling.
    FFX_FILTER_TYPE_MINMAGMIP_LINEAR,       ///< Sampling with interpolation.
    FFX_FILTER_TYPE_MINMAGLINEARMIP_POINT,  ///< Use linear interpolation for minification and magnification; use point sampling for mip-level sampling.
} FfxFilterType;

/// The address mode used when reading a texture.
///
/// @ingroup SDKTypes
typedef enum FfxAddressMode
{

    FFX_ADDRESS_MODE_WRAP,         ///< Wrap when reading texture.
    FFX_ADDRESS_MODE_MIRROR,       ///< Mirror when reading texture.
    FFX_ADDRESS_MODE_CLAMP,        ///< Clamp when reading texture.
    FFX_ADDRESS_MODE_BORDER,       ///< Border color when reading texture.
    FFX_ADDRESS_MODE_MIRROR_ONCE,  ///< Mirror once when reading texture.
} FfxAddressMode;

/// An enumeration of all supported shader models.
///
/// @ingroup SDKTypes
typedef enum FfxShaderModel
{

    FFX_SHADER_MODEL_5_1,  ///< Shader model 5.1.
    FFX_SHADER_MODEL_6_0,  ///< Shader model 6.0.
    FFX_SHADER_MODEL_6_1,  ///< Shader model 6.1.
    FFX_SHADER_MODEL_6_2,  ///< Shader model 6.2.
    FFX_SHADER_MODEL_6_3,  ///< Shader model 6.3.
    FFX_SHADER_MODEL_6_4,  ///< Shader model 6.4.
    FFX_SHADER_MODEL_6_5,  ///< Shader model 6.5.
    FFX_SHADER_MODEL_6_6,  ///< Shader model 6.6.
    FFX_SHADER_MODEL_6_7,  ///< Shader model 6.7.
} FfxShaderModel;

// An enumeration for different resource types
///
/// @ingroup SDKTypes
typedef enum FfxResourceType
{

    FFX_RESOURCE_TYPE_BUFFER,        ///< The resource is a buffer.
    FFX_RESOURCE_TYPE_TEXTURE1D,     ///< The resource is a 1-dimensional texture.
    FFX_RESOURCE_TYPE_TEXTURE2D,     ///< The resource is a 2-dimensional texture.
    FFX_RESOURCE_TYPE_TEXTURE_CUBE,  ///< The resource is a cube map.
    FFX_RESOURCE_TYPE_TEXTURE3D,     ///< The resource is a 3-dimensional texture.
    FFX_RESOURCE_TYPE_TENSOR,        ///< The resource is a tensor.
} FfxResourceType;

//TODO: check if there is a better place for these optical flow enums
/// The transfer function used to convert interpolation source colour data to linear RGB.
///
/// Restored together with the D3D12 frame-interpolation swapchain, which is imported from
/// AMD's FidelityFX SDK 1.1.3 and forwards this through to the frame generation callback.
/// The Arm fork dropped the enumeration along with the whole D3D12 backend.
///
/// @ingroup SDKTypes
typedef enum FfxBackbufferTransferFunction
{
    FFX_BACKBUFFER_TRANSFER_FUNCTION_SRGB,
    FFX_BACKBUFFER_TRANSFER_FUNCTION_PQ,
    FFX_BACKBUFFER_TRANSFER_FUNCTION_SCRGB
} FfxBackbufferTransferFunction;

// An enumeration for different optical flow connection types
///
/// @ingroup SDKTypes
typedef enum FfxOpticalFlowConnectionType
{
    FFX_OPTICAL_FLOW_CONNECTION_UNKNOWN,
    FFX_OPTICAL_FLOW_CONNECTION_INPUT,
    FFX_OPTICAL_FLOW_CONNECTION_REFERENCE,
    FFX_OPTICAL_FLOW_CONNECTION_HINT,
    FFX_OPTICAL_FLOW_CONNECTION_FLOW_VECTOR,
    FFX_OPTICAL_FLOW_CONNECTION_BACKWARD_FLOW_VECTOR,
    FFX_OPTICAL_FLOW_CONNECTION_COST,
    FFX_OPTICAL_FLOW_CONNECTION_BACKWARD_COST,
} FfxOpticalFlowConnectionType;

// An enumeration for different optical flow grid sizes
///
/// @ingroup SDKTypes
typedef enum FfxOpticalFlowGridSize
{
    FFX_OPTICAL_FLOW_GRID_SIZE_UNKNOWN = 0,
    FFX_OPTICAL_FLOW_GRID_SIZE_1X1     = 1,
    FFX_OPTICAL_FLOW_GRID_SIZE_2X2     = 2,
    FFX_OPTICAL_FLOW_GRID_SIZE_4X4     = 4,
    FFX_OPTICAL_FLOW_GRID_SIZE_8X8     = 8,
} FfxOpticalFlowGridSize;

// An enumeration for different optical flow grid sizes
///
/// @ingroup SDKTypes
typedef enum FfxOpticalFlowPerformanceLevel
{
    FFX_OPTICAL_FLOW_PERFORMANCE_LEVEL_UNKNOWN,
    FFX_OPTICAL_FLOW_PERFORMANCE_LEVEL_SLOW,
    FFX_OPTICAL_FLOW_PERFORMANCE_LEVEL_MEDIUM,
    FFX_OPTICAL_FLOW_PERFORMANCE_LEVEL_FAST
} FfxOpticalFlowPerformanceLevel;

typedef enum FfxDataGraphOpticalFlowExecuteFlagBits
{
    FFX_DATA_GRAPH_OPTICAL_FLOW_EXECUTE_DISABLE_TEMPORAL_HINTS      = (1 << 0),
    FFX_DATA_GRAPH_OPTICAL_FLOW_EXECUTE_INPUT_UNCHANGED             = (1 << 1),
    FFX_DATA_GRAPH_OPTICAL_FLOW_EXECUTE_REFERENCE_UNCHANGED         = (1 << 2),
    FFX_DATA_GRAPH_OPTICAL_FLOW_EXECUTE_INPUT_IS_PREVIOUS_REFERENCE = (1 << 3),
    FFX_DATA_GRAPH_OPTICAL_FLOW_EXECUTE_REFERENCE_IS_PREVIOUS_INPUT = (1 << 4),
} FfxDataGraphOpticalFlowExecuteFlagBits;

/// An enumeration for different heap types
///
/// @ingroup SDKTypes
typedef enum FfxHeapType
{

    FFX_HEAP_TYPE_DEFAULT = 0,  ///< Local memory.
    FFX_HEAP_TYPE_UPLOAD,       ///< Heap used for uploading resources.
    FFX_HEAP_TYPE_READBACK      ///< Heap used for reading back resources.
} FfxHeapType;

/// An enumeration for different render job types
///
/// @ingroup SDKTypes
typedef enum FfxGpuJobType
{

    FFX_GPU_JOB_CLEAR_FLOAT = 0,  ///< The GPU job is performing a floating-point clear.
    FFX_GPU_JOB_COPY        = 1,  ///< The GPU job is performing a copy.
    FFX_GPU_JOB_COMPUTE     = 2,  ///< The GPU job is performing a compute dispatch.
    FFX_GPU_JOB_BARRIER     = 3,  ///< The GPU job is performing a barrier.
    FFX_GPU_JOB_DISCARD     = 4,  ///< The GPU job is performing a floating-point clear.
    FFX_GPU_JOB_FRAGMENT    = 5,  ///< The GPU job is performing a fragment pass.
    FFX_GPU_JOB_DATA_GRAPH  = 6,  ///< The GPU job is performing a data graph pass.
    FFX_GPU_JOB_CLEAR_UINT  = 7   ///< The GPU job is performing an unsigned integer clear.
} FfxGpuJobType;

/// An enumeration for various descriptor types
///
/// @ingroup SDKTypes
typedef enum FfxDescriptorType
{

    //FFX_DESCRIPTOR_CBV = 0,   // All CBVs currently mapped to root signature
    //FFX_DESCRIPTOR_SAMPLER,   // All samplers currently static
    FFX_DESCRIPTOR_TEXTURE_SRV = 0,
    FFX_DESCRIPTOR_BUFFER_SRV,
    FFX_DESCRIPTOR_TEXTURE_UAV,
    FFX_DESCRIPTOR_BUFFER_UAV,
} FfxDescriptiorType;

/// An enumeration for view binding stages
///
/// @ingroup SDKTypes
typedef enum FfxBindStage
{

    FFX_BIND_PIXEL_SHADER_STAGE   = 1 << 0,
    FFX_BIND_VERTEX_SHADER_STAGE  = 1 << 1,
    FFX_BIND_COMPUTE_SHADER_STAGE = 1 << 2,

} FfxBindStage;

/// An enumeration for barrier types
///
/// @ingroup SDKTypes
typedef enum FfxBarrierType
{
    FFX_BARRIER_TYPE_TRANSITION = 0,
    FFX_BARRIER_TYPE_UAV,
} FfxBarrierType;

/// An enumeration for message types that can be passed
///
/// @ingroup SDKTypes
typedef enum FfxMsgType
{
    FFX_MESSAGE_TYPE_ERROR   = 0,
    FFX_MESSAGE_TYPE_WARNING = 1,
    FFX_MESSAGE_TYPE_COUNT
} FfxMsgType;

/// An enumeration of all the effects which constitute the FidelityFX SDK.
///
/// Dictates what effect shader blobs to fetch for pipeline creation
///
/// @ingroup SDKTypes
typedef enum FfxEffect
{

    FFX_EFFECT_FSR2 = 0,            ///< FidelityFX Super Resolution v2
    FFX_EFFECT_FSR1,                ///< FidelityFX Super Resolution
    FFX_EFFECT_SPD,                 ///< FidelityFX Single Pass Downsampler
    FFX_EFFECT_BLUR,                ///< FidelityFX Blur
    FFX_EFFECT_BRIXELIZER,          ///< FidelityFX Brixelizer
    FFX_EFFECT_BRIXELIZER_GI,       ///< FidelityFX Brixelizer GI
    FFX_EFFECT_CACAO,               ///< FidelityFX Combined Adaptive Compute Ambient Occlusion
    FFX_EFFECT_CAS,                 ///< FidelityFX Contrast Adaptive Sharpening
    FFX_EFFECT_DENOISER,            ///< FidelityFX Denoiser
    FFX_EFFECT_LENS,                ///< FidelityFX Lens
    FFX_EFFECT_PARALLEL_SORT,       ///< FidelityFX Parallel Sort
    FFX_EFFECT_SSSR,                ///< FidelityFX Stochastic Screen Space Reflections
    FFX_EFFECT_VARIABLE_SHADING,    ///< FidelityFX Variable Shading
    FFX_EFFECT_LPM,                 ///< FidelityFX Luma Preserving Mapper
    FFX_EFFECT_DOF,                 ///< FidelityFX Depth of Field
    FFX_EFFECT_CLASSIFIER,          ///< FidelityFX Classifier
    FFX_EFFECT_FSR3UPSCALER,        ///< FidelityFX Super Resolution v3
    FFX_EFFECT_FRAMEINTERPOLATION,  ///< FidelityFX Frame Interpolation, part of FidelityFX Super Resolution v3
    FFX_EFFECT_OPTICALFLOW,         ///< FidelityFX Optical Flow, part of FidelityFX Super Resolution v3
    FFX_EFFECT_NSSPLACEHOLDER,
    FFX_EFFECT_NFRUPLACEHOLDER,
    FFX_EFFECT_ASR,
    FFX_EFFECT_NSS,
    ARM_EFFECT_FRAMEINTERPOLATION,  ///< Arm Frame Interpolation
    FFX_EFFECT_CT_OPTICALFLOW,      ///< Arm Optical Flow
    ARM_EFFECT_OPTICALFLOW,         ///< Arm neural based Optical Flow

    FFX_EFFECT_SHAREDRESOURCES  = 127,  ///< FidelityFX Shared resources effect ID
    FFX_EFFECT_SHAREDAPIBACKEND = 128   ///< FidelityFX Shared backend context used with DLL API
} FfxEffect;

/// A typedef representing the graphics device.
///
/// @ingroup SDKTypes
typedef void* FfxDevice;

typedef void* FfxCommandQueue;

typedef void* FfxSwapchain;

/// A typedef representing a command list or command buffer.
///
/// @ingroup SDKTypes
typedef void* FfxCommandList;

/// A typedef for a root signature.
///
/// @ingroup SDKTypes
typedef void* FfxRootSignature;

/// A typedef for a command signature, used for indirect workloads
///
/// @ingroup SDKTypes
typedef void* FfxCommandSignature;

/// A typedef for a pipeline state object.
///
/// @ingroup SDKTypes
typedef void* FfxPipeline;

/// Allocate block of memory.
///
/// The callback function for requesting memory of provided size.
/// <c><i>size</i></c> cannot be 0.
///
/// @param [in]  size               Size in bytes of memory to allocate.
///
/// @retval
/// NULL                            The operation failed.
/// @retval
/// Anything else                   The operation completed successfully.
///
/// @ingroup SDKTypes
typedef void* (*FfxAllocFunc)(size_t size);

/// Reallocate block of memory.
///
/// The callback function for reallocating provided block of memory to new location
/// with specified size.
/// If the operation failed then contents of <c><i>ptr</i></c>
/// cannot be changed. <c><i>size</i></c> cannot be 0.
///
/// @param [in]  ptr                A pointer to previous block of memory.
/// @param [in]  size               Size in bytes of memory to allocate.
///
/// @retval
/// NULL                            The operation failed.
/// @retval
/// Anything else                   The operation completed successfully.
///
/// @ingroup SDKTypes
typedef void* (*FfxReallocFunc)(void* ptr, size_t size);

/// Free block of memory.
///
/// The callback function for freeing provided block of memory.
/// <c><i>ptr</i></c> cannot be <c><i>NULL</i></c>.
///
/// @param [in]  ptr                A pointer to block of memory.
///
/// @ingroup SDKTypes
typedef void (*FfxFreeFunc)(void* ptr);

/// A structure encapsulating a set of allocation callbacks.
///
/// @ingroup SDKTypes
typedef struct FfxAllocationCallbacks
{
    FfxAllocFunc   fpAlloc;    ///< Callback for allocating memory in the library.
    FfxReallocFunc fpRealloc;  ///< Callback for reallocating memory in the library.
    FfxFreeFunc    fpFree;     ///< Callback for freeing allocated memory in the library.
} FfxAllocationCallbacks;

/// A structure encapsulating the bindless descriptor configuration of an effect.
///
/// @ingroup SDKTypes
typedef struct FfxEffectBindlessConfig
{
    uint32_t maxTextureSrvs;  ///< Maximum number of texture SRVs needed in the bindless table.
    uint32_t maxBufferSrvs;   ///< Maximum number of buffer SRVs needed in the bindless table.
    uint32_t maxTextureUavs;  ///< Maximum number of texture UAVs needed in the bindless table.
    uint32_t maxBufferUavs;   ///< Maximum number of buffer UAVs needed in the bindless table.
} FfxEffectBindlessConfig;

/// A structure encapsulating the bindless descriptor configuration of an effect.
///
/// @ingroup SDKTypes
typedef void* FfxDataGraphPipelineSession;

/// A structure encapsulating a collection of device capabilities.
///
/// @ingroup SDKTypes
typedef struct FfxDeviceCapabilities
{
    FfxShaderModel maximumSupportedShaderModel;                 ///< The maximum shader model supported by the device.
    uint32_t       waveLaneCountMin;                            ///< The minimum supported wavefront width.
    uint32_t       waveLaneCountMax;                            ///< The maximum supported wavefront width.
    bool           fp16Supported;                               ///< The device supports FP16 in hardware.
    bool           raytracingSupported;                         ///< The device supports ray tracing.
    bool           deviceCoherentMemorySupported;               ///< The device supports AMD coherent memory.
    bool           dedicatedAllocationSupported;                ///< The device supports dedicated allocations for resources.
    bool           bufferMarkerSupported;                       ///< The device supports AMD buffer markers.
    bool           extendedSynchronizationSupported;            ///< The device supports extended synchronization mechanism.
    bool           shaderStorageBufferArrayNonUniformIndexing;  ///< The device supports shader storage buffer array non uniform indexing.
    bool           tensorSupported;                             ///< The device supports tensors.
    bool           computeSupportTensor;                        ///< The device supports tensors in compute stage.
    bool           fragmentSupportTensor;                       ///< The device supports tensors in fragment stage.
    bool           dataGraphSupported;                          ///< The device supports data graphs.
    bool           dataGraphOFSupported;                        ///< The device supports data graph optical flow.
    bool           costSupported;                               ///< The device supports cost map generation in the data graph optical flow.
    bool           hintSupported;                               ///< The device supports hint flow vector map in the data graph optical flow.
    uint32_t       supportedOutputGridSizes;                    ///< The supported output grid sizes bit mask for the data graph optical flow.
    uint32_t       supportedHintGridSizes;                      ///< The supported hint grid sizes bit mask for the data graph optical flow.
    uint32_t       dataGraphOFMinWidth;                         ///< The minimum width for data graph optical flow.
    uint32_t       dataGraphOFMinHeight;                        ///< The minimum height for data graph optical flow.
    uint32_t       dataGraphOFMaxWidth;                         ///< The maximum width for the data graph optical flow.
    uint32_t       dataGraphOFMaxHeight;                        ///< The maximum height for the data graph optical flow.
} FfxDeviceCapabilities;

/// A structure encapsulating a 2-dimensional point, using 32bit unsigned integers.
///
/// @ingroup SDKTypes
typedef struct FfxDimensions2D
{
    uint32_t width;   ///< The width of a 2-dimensional range.
    uint32_t height;  ///< The height of a 2-dimensional range.
} FfxDimensions2D;

/// A structure encapsulating a tensor.
///
/// @ingroup SDKTypes
struct FfxTensorInfo
{
    uint32_t width;      ///< The width of a 2-dimensional range.
    uint32_t height;     ///< The height of a 2-dimensional range.
    uint32_t channels;   ///< The channels of the tensor.
    uint32_t shapeSize;  ///< The size of a tensor shape in bytes.
};

/// A structure encapsulating a 2-dimensional point.
///
/// @ingroup SDKTypes
typedef struct FfxIntCoords2D
{
    int32_t x;  ///< The x coordinate of a 2-dimensional point.
    int32_t y;  ///< The y coordinate of a 2-dimensional point.
} FfxIntCoords2D;

/// A structure encapsulating a 2-dimensional rect.
///
/// @ingroup SDKTypes
typedef struct FfxRect2D
{
    int32_t left;    ///< Left most coordinate
    int32_t top;     ///< Top most coordinate
    int32_t width;   ///< Rect width
    int32_t height;  ///< Rect height
} FfxRect2D;

/// A structure encapsulating a 2-dimensional set of floating point coordinates.
///
/// @ingroup SDKTypes
typedef struct FfxFloatCoords2D
{
    float x;  ///< The x coordinate of a 2-dimensional point.
    float y;  ///< The y coordinate of a 2-dimensional point.
} FfxFloatCoords2D;

/// A structure describing a resource.
///
/// @ingroup SDKTypes
typedef struct FfxResourceDescription
{
    FfxResourceType  type;    ///< The type of the resource.
    FfxSurfaceFormat format;  ///< The surface format.
    union
    {
        uint32_t width;  ///< The width of the texture resource.
        uint32_t size;   ///< The size of the buffer resource.
    };

    union
    {
        uint32_t height;  ///< The height of the texture/tensor resource.
        uint32_t stride;  ///< The stride of the buffer resource.
    };

    union
    {
        uint32_t depth;      ///< The depth of the texture/tensor resource.
        uint32_t alignment;  ///< The alignment of the buffer resource.
        uint32_t channel;    ///< The channel of tensor resource.
    };

    uint32_t         mipCount;  ///< Number of mips (or 0 for full mipchain).
    FfxResourceFlags flags;     ///< A set of <c><i>FfxResourceFlags</i></c> flags.
    FfxResourceUsage usage;     ///< Resource usage flags.

    uint32_t batchSize;  ///< Only used by tensor resource.
    uint32_t shapeSize;  ///< Only used by tensor resource.

} FfxResourceDescription;

/// An outward facing structure containing a resource
///
/// @ingroup SDKTypes
typedef struct FfxResource
{
    void*                  resource;  ///< pointer to the resource.
    FfxResourceDescription description;
    FfxResourceStates      state;
    char                   name[FFX_RESOURCE_NAME_SIZE];  ///< (optional) Resource name.
} FfxResource;

/// A structure describing a static resource.
///
/// @ingroup SDKTypes
typedef struct FfxStaticResourceDescription
{
    const FfxResource* resource;         ///< The resource to register.
    FfxDescriptorType  descriptorType;   ///< The type of descriptor to create.
    uint32_t           descriptorIndex;  ///< The destination index of the descriptor within the static table.

    union
    {
        uint32_t bufferOffset;   ///< The buffer offset in bytes.
        uint32_t textureUavMip;  ///< The mip of the texture resource to create a UAV for.
    };

    uint32_t bufferSize;    ///< The buffer size in bytes.
    uint32_t bufferStride;  ///< The buffer stride in bytes.
} FfxStaticResourceDescription;

/// A structure describing a constant buffer allocation.
///
/// @ingroup SDKTypes
typedef struct FfxConstantAllocation
{
    FfxResource resource;  ///< The resource representing the constant buffer resource.
    FfxUInt64   handle;    ///< The binding handle for the constant buffer

} FfxRootConstantAllocation;

/// A function definition for a constant buffer allocation callback
///
/// Used to provide a constant buffer allocator to the calling backend
///
/// @param [in] data                       The constant buffer data.
/// @param [in] dataSize                   The size of the constant buffer data.
///
///
/// @ingroup SDKTypes
typedef FfxConstantAllocation (*FfxConstantBufferAllocator)(void* data, const FfxUInt64 dataSize);

/// Information about single AMD FidelityFX Breadcrumbs Library GPU memory block.
///
/// Restored for the D3D12 backend. The Arm fork removed breadcrumbs along with the whole
/// D3D12 backend; AMD's FidelityFX SDK 1.1.3 -- which this tree derives from -- still has
/// them, and the D3D12 backend references this type from 86 places. See
/// sdk/src/shared/ffx_breadcrumbs_list.{h,cpp}.
///
/// @ingroup SDKTypes
typedef struct FfxBreadcrumbsBlockData {
    void*    memory;       ///< Pointer to CPU mapped GPU buffer memory.
    void*    heap;         ///< GPU memory block handle.
    void*    buffer;       ///< GPU buffer handle for memory block.
    uint64_t baseAddress;  ///< GPU address of memory block.
    uint32_t nextMarker;   ///< Index of next marker to be saved in memory block.
} FfxBreadcrumbsBlockData;

/// An internal structure containing a handle to a resource and resource views
///
/// @ingroup SDKTypes
typedef struct FfxResourceInternal
{
    int32_t  internalIndex = 0;  ///< The index of the resource.
    uint32_t alignedWidth =
        0;  ///< TODO ARM: remove this, this should be a clean type! AlignedWidth should be resource metadata and its only relevant for buffer-image aliasing.
} FfxResourceInternal;

/// An enumeration for resource init data types that can be passed
///
/// @ingroup SDKTypes
typedef enum FfxResourceInitDataType
{
    FFX_RESOURCE_INIT_DATA_TYPE_INVALID = 0,
    FFX_RESOURCE_INIT_DATA_TYPE_UNINITIALIZED,
    FFX_RESOURCE_INIT_DATA_TYPE_BUFFER,
    FFX_RESOURCE_INIT_DATA_TYPE_VALUE,
} FfxResourceInitDataType;

/// An structure housing all that is needed for resource initialization
///
/// @ingroup SDKTypes
typedef struct FfxResourceInitData
{
    FfxResourceInitDataType type;  ///< Indicates that the resource will be initialized from a buffer or a value, or stay uninitialized.
    size_t                  size;  ///< The size, in bytes, of the resource that needed be initialized.
    union
    {
        void*         buffer;  ///< The buffer used to initialize the resource.
        unsigned char value;   ///< Indicates that the resource will be filled up with this value.
    };

    static FfxResourceInitData FfxResourceInitValue(size_t dataSize, uint8_t initVal)
    {
        FfxResourceInitData initData = {FFX_RESOURCE_INIT_DATA_TYPE_VALUE};
        initData.size                = dataSize;
        initData.value               = initVal;
        return initData;
    }

    static FfxResourceInitData FfxResourceInitBuffer(size_t dataSize, void* pInitData)
    {
        FfxResourceInitData initData = {FFX_RESOURCE_INIT_DATA_TYPE_BUFFER};
        initData.size                = dataSize;
        initData.buffer              = pInitData;
        return initData;
    }

} FfxResourceInitData;

/// An internal structure housing all that is needed for backend resource descriptions
///
/// @ingroup SDKTypes
typedef struct FfxInternalResourceDescription
{
    uint32_t            id;
    const char*         name;
    FfxResourceType     type;
    FfxResourceUsage    usage;
    FfxSurfaceFormat    format;
    uint32_t            width;
    uint32_t            height;
    uint32_t            mipCount;
    FfxResourceFlags    flags;
    FfxResourceInitData initData;
    uint32_t            batchSize;
    uint32_t            channel;
    uint32_t            shapeSize;
} FfxInternalResourceDescription;

/// Tensor aliasing metadata used for data-graph pipeline creation.
///
/// @ingroup SDKTypes
typedef struct FfxDataGraphTensorInfo
{
    const char* resourceName;   ///< Tensor resource name from shader/data-graph reflection.
    bool        bufferAliased;  ///< True when this tensor is expected to be accessed through a buffer alias.
} FfxDataGraphTensorInfo;

/// A structure defining the view to create
///
/// @ingroup SDKTypes
typedef struct FfxViewDescription
{
    bool                     uavView;        ///< Indicates that the view is a UAV.
    FfxResourceViewDimension viewDimension;  ///< The view dimension to map
    union
    {
        int32_t mipLevel;      ///< The mip level of the view, (-1) for default
        int32_t firstElement;  ///< The first element of a buffer view, (-1) for default
    };

    union
    {
        int32_t arraySize;     ///< The array size of the view, (-1) for full depth/array size
        int32_t elementCount;  ///< The number of elements in a buffer view, (-1) for full depth/array size
    };

    int32_t firstSlice;  ///< The first slice to map to, (-1) for default first slice
    char    name[FFX_RESOURCE_NAME_SIZE];
} FfxViewDescription;

static FfxViewDescription s_FfxViewDescInit = {false, FFX_RESOURCE_VIEW_DIMENSION_TEXTURE_2D, -1, -1, -1, ""};

/// A structure defining a resource bind point
///
/// @ingroup SDKTypes
typedef struct FfxResourceBinding
{
    uint32_t slotIndex;                     ///< The slot into which to bind the resource
    uint32_t arrayIndex;                    ///< The resource offset for mip/array access
    uint32_t resourceIdentifier;            ///< A unique resource identifier representing an internal resource index
    char     name[FFX_RESOURCE_NAME_SIZE];  ///< A debug name to help track the resource binding
    uint32_t bindCount;
    uint32_t bindSet;
} FfxResourceBinding;

/// A structure encapsulating a single pass of an algorithm.
///
/// @ingroup SDKTypes
typedef struct FfxPipelineState
{
    FfxRootSignature            rootSignature;          ///< The pipelines rootSignature
    uint32_t                    passId;                 ///< The id of the effect pass this pipeline corresponds to
    FfxCommandSignature         cmdSignature;           ///< The command signature used for indirect workloads
    FfxPipeline                 pipeline;               ///< The pipeline object
    FfxDataGraphPipelineSession session;                ///< The pipeline object
    uint32_t                    uavTextureCount;        ///< Count of Texture UAVs used in this pipeline
    uint32_t                    srvTextureCount;        ///< Count of Texture SRVs used in this pipeline
    uint32_t                    srvBufferCount;         ///< Count of Buffer SRV used in this pipeline
    uint32_t                    uavBufferCount;         ///< Count of Buffer UAVs used in this pipeline
    uint32_t                    staticTextureSrvCount;  ///< Count of static Texture SRVs used in this pipeline
    uint32_t                    staticBufferSrvCount;   ///< Count of static Buffer SRVs used in this pipeline
    uint32_t                    staticTextureUavCount;  ///< Count of static Texture UAVs used in this pipeline
    uint32_t                    staticBufferUavCount;   ///< Count of static Buffer UAVs used in this pipeline
    uint32_t                    constCount;             ///< Count of constant buffers used in this pipeline
    uint32_t                    rtCount;
    uint32_t                    uavTensorCount;
    uint32_t                    srvTensorCount;

    FfxResourceBinding uavTextureBindings[FFX_MAX_NUM_UAVS];               ///< Array of ResourceIdentifiers bound as texture UAVs
    FfxResourceBinding srvTextureBindings[FFX_MAX_NUM_SRVS];               ///< Array of ResourceIdentifiers bound as texture SRVs
    FfxResourceBinding srvBufferBindings[FFX_MAX_NUM_SRVS];                ///< Array of ResourceIdentifiers bound as buffer SRVs
    FfxResourceBinding uavBufferBindings[FFX_MAX_NUM_UAVS];                ///< Array of ResourceIdentifiers bound as buffer UAVs
    FfxResourceBinding constantBufferBindings[FFX_MAX_NUM_CONST_BUFFERS];  ///< Array of ResourceIdentifiers bound as CBs
    FfxResourceBinding rtBindings[FFX_MAX_NUM_RTS];                        ///< Array of ResourceIdentifiers bound as RenderTargets

    // Potentially we need to split tensor bindings into separate arrays for each type.
    // Similar to srv/uav buffers. For now we just have one array as in Unreal 5.6 there is no difference.
    FfxResourceBinding srvTensorBindings[FFX_MAX_NUM_TENSORS];  ///< Array of ResourceIdentifiers bound as tensor SRVs
    FfxResourceBinding uavTensorBindings[FFX_MAX_NUM_TENSORS];  ///< Array of ResourceIdentifiers bound as tensor UAVs

    char name[FFX_RESOURCE_NAME_SIZE];  ///< Pipeline name for debugging/profiling purposes
} FfxPipelineState;

/// A structure containing the data required to create a resource.
///
/// @ingroup SDKTypes
typedef struct FfxCreateResourceDescription
{
    FfxHeapType            heapType;             ///< The heap type to hold the resource, typically <c><i>FFX_HEAP_TYPE_DEFAULT</i></c>.
    FfxResourceDescription resourceDescription;  ///< A resource description.
    FfxResourceStates      initialState;         ///< The initial resource state.
    const char*            name;                 ///< Name of the resource.
    uint32_t               id;                   ///< Internal resource ID.
    FfxResourceInitData    initData;             ///< Buffer containing data to fill the resource.
} FfxCreateResourceDescription;

/// A structure containing the data required to create sampler mappings
///
/// @ingroup SDKTypes
typedef struct FfxSamplerDescription
{
    FfxFilterType  filter;
    FfxAddressMode addressModeU;
    FfxAddressMode addressModeV;
    FfxAddressMode addressModeW;
    FfxBindStage   stage;
    FfxBoolean     unnormalizedCoordinates;
} FfxSamplerDescription;

/// A structure containing the data required to create root constant buffer mappings
///
/// @ingroup SDKTypes
typedef struct FfxRootConstantDescription
{
    uint32_t     size;
    FfxBindStage stage;
} FfxRootConstantDescription;

/// A structure containing the description used to create a
/// <c><i>FfxPipeline</i></c> structure.
///
/// A pipeline is the name given to a shader and the collection of state that
/// is required to dispatch it. In the context of the FidelityFX SDK and its architecture
/// this means that a <c><i>FfxPipelineDescription</i></c> will map to either a
/// monolithic object in an explicit API (such as a
/// <c><i>PipelineStateObject</i></c> in DirectX 12). Or a shader and some
/// ancillary API objects (in something like DirectX 11).
///
/// The <c><i>contextFlags</i></c> field contains a copy of the flags passed
/// to <c><i>ffxContextCreate</i></c> via the <c><i>flags</i></c> field of
/// the <c><i>Ffx<Effect>InitializationParams</i></c> structure. These flags are
/// used to determine which permutation of a pipeline for a specific
/// <c><i>Ffx<Effect>Pass</i></c> should be used to implement the features required
/// by each application, as well as to achieve the best performance on specific
/// target hardware configurations.
///
/// When using one of the provided backends for FidelityFX SDK (such as DirectX 12 or
/// Vulkan) the data required to create a pipeline is compiled off line and
/// included into the backend library that you are using. For cases where the
/// backend interface is overridden by providing custom callback function
/// implementations care should be taken to respect the contents of the
/// <c><i>contextFlags</i></c> field in order to correctly support the options
/// provided by the FidelityFX SDK, and achieve best performance.
/// ///
/// @ingroup SDKTypes
typedef struct FfxPipelineDescription
{
    uint32_t                          contextFlags;   ///< A collection of <c><i>FfxInitializationFlagBits</i></c> which were passed to the context.
    const FfxSamplerDescription*      samplers;       ///< A collection of samplers to use when building the root signature for the pipeline
    size_t                            samplerCount;   ///< Number of samplers to create for the pipeline
    const FfxRootConstantDescription* rootConstants;  ///< A collection of root constant descriptions to use when building the root signature for the pipeline
    uint32_t                          rootConstantBufferCount;       ///< Number of root constant buffers to create for the pipeline
    char                              name[FFX_RESOURCE_NAME_SIZE];  ///< Pipeline name with which to name the pipeline object
    FfxBindStage                      stage;                         ///< The stage(s) for which this pipeline is being built
    uint32_t                          indirectWorkload;              ///< Whether this pipeline has an indirect workload
    FfxSurfaceFormat                  backbufferFormat;              ///< For raster pipelines this contains the backbuffer format
    const FfxDataGraphTensorInfo*     dataGraphTensorInfo;           ///< Optional tensor aliasing metadata used by data-graph backends.
    uint32_t                          dataGraphTensorInfoCount;      ///< Number of elements in <c><i>dataGraphTensorInfo</i></c>.
} FfxPipelineDescription;

/// A structure containing the data required to create a barrier
///
/// @ingroup SDKTypes
typedef struct FfxBarrierDescription
{
    FfxResourceInternal resource;       ///< The resource representation
    FfxBarrierType      barrierType;    ///< The type of barrier to execute
    FfxResourceStates   currentState;   ///< The initial state of the resource
    FfxResourceStates   newState;       ///< The new state of the resource after barrier
    uint32_t            subResourceID;  ///< The subresource id to apply barrier operation to
} FfxBarrierDescription;

/// A structure containing a constant buffer.
///
/// @ingroup SDKTypes
typedef struct FfxConstantBuffer
{
    uint32_t  num32BitEntries;  ///< The size (expressed in 32-bit chunks) stored in data.
    uint32_t* data;             ///< Pointer to constant buffer data
} FfxConstantBuffer;

/// A structure containing a shader resource view.
typedef struct FfxTextureSRV
{
    FfxResourceInternal resource;  ///< Resource corresponding to the shader resource view.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxTextureSRV;

typedef struct FfxTensor
{
    FfxResourceInternal resource;  ///< Resource corresponding to the tensor.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxTensor;

/// A structure containing a shader resource view.
typedef struct FfxBufferSRV
{
    uint32_t            offset;    ///< Offset of resource to bind in bytes.
    uint32_t            size;      ///< Size of resource to bind in bytes.
    uint32_t            stride;    ///< Size of resource to bind in bytes.
    FfxResourceInternal resource;  ///< Resource corresponding to the shader resource view.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxBufferSRV;

/// A structure containing a unordered access view.
typedef struct FfxTextureUAV
{
    uint32_t            mip;       ///< Mip level of resource to bind.
    FfxResourceInternal resource;  ///< Resource corresponding to the unordered access view.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxTextureUAV;

/// A structure containing a Texture Render Target view.
typedef struct FfxTextureRT
{
    FfxResourceInternal resource;  ///< Resource corresponding to the render target view.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxTextureRT;

/// A structure containing a unordered access view.
typedef struct FfxBufferUAV
{
    uint32_t            offset;    ///< Offset of resource to bind in bytes.
    uint32_t            size;      ///< Size of resource to bind in bytes.
    uint32_t            stride;    ///< Size of resource to bind in bytes.
    FfxResourceInternal resource;  ///< Resource corresponding to the unordered access view.
#ifdef FFX_DEBUG
    char name[FFX_RESOURCE_NAME_SIZE];
#endif
} FfxBufferUAV;

/// A structure describing a clear render job.
///
/// @ingroup SDKTypes
typedef struct FfxClearFloatJobDescription
{
    float               color[4];  ///< The clear color of the resource.
    FfxResourceInternal target;    ///< The resource to be cleared.
} FfxClearFloatJobDescription;

/// A structure describing a compute render job.
///
/// @ingroup SDKTypes
typedef struct FfxComputeJobDescription
{
    FfxPipelineState    pipeline;                         ///< Compute pipeline for the render job.
    uint32_t            dimensions[3];                    ///< Dispatch dimensions.
    FfxResourceInternal cmdArgument;                      ///< Dispatch indirect cmd argument buffer
    uint32_t            cmdArgumentOffset;                ///< Dispatch indirect offset within the cmd argument buffer
    FfxTextureSRV       srvTextures[FFX_MAX_NUM_SRVS];    ///< SRV texture resources to be bound in the compute job.
    FfxBufferSRV        srvBuffers[FFX_MAX_NUM_SRVS];     ///< SRV buffer resources to be bound in the compute job.
    FfxTextureUAV       uavTextures[FFX_MAX_NUM_UAVS];    ///< UAV texture resources to be bound in the compute job.
    FfxBufferUAV        uavBuffers[FFX_MAX_NUM_UAVS];     ///< UAV buffer resources to be bound in the compute job.
    FfxTensor           srvTensors[FFX_MAX_NUM_TENSORS];  ///< SRV tensor resources to be bound in the compute job.
    FfxTensor           uavTensors[FFX_MAX_NUM_TENSORS];  ///< UAV tensor resources to be bound in the compute job.

    FfxConstantBuffer cbs[FFX_MAX_NUM_CONST_BUFFERS];  ///< Constant buffers to be bound in the compute job.
#ifdef FFX_DEBUG
    char cbNames[FFX_MAX_NUM_CONST_BUFFERS][FFX_RESOURCE_NAME_SIZE];

    char srvTextureNames[FFX_MAX_NUM_SRVS][64];
    char uavTextureNames[FFX_MAX_NUM_UAVS][64];
    char srvBufferNames[FFX_MAX_NUM_SRVS][64];
    char uavBufferNames[FFX_MAX_NUM_UAVS][64];
    char sampledTextureNames[FFX_MAX_NUM_SRVS][64];
    char tensorNames[FFX_MAX_NUM_TENSORS][64];
#endif
    uint32_t uavTextureMips[FFX_MAX_NUM_UAVS];  ///< Mip level of UAV texture resources to be bound in the compute job.
} FfxComputeJobDescription;

/// A structure describing a fragment render job.
///
/// @ingroup SDKTypes
typedef struct FfxFragmentJobDescription
{
    FfxPipelineState  pipeline;                         ///< Fragment pipeline for the render job.
    uint32_t          viewport[2];                      ///< Viewport dimensions.
    FfxTextureSRV     srvTextures[FFX_MAX_NUM_SRVS];    ///< SRV texture resources to be bound in the fragment job.
    FfxBufferSRV      srvBuffers[FFX_MAX_NUM_SRVS];     ///< SRV buffer resources to be bound in the fragment job.
    FfxTextureUAV     uavTextures[FFX_MAX_NUM_UAVS];    ///< UAV texture resources to be bound in the fragment job.
    FfxTextureRT      rtTextures[FFX_MAX_NUM_RTS];      ///< RT texture resources to be bound in the fragment job.
    FfxBufferUAV      uavBuffers[FFX_MAX_NUM_UAVS];     ///< UAV buffer resources to be bound in the fragment job.
    FfxTensor         srvTensors[FFX_MAX_NUM_TENSORS];  ///< SRV tensor resources to be bound in the fragment job.
    FfxTensor         uavTensors[FFX_MAX_NUM_TENSORS];  ///< UAV tensor resources to be bound in the fragment job.
    FfxConstantBuffer cbs[FFX_MAX_NUM_CONST_BUFFERS];   ///< Constant buffers to be bound in the fragment job.
#ifdef FFX_DEBUG
    char cbNames[FFX_MAX_NUM_CONST_BUFFERS][FFX_RESOURCE_NAME_SIZE];
#endif
} FfxFragmentJobDescription;

/// ARM NOTE: This is Currently unused but it is present in the FFX SDK. Use FragmentJob instead
/// A structure describing a raster job
///
/// @ingroup SDKTypes
typedef struct FfxRasterJobDescription
{
    // ARM NOTE: This is currently unused but it is present in the FFX SDK. Use FragmentJob instead
    FfxPipelineState    pipeline;  ///< Raster pipeline for the render job.
    uint32_t            numVertices;
    FfxResourceInternal renderTarget;
    FfxTextureSRV       srvTextures[FFX_MAX_NUM_SRVS];  ///< SRV texture resources to be bound in the compute job.
    FfxTextureUAV       uavTextures[FFX_MAX_NUM_UAVS];  ///< UAV texture resources to be bound in the compute job.

    FfxConstantBuffer cbs[FFX_MAX_NUM_CONST_BUFFERS];  ///< Constant buffers to be bound in the compute job.
#ifdef FFX_DEBUG
    char cbNames[FFX_MAX_NUM_CONST_BUFFERS][FFX_RESOURCE_NAME_SIZE];
#endif
} FfxRasterJobDescription;

/// A structure describing a data graph job.
///
/// @ingroup SDKTypes
typedef struct FfxDataGraphJobDescription
{
    FfxPipelineState pipeline;                         ///< Data graph pipeline for the render job.
    FfxTensor        srvTensors[FFX_MAX_NUM_TENSORS];  ///< SRV tensor resources to be bound in the compute job.
    FfxTensor        uavTensors[FFX_MAX_NUM_TENSORS];  ///< UAV tensor resources to be bound in the compute job.
    FfxTextureSRV    srvTextures[FFX_MAX_NUM_SRVS];    ///< SRV texture resources to be bound in the data graph job.
    FfxTextureUAV    uavTextures[FFX_MAX_NUM_UAVS];    ///< UAV texture resources to be bound in the data graph job.
    uint32_t         opticalFlowExecuteFlags;          ///< Optional optical flow execute flags.
    uint32_t         meanFlowL1NormHint;               ///< Optional mean L1 norm flow hint. 0 means backend default.
} FfxDataGraphJobDescription;

/// An enumeration for different copy mode for GPU copy job
///
/// @ingroup SDKTypes
typedef enum FfxCopyJobMode
{

    FFX_GPU_COPY_SRC_EXTENT = 0,  ///< When performing a copy, use the extent of the source resource.
    FFX_GPU_COPY_DST_EXTENT = 1,  ///< When performing a copy, use the extent of the destination resource.
    FFX_GPU_COPY_MIN_EXTENT = 2,  ///< When performing a copy, use the minimum extent of the source and destination resources.
} FfxCopyJobMode;

/// A structure describing a copy render job.
///
/// @ingroup SDKTypes
typedef struct FfxCopyJobDescription
{
    FfxResourceInternal src;        ///< Source resource for the copy.
    uint32_t            srcOffset;  ///< Offset into the source buffer in bytes.
    FfxResourceInternal dst;        ///< Destination resource for the copy.
    uint32_t            dstOffset;  ///< Offset into the destination buffer in bytes.
    uint32_t            size;       ///< Number of bytes to copy (Set to 0 to copy entire buffer).
    FfxCopyJobMode      copyMode;   ///< Copy mode to use when performing the copy.
} FfxCopyJobDescription;

typedef struct FfxDiscardJobDescription
{
    FfxResourceInternal target;  ///< The resource to be discarded.
} FfxDiscardJobDescription;

/// A structure describing a clear render job.
///
/// @ingroup SDKTypes
typedef struct FfxClearUintJobDescription
{
    uint32_t            color[4];  ///< The clear color of the resource.
    FfxResourceInternal target;    ///< The resource to be cleared.
} FfxClearUintJobDescription;

/// A structure describing a single render job.
///
/// @ingroup SDKTypes
typedef struct FfxGpuJobDescription
{
    FfxGpuJobType jobType;  ///< Type of the job.
#ifdef FFX_DEBUG
    char jobLabel[FFX_RESOURCE_NAME_SIZE];  ///< Job label for markers
#endif

    union
    {
        FfxClearFloatJobDescription
                                  clearJobDescriptor;  ///< Clear job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_RENDER_JOB_CLEAR_FLOAT</i></c>.
        FfxCopyJobDescription     copyJobDescriptor;   ///< Copy job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_RENDER_JOB_COPY</i></c>.
        FfxComputeJobDescription  computeJobDescriptor;   ///< Compute job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_RENDER_JOB_COMPUTE</i></c>.
        FfxFragmentJobDescription fragmentJobDescriptor;  ///< Fragment job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_GPU_JOB_FRAGMENT</i></c>.
        FfxRasterJobDescription   rasterJobDescriptor;
        FfxBarrierDescription     barrierDescriptor;
        FfxDiscardJobDescription  discardJobDescriptor;
        FfxDataGraphJobDescription
            dataGraphJobDescription;  ///< Data graph job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_GPU_JOB_DATA_GRAPH</i></c>.
        FfxClearUintJobDescription
            clearUintJobDescriptor;  ///< Clear job descriptor. Valid when <c><i>jobType</i></c> is <c><i>FFX_RENDER_JOB_CLEAR_UINT</i></c>.
    };
} FfxGpuJobDescription;

#if defined(POPULATE_SHADER_BLOB_FFX)
#undef POPULATE_SHADER_BLOB_FFX
#endif  // #if defined(POPULATE_SHADER_BLOB_FFX)

/// Macro definition to copy header shader blob information into its SDK structural representation
#define POPULATE_SHADER_BLOB_FFX(info, index)                                                                                                                  \
    {                                                                                                                                                          \
        info[index].blobData, info[index].blobSize, info[index].numConstantBuffers, info[index].numSRVTextures, info[index].numUAVTextures,                    \
            info[index].numSRVBuffers, info[index].numUAVBuffers, info[index].numSamplers, info[index].numRTAccelerationStructures, info[index].numRTTextures, \
            info[index].numSRVTensors, info[index].numUAVTensors, info[index].constantBufferNames, info[index].constantBufferBindings,                         \
            info[index].constantBufferCounts, info[index].constantBufferSpaces, info[index].srvTextureNames, info[index].srvTextureBindings,                   \
            info[index].srvTextureCounts, info[index].srvTextureSpaces, info[index].uavTextureNames, info[index].uavTextureBindings,                           \
            info[index].uavTextureCounts, info[index].uavTextureSpaces, info[index].srvBufferNames, info[index].srvBufferBindings,                             \
            info[index].srvBufferCounts, info[index].srvBufferSpaces, info[index].uavBufferNames, info[index].uavBufferBindings, info[index].uavBufferCounts,  \
            info[index].uavBufferSpaces, info[index].samplerNames, info[index].samplerBindings, info[index].samplerCounts, info[index].samplerSpaces,          \
            info[index].rtAccelerationStructureNames, info[index].rtAccelerationStructureBindings, info[index].rtAccelerationStructureCounts,                  \
            info[index].rtAccelerationStructureSpaces, info[index].rtTextureNames, info[index].rtTextureBindings, info[index].rtTextureCounts,                 \
            info[index].rtTextureSpaces, info[index].srvTensorNames, info[index].srvTensorBindings, info[index].srvTensorCounts, info[index].srvTensorSpaces,  \
            info[index].uavTensorNames, info[index].uavTensorBindings, info[index].uavTensorCounts, info[index].uavTensorSpaces                                \
    }

// A single shader blob and a description of its resources.
///
/// @ingroup SDKTypes
typedef struct FfxShaderBlob
{
    const uint8_t* data;  // A pointer to the blob
    const uint32_t size;  // Size in bytes.

    const uint32_t cbvCount;            // Number of CBs.
    const uint32_t srvTextureCount;     // Number of SRV Textures.
    const uint32_t uavTextureCount;     // Number of UAV Textures.
    const uint32_t srvBufferCount;      // Number of SRV Buffers.
    const uint32_t uavBufferCount;      // Number of UAV Buffers.
    const uint32_t samplerCount;        // Number of Samplers.
    const uint32_t rtAccelStructCount;  // Number of RT Acceleration structures.
    const uint32_t rtTextureCount;      // Number of RenderTarget textures.
    const uint32_t srvTensorCount;      // Number of Tensors.
    const uint32_t uavTensorCount;      // Number of Tensors.

    // constant buffers
    const char**    boundConstantBufferNames;
    const uint32_t* boundConstantBuffers;       // Pointer to an array of bound ConstantBuffers.
    const uint32_t* boundConstantBufferCounts;  // Pointer to an array of bound ConstantBuffer resource counts
    const uint32_t* boundConstantBufferSpaces;  // Pointer to an array of bound ConstantBuffer resource descriptor sets

    // srv textures
    const char**    boundSRVTextureNames;
    const uint32_t* boundSRVTextures;       // Pointer to an array of bound SRV resources.
    const uint32_t* boundSRVTextureCounts;  // Pointer to an array of bound SRV resource counts
    const uint32_t* boundSRVTextureSpaces;  // Pointer to an array of bound SRV resource descriptor sets

    // uav textures
    const char**    boundUAVTextureNames;
    const uint32_t* boundUAVTextures;       // Pointer to an array of bound UAV texture resources.
    const uint32_t* boundUAVTextureCounts;  // Pointer to an array of bound UAV texture resource counts
    const uint32_t* boundUAVTextureSpaces;  // Pointer to an array of bound UAV texture resource descriptor sets

    // srv buffers
    const char**    boundSRVBufferNames;
    const uint32_t* boundSRVBuffers;       // Pointer to an array of bound SRV buffer resources.
    const uint32_t* boundSRVBufferCounts;  // Pointer to an array of bound SRV buffer resource counts
    const uint32_t* boundSRVBufferSpaces;  // Pointer to an array of bound SRV buffer resource descriptor sets

    // uav buffers
    const char**    boundUAVBufferNames;
    const uint32_t* boundUAVBuffers;       // Pointer to an array of bound UAV buffer resources.
    const uint32_t* boundUAVBufferCounts;  // Pointer to an array of bound UAV buffer resource counts
    const uint32_t* boundUAVBufferSpaces;  // Pointer to an array of bound UAV buffer resource descriptor sets

    // samplers
    const char**    boundSamplerNames;
    const uint32_t* boundSamplers;       // Pointer to an array of bound sampler resources.
    const uint32_t* boundSamplerCounts;  // Pointer to an array of bound sampler resource counts
    const uint32_t* boundSamplerSpaces;  // Pointer to an array of bound sampler resource descriptor sets

    // rt acceleration structures
    const char**    boundRTAccelerationStructureNames;
    const uint32_t* boundRTAccelerationStructures;       // Pointer to an array of bound UAV buffer resources.
    const uint32_t* boundRTAccelerationStructureCounts;  // Pointer to an array of bound UAV buffer resource counts
    const uint32_t* boundRTAccelerationStructureSpaces;  // Pointer to an array of bound UAV buffer resource descriptor sets

    // rt textures
    const char**    boundRTTextureNames;
    const uint32_t* boundRTTextures;       // Pointer to an array of bound rt texture resources.
    const uint32_t* boundRTTextureCounts;  // Pointer to an array of bound rt texture resource counts
    const uint32_t* boundRTTextureSpaces;  // Pointer to an array of bound rt texture resource descriptor sets

    // tensors
    const char**    boundSRVTensorNames;
    const uint32_t* boundSRVTensors;       // Pointer to an array of bound tensor resources.
    const uint32_t* boundSRVTensorCounts;  // Pointer to an array of bound tensor resource counts
    const uint32_t* boundSRVTensorSpaces;  // Pointer to an array of bound tensor resource descriptor sets

    // tensors
    const char**    boundUAVTensorNames;
    const uint32_t* boundUAVTensors;       // Pointer to an array of bound tensor resources.
    const uint32_t* boundUAVTensorCounts;  // Pointer to an array of bound tensor resource counts
    const uint32_t* boundUAVTensorSpaces;  // Pointer to an array of bound tensor resource descriptor sets

} FfxShaderBlob;

typedef struct FfxDataGraphBlob
{
    const uint32_t        constantNums;
    const uint32_t*       constantIds;
    const uint32_t*       constantFormats;
    const uint32_t*       constantShapeSize;
    const int64_t**       constantShapes;
    const int64_t*        constantSparsityDimensions;
    const uint32_t*       constantDataSize;
    const unsigned char** constantDatas;

    const char*          graphEntryPoint;
    const uint32_t       graphDataSize;
    const unsigned char* graphData;

    const uint32_t  inputTensorNums;
    const char**    inputTensorNames;
    const uint32_t* inputTensorSets;
    const uint32_t* inputTensorBindings;
    const uint32_t* inputTensorFormats;
    const uint32_t* inputTensorDimSize;
    const int64_t** inputTensorDims;

    const uint32_t  outputTensorNums;
    const char**    outputTensorNames;
    const uint32_t* outputTensorSets;
    const uint32_t* outputTensorBindings;
    const uint32_t* outputTensorFormats;
    const uint32_t* outputTensorDimSize;
    const int64_t** outputTensorDims;
} FfxDataGraphBlob;

// A description of resources for an optical flow dispatch.
///
/// @ingroup SDKTypes
typedef struct FfxOpticalFlowDescription
{
    uint32_t srvTextureCount;  // Number of SRV Textures.
    uint32_t uavTextureCount;  // Number of UAV Textures.
    uint32_t srvTensorCount;   // Number of SRV Tensors.
    uint32_t uavTensorCount;   // Number of UAV Tensors.

    // SRV textures
    const char**                  boundSRVTextureNames;
    uint32_t*                     boundSRVTextures;               // Pointer to an array of bound SRV resources.
    uint32_t*                     boundSRVTextureCounts;          // Pointer to an array of bound SRV resource counts
    uint32_t*                     boundSRVTextureSpaces;          // Pointer to an array of bound SRV resource descriptor sets
    FfxOpticalFlowConnectionType* boundSRVTextureConnectionType;  // Pointer to an array of bound SRV resource connection types
    FfxSurfaceFormat*             boundSRVTextureFormats;

    // UAV textures
    const char**                  boundUAVTextureNames;
    uint32_t*                     boundUAVTextures;               // Pointer to an array of bound UAV texture resources.
    uint32_t*                     boundUAVTextureCounts;          // Pointer to an array of bound UAV texture resource counts
    uint32_t*                     boundUAVTextureSpaces;          // Pointer to an array of bound UAV texture resource descriptor sets
    FfxOpticalFlowConnectionType* boundUAVTextureConnectionType;  // Pointer to an array of bound UAV resource connection types
    FfxSurfaceFormat*             boundUAVTextureFormats;

    // SRV tensors
    const char**                  boundSRVTensorNames;
    uint32_t*                     boundSRVTensors;               // Pointer to an array of bound SRV tensor resources.
    uint32_t*                     boundSRVTensorCounts;          // Pointer to an array of bound SRV tensor resource counts
    uint32_t*                     boundSRVTensorSpaces;          // Pointer to an array of bound SRV tensor resource descriptor sets
    FfxOpticalFlowConnectionType* boundSRVTensorConnectionType;  // Pointer to an array of bound SRV tensor connection types
    FfxSurfaceFormat*             boundSRVTensorFormats;

    // UAV tensors
    const char**                  boundUAVTensorNames;
    uint32_t*                     boundUAVTensors;               // Pointer to an array of bound UAV tensor resources.
    uint32_t*                     boundUAVTensorCounts;          // Pointer to an array of bound UAV tensor resource counts
    uint32_t*                     boundUAVTensorSpaces;          // Pointer to an array of bound UAV tensor resource descriptor sets
    FfxOpticalFlowConnectionType* boundUAVTensorConnectionType;  // Pointer to an array of bound UAV tensor connection types
    FfxSurfaceFormat*             boundUAVTensorFormats;

    // OF metadata
    FfxDimensions2D                dimensions;
    FfxOpticalFlowGridSize         gridSize;
    FfxOpticalFlowPerformanceLevel performanceLevel;

} FfxOpticalFlowDescription;

/// A structure describing the parameters passed from the
/// presentation thread to the ui composition callback function.
///
/// @ingroup SDKTypes
typedef struct FfxPresentCallbackDescription
{
    FfxDevice      device;                 ///< The active device
    FfxCommandList commandList;            ///< The command list on which to register render commands
    FfxResource    currentBackBuffer;      ///< The backbuffer resource with scene information
    FfxResource    currentUI;              ///< Optional UI texture (when doing backbuffer + ui blend)
    FfxResource    outputSwapChainBuffer;  ///< The swapchain target into which to render ui composition
    bool           isInterpolatedFrame;    ///< Whether this is an interpolated or real frame
    bool           usePremulAlpha;         ///< Toggles whether UI gets premultiplied alpha blending or not
    uint64_t       frameID;
} FfxPresentCallbackDescription;

/// A structure describing the parameters to pass to frame generation passes.
///
/// @ingroup SDKTypes
typedef struct FfxFrameGenerationDispatchDescription
{
    FfxCommandList commandList;            ///< The command list on which to register render commands
    FfxResource    presentColor;           ///< The current presentation color, this will be used as interpolation source data.
    FfxResource    outputs[4];             ///< Interpolation destination targets (1 for each frame in numInterpolatedFrames)
    uint32_t       numInterpolatedFrames;  ///< The number of frames to interpolate from the passed in color target
    bool           reset;                  ///< A boolean value which when set to true, indicates the camera has moved discontinuously.
    uint64_t       frameID;

    // Restored from AMD FidelityFX SDK 1.1.3 for the imported D3D12 frame-interpolation
    // swapchain, which fills these in for the frame generation callback. Nothing in the
    // Arm fork's own provider reads them, so they are purely additive.
    FfxRect2D                    interpolationRect;          ///< The sub-rectangle of the presentation colour to interpolate.
    FfxBackbufferTransferFunction backBufferTransferFunction; ///< Transfer function used to linearise the interpolation source colour.
    float                        minMaxLuminance[2];         ///< Min/max luminance of the source, for the transfer function.
} FfxFrameGenerationDispatchDescription;

//struct definition matches FfxApiEffectMemoryUsage
typedef struct FfxEffectMemoryUsage
{
    uint64_t totalUsageInBytes;
    uint64_t aliasableUsageInBytes;
} FfxEffectMemoryUsage;

//struct definition matches FfxApiSwapchainFramePacingTuning
typedef struct FfxSwapchainFramePacingTuning
{
    float safetyMarginInMs;  // in Millisecond
    float varianceFactor;    // valid range [0.0,1.0]
    bool  allowHybridSpin;   //Allows pacing spinlock to sleep.
    uint32_t
         hybridSpinTime;  //How long to spin when hybridSpin is enabled. Measured in timer resolution units. Not recommended to go below 2. Will result in frequent overshoots.
    bool allowWaitForSingleObjectOnFence;  //Allows to call WaitForSingleObject() instead of spinning for fence value.
} FfxSwapchainFramePacingTuning;

#ifdef __cplusplus
}
#endif  // #ifdef __cplusplus
