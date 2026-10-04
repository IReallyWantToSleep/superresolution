// This file is part of the FidelityFX SDK.
//
// Copyright (C) 2024 Advanced Micro Devices, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and /or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

// SPDX-FileCopyrightText: Copyright 2025-2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
// SPDX-License-Identifier: MIT

// @defgroup OpticalFlow

#pragma once

// Include the interface for the backend of the OpticalFlow API.
#include <FidelityFX/host/ffx_interface.h>

/// FidelityFX OpticalFlow major version.
///
/// @ingroup ffxOpticalflow
#define FFX_OPTICALFLOW_VERSION_MAJOR (1)

/// FidelityFX OpticalFlow minor version.
///
/// @ingroup ffxOpticalflow
#define FFX_OPTICALFLOW_VERSION_MINOR (0)

/// FidelityFX OpticalFlow patch version.
///
/// @ingroup ffxOpticalflow
#define FFX_OPTICALFLOW_VERSION_PATCH (0)

/// FidelityFX Optical Flow context count
///
/// Defines the number of internal effect contexts required by Optical Flow
///
/// @ingroup ffxOpticalFlow
#define FFX_OPTICALFLOW_CONTEXT_COUNT (1)

/// The size of the context specified in 32bit size units.
///
/// @ingroup ffxOpticalflow
#define FFX_OPTICALFLOW_CONTEXT_SIZE (FFX_SDK_DEFAULT_CONTEXT_SIZE * 2)

#if defined(__cplusplus)
extern "C" {
#endif  // #if defined(__cplusplus)

typedef enum FfxOpticalflowPass
{
    /*
     * AMD FidelityFX SDK 1.1.3's seven compute passes.
     *
     * These come first because createPipelineStates() iterates from zero to
     * FFX_OPTICALFLOW_PASS_COUNT and creates one pipeline per value.
     */
    FFX_OPTICALFLOW_PASS_PREPARE_LUMA = 0,
    FFX_OPTICALFLOW_PASS_GENERATE_OPTICAL_FLOW_INPUT_PYRAMID,
    FFX_OPTICALFLOW_PASS_GENERATE_SCD_HISTOGRAM,
    FFX_OPTICALFLOW_PASS_COMPUTE_SCD_DIVERGENCE,
    FFX_OPTICALFLOW_PASS_COMPUTE_OPTICAL_FLOW_ADVANCED_V5,
    FFX_OPTICALFLOW_PASS_FILTER_OPTICAL_FLOW_V5,
    FFX_OPTICALFLOW_PASS_SCALE_OPTICAL_FLOW_ADVANCED_V5,

    /* The fork's own MV-hints pass. Kept because the frame generation provider and the
     * MV-hints path name it; deliberately past the count so the pipeline loop skips it. */
    FFX_OPTICALFLOW_PASS_COMPUTE_MOTION_FROM_DEPTH = 7,

    FFX_OPTICALFLOW_PASS_COUNT = 7
} FfxOpticalflowPass;

typedef enum FfxOpticalflowInitializationFlagBits
{
    FFX_OPTICALFLOW_ENABLE_DEPTH_INVERTED    = (1 << 0),
    FFX_OPTICALFLOW_ENABLE_MV_HINTS_FRAGMENT = (1 << 1),

    /* AMD 1.1.3's flag. Bits 0 and 1 were already taken by the fork's two, so this takes
     * the next free bit rather than the value it had upstream. */
    FFX_OPTICALFLOW_ENABLE_TEXTURE1D_USAGE   = (1 << 2),

} FfxOpticalflowInitializationFlagBits;

typedef enum FfxOpticalFlowSearchRange
{
    FFX_OPTICALFLOW_SEARCH_RANGE_DEFAULT = 0,
    FFX_OPTICALFLOW_SEARCH_RANGE_1       = 1,
    FFX_OPTICALFLOW_SEARCH_RANGE_2       = 2,
    FFX_OPTICALFLOW_SEARCH_RANGE_3       = 3,
} FfxOpticalFlowSearchRange;

/// A structure encapsulating the parameters required to initialize
/// FidelityFX OpticalFlow.
///
/// @ingroup ffxOpticalflow
typedef struct FfxOpticalFlowContextDescription
{
    FfxInterface                   backendInterface;       ///< A set of pointers to the backend implementation for FidelityFX SDK
    uint32_t                       flags;                  ///< A collection of <c><i>FfxOpticalflowInitializationFlagBits</i></c>.
    FfxDimensions2D                resolution;             ///< Display size.
    FfxDimensions2D                maxRenderSize;          ///< The maximum size that rendering will be performed at.
    FfxFloat32x4x4                 initialViewProjection;  ///< The initial view-projection matrix used to initialize internal state.
    FfxSurfaceFormat               backBufferFormat;
    FfxOpticalFlowGridSize         gridSize;
    FfxOpticalFlowPerformanceLevel performanceLevel;
} FfxOpticalFlowContextDescription;

/// A structure encapsulating the parameters for dispatching the various passes
/// of FidelityFX Opticalflow.
///
/// @ingroup ffxOpticalflow
typedef struct FfxOpticalFlowDispatchDescription
{
    FfxCommandList commandList;        ///< The <c><i>FfxCommandList</i></c> to record rendering commands into.
    FfxResource    color;              ///< A <c><i>FfxResource</i></c> containing the input color buffer
    FfxResource    depth;              ///< A <c><i>FfxResource</i></c> containing the input depth buffer for computing MV hints
    FfxFloat32x4x4 viewProjection;     ///< The view-projection matrix for the current frame
    FfxResource    opticalFlowVector;  ///< A <c><i>FfxResource</i></c> containing the output motion buffer
    FfxResource
        depthTm1;  ///< A <c><i>FfxResource</i></c> containing the output depth buffer for the previous frame, used for computing MV hints and motion vectors.
    FfxResource
             colorTm1;  ///< A <c><i>FfxResource</i></c> containing the output color buffer for the previous frame, used for computing MV hints and motion vectors.
    uint32_t meanFlowL1NormHint;  ///< Optional mean L1 norm flow hint. 0 means backend default.
    bool     reset;               ///< A boolean value which when set to true, indicates the camera has moved discontinuously.
    /* AMD 1.1.3 target for the scene-change-detection histogram. */
    FfxResource      opticalFlowSCD;

    /*
     * AMD 1.1.3 fields. The fork's implementation derived the transfer function from the
     * backend and left luminance alone; the restored implementation takes both from the
     * caller. Defaults are the linear/smpte2084-agnostic case the fork assumed.
     */
    int              backbufferTransferFunction;
    FfxFloatCoords2D minMaxLuminance;
} FfxOpticalFlowDispatchDescription;

typedef struct FfxOpticalFlowSharedResourceDescriptions
{
    FfxCreateResourceDescription opticalFlowVector;
    FfxCreateResourceDescription depthTm1;
    FfxCreateResourceDescription depthTm1Next;
    FfxCreateResourceDescription colorTm1;
    /*
     * AMD 1.1.3 returns a scene-change-detection resource alongside the flow vector. The
     * fork's data-graph implementation had no SCD output; the restored compute
     * implementation does, so the field is added rather than the provider being changed.
     */
    FfxCreateResourceDescription opticalFlowSCD;
} FfxOpticalFlowSharedResourceDescriptions;

/// A structure encapsulating the FidelityFX OpticalFlow context.
///
/// This sets up an object which contains all persistent internal data and
/// resources that are required by OpticalFlow.
///
/// The <c><i>FfxOpticalFlowContext</i></c> object should have a lifetime matching
/// your use of OpticalFlow. Before destroying the OpticalFlow context care should be taken
/// to ensure the GPU is not accessing the resources created or used by OpticalFlow.
/// It is therefore recommended that the GPU is idle before destroying OpticalFlow
/// OpticalFlow context.
///
/// @ingroup ffxOpticalflow
typedef struct FfxOpticalFlowContext
{
    uint32_t data[FFX_OPTICALFLOW_CONTEXT_SIZE];  ///< An opaque set of <c>uint32_t</c> which contain the data for the context.
} FfxOpticalFlowContext;

/*
 * AMD 1.1.3 spellings.
 *
 * The fork renamed these to FfxOpticalFlow* (capital F) for the C++ binding layer that
 * ffx_provider_framegeneration.cpp uses, and the C++ binding layer is not being touched.
 * The compute-shader implementation restored from 1.1.3 keeps its own spellings, so the
 * two are aliased together here rather than renamed in either place.
 */
typedef FfxOpticalFlowContextDescription         FfxOpticalflowContextDescription;
typedef FfxOpticalFlowDispatchDescription        FfxOpticalflowDispatchDescription;
typedef FfxOpticalFlowSharedResourceDescriptions FfxOpticalflowSharedResourceDescriptions;
typedef FfxOpticalFlowContext                    FfxOpticalflowContext;

/// Create a FidelityFX OpticalFlow context from the parameters
/// programmed to the <c><i>FfxOpticalFlowContextDescription</i></c> structure.
///
/// The context structure is the main object used to interact with the OpticalFlow
/// API, and is responsible for the management of the internal resources used
/// by the OpticalFlow algorithm. When this API is called, multiple calls will be
/// made via the pointers contained in the <c><i>callbacks</i></c> structure.
/// These callbacks will attempt to retreive the device capabilities, and
/// create the internal resources, and pipelines required by OpticalFlow's
/// frame-to-frame function. Depending on the precise configuration used when
/// creating the <c><i>FfxOpticalFlowContext</i></c> a different set of resources and
/// pipelines might be requested via the callback functions.
///
/// The flags included in the <c><i>flags</i></c> field of
/// <c><i>FfxOpticalFlowContext</i></c> how match the configuration of your
/// application as well as the intended use of OpticalFlow. It is important that these
/// flags are set correctly (as well as a correct programmed
/// <c><i>FfxOpticalFlowContextDescription</i></c>) to ensure correct operation. It is
/// recommended to consult the overview documentation for further details on
/// how OpticalFlow should be integerated into an application.
///
/// When the <c><i>FfxOpticalFlowContext</i></c> is created, you should use the
/// <c><i>ffxOpticalFlowContextDispatch</i></c> function each frame where FSR3
/// upscaling should be applied. See the documentation of
/// <c><i>ffxOpticalFlowContextDispatch</i></c> for more details.
///
/// The <c><i>FfxOpticalFlowContext</i></c> should be destroyed when use of it is
/// completed, typically when an application is unloaded or OpticalFlow is
/// disabled by a user. To destroy the OpticalFlow context you should call
/// <c><i>ffxOpticalFlowContextDestroy</i></c>.
///
/// @param [out] context                A pointer to a <c><i>FfxOpticalFlowContext</i></c> structure to populate.
/// @param [in]  contextDescription     A pointer to a <c><i>FfxOpticalFlowContextDescription</i></c> structure.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_CODE_NULL_POINTER         The operation failed because either <c><i>context</i></c> or <c><i>contextDescription</i></c> was <c><i>NULL</i></c>.
/// @retval
/// FFX_ERROR_INCOMPLETE_INTERFACE      The operation failed because the <c><i>FfxOpticalFlowContextDescription.callbacks</i></c>  was not fully specified.
/// @retval
/// FFX_ERROR_BACKEND_API_ERROR         The operation failed because of an error returned from the backend.
///
/// @ingroup ffxOpticalflow
FFX_API FfxErrorCode ffxOpticalFlowContextCreate(FfxOpticalFlowContext* context, FfxOpticalFlowContextDescription* contextDescription);

FFX_API FfxErrorCode ffxOpticalFlowGetSharedResourceDescriptions(FfxOpticalFlowContext* context, FfxOpticalFlowSharedResourceDescriptions* SharedResources);

FFX_API FfxErrorCode ffxOpticalFlowContextDispatch(FfxOpticalFlowContext* context, FfxOpticalFlowDispatchDescription* dispatchDescription);

/// Get the default optical flow grid size from the backend interface.
///
/// @param [in] backendInterface                A reference to a <c><i>FfxInterface</i></c> structure.
///
/// @retval
/// The optical flow grid size.
///
/// @ingroup ffxOpticalflow
FFX_API FfxOpticalFlowGridSize ffxGetDefaultDataGraphOpticalFlowGridSize(FfxInterface& backendInterface);

/// Check if a grid size is supported by the backend interface.
///
/// @param [in] backendInterface                A reference to a <c><i>FfxInterface</i></c> structure.
/// @param      gridSize                        The optical flow grid size to check.
///
/// @retval
/// True if the optical flow grid size is supported，false otherwise.
///
/// @ingroup ffxOpticalflow
FFX_API bool ffxOpticalFlowGridSizeSupported(FfxInterface& backendInterface, const FfxOpticalFlowGridSize gridSize);

/// Get the optical flow grid size from the context.
///
/// @param [out] context                A pointer to a <c><i>FfxOpticalFlowContext</i></c>.
///
/// @retval
/// The optical flow grid size.
///
/// @ingroup ffxOpticalflow
FFX_API FfxOpticalFlowGridSize ffxOpticalFlowGetGridSize(FfxOpticalFlowContext* context);

/// Get the optical flow texture size from the context.
///
/// @param [out] context                A pointer to a <c><i>ArmOpticalFlowContext</i></c>.
///
/// @retval
/// The optical flow texture dimensions.
///
/// @ingroup ffxOpticalflow
FFX_API FfxDimensions2D ffxOpticalFlowGetSize(FfxOpticalFlowContext* context);

/// Destroy the FidelityFX OpticalFlow context.
///
/// @param [out] context                A pointer to a <c><i>FfxOpticalFlowContext</i></c> structure to destroy.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_CODE_NULL_POINTER         The operation failed because either <c><i>context</i></c> was <c><i>NULL</i></c>.
///
/// @ingroup ffxOpticalflow
FFX_API FfxErrorCode ffxOpticalFlowContextDestroy(FfxOpticalFlowContext* context);

/// Queries the effect version number.
///
/// @returns
/// The SDK version the effect was built with.
///
/// @ingroup ffxOpticalflow
FFX_API FfxVersionNumber ffxOpticalFlowGetEffectVersion();

#if defined(__cplusplus)
}
#endif  // #if defined(__cplusplus)
