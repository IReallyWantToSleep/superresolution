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

// Include the interface for the backend of the NSS API.
#include "ffx_interface.h"

/// @defgroup ffxNss NSS
/// NSS runtime library
///
/// @ingroup SDKComponents

/// NSS major version.
///
/// @ingroup ffxNss
#define FFX_NSS_VERSION_MAJOR (1)

/// NSS minor version.
///
/// @ingroup ffxNss
#define FFX_NSS_VERSION_MINOR (0)

/// NSS patch version.
///
/// @ingroup ffxNss
#define FFX_NSS_VERSION_PATCH (0)

/// NSS context count
///
/// Defines the number of internal effect contexts required by NSS
///
/// @ingroup ffxNss
#define FFX_NSS_CONTEXT_COUNT 1

/// The size of the context specified in 32bit values.
///
/// @ingroup ffxNss
#define FFX_NSS_CONTEXT_SIZE (24576)

#if defined(__cplusplus)
extern "C" {
#endif  // #if defined(__cplusplus)

/// An enumeration of all the passes which constitute the NSS algorithm.
///
/// NSS is implemented as a composite of several compute passes each
/// computing a key part of the final result. Each call to the
/// <c><i>FfxNssScheduleGpuJobFunc</i></c> callback function will
/// correspond to a single pass included in <c><i>FfxNssPass</i></c>. For a
/// more comprehensive description of each pass, please refer to the NSS
/// reference documentation.
///
/// Please note in some cases e.g.: <c><i>FFX_NSS_PASS_ACCUMULATE</i></c>
/// and <c><i>FFX_NSS_PASS_ACCUMULATE_SHARPEN</i></c> either one pass or the
/// other will be used (they are mutually exclusive). The choice of which will
/// depend on the way the <c><i>FfxNssContext</i></c> is created and the
/// precise contents of <c><i>FfxNssDispatchParamters</i></c> each time a call
/// is made to <c><i>ffxNssContextDispatch</i></c>.
///
/// @ingroup ffxNss
typedef enum FfxNssPass
{
    FFX_NSS_PASS_DEPTH_SCATTER       = 0,  ///< A pass which performs depth scattering.
    FFX_NSS_PASS_DISOCCLUSION_MASK   = 1,  ///< A pass which computes low-quality disocclusion mask.
    FFX_NSS_PASS_PREPROCESS          = 2,  ///< A pass which performs preprocessing.
    FFX_NSS_PASS_AUTO_EXPOSURE       = 3,  ///< A pass which computes the current frame exposure from the input color.
    FFX_NSS_PASS_GENERATE_OFFSET_LUT = 4,  ///< A pass which builds the dynamic postprocess offset LUT.
    FFX_NSS_PASS_POSTPROCESS         = 5,  ///< A pass which performs postprocessing.
    FFX_NSS_PASS_DEBUG_VIEW          = 6,  ///< A pass which overlays debug views.
    FFX_NSS_PASS_COUNT                     ///< The number of passes performed by NSS.
} FfxNssPass;

/// An enumeration of all the quality modes supported by NSS.
///
/// In order to provide a consistent user experience across multiple
/// applications which implement NSS. It is strongly recommended that the
/// following preset scaling factors are made available through your
/// application's user interface.
///
/// If your application does not expose the notion of preset scaling factors
/// for upscaling algorithms (perhaps instead implementing a fixed ratio which
/// is immutable) or implementing a more dynamic scaling scheme (such as
/// dynamic resolution scaling), then there is no need to use these presets.
///
/// Please note that <c><i>FFX_NSS_QUALITY_MODE_ULTRA_PERFORMANCE</i></c> is
/// an optional mode which may introduce significant quality degradation in the
/// final image. As such it is recommended that you evaluate the final results
/// of using this scaling mode before deciding if you should include it in your
/// application.
///
/// @ingroup ffxNss
typedef enum FfxNssShaderQualityMode
{
    FFX_NSS_SHADER_QUALITY_MODE_QUALITY     = 0,  ///< Perform upscaling with a shader quality mode of 'Quality'
    FFX_NSS_SHADER_QUALITY_MODE_BALANCED    = 1,  ///< Perform upscaling with a shader quality mode of 'Balance'
    FFX_NSS_SHADER_QUALITY_MODE_PERFORMANCE = 2,  ///< Perform upscaling with a shader quality mode of 'Performance'
} FfxNssShaderQualityMode;

/// An enumeration of bit flags used when creating a
/// <c><i>FfxNssContext</i></c>. See <c><i>FfxNssContextDescription</i></c>.
///
/// @ingroup ffxNss
typedef enum FfxNssInitializationFlagBits
{
    FFX_NSS_CONTEXT_FLAG_QUANTIZED          = (1 << 0),  ///< Use a quantized data graph. Resources will be quantized to 8 bits.
    FFX_NSS_CONTEXT_FLAG_HIGH_DYNAMIC_RANGE = (1 << 1),  ///< A bit indicating if the input color data provided is using a high-dynamic range.
    FFX_NSS_CONTEXT_FLAG_DEPTH_INVERTED     = (1 << 2),  ///< A bit indicating that the input depth buffer data provided is inverted [1..0].
    FFX_NSS_CONTEXT_FLAG_DEPTH_INFINITE     = (1 << 3),  ///< A bit indicating that the input depth buffer data provided is using an infinite far plane.
    FFX_NSS_CONTEXT_FLAG_RESAMPLE_BICUBIC   = (1 << 4),  ///< A bit indicating sample using Bicubic filtering
    FFX_NSS_CONTEXT_FLAG_ALLOW_16BIT        = (1 << 5),  ///< A bit indicating that the runtime should allow 16bit resources to be used.
    FFX_NSS_CONTEXT_FLAG_MANAGE_HISTORY =
        (1
         << 6),  ///< A bit indicating that the SDK manages the previous history textures internally. When not set the user must supply outputTm1 each dispatch.
    FFX_NSS_CONTEXT_FLAG_PRE_PROCESS_FRAGMENT  = (1 << 7),  ///< A bit indicating that the pre-process shader should run as a fragment job when supported.
    FFX_NSS_CONTEXT_FLAG_POST_PROCESS_FRAGMENT = (1 << 8),  ///< A bit indicating that the post-process shader should run as a fragment job when supported.
    FFX_NSS_CONTEXT_FLAG_ENABLE_MOTION_VECTORS_JITTER_CANCELLATION = (1 << 9),  ///< Motion vectors contain the camera jitter delta.
    FFX_NSS_CONTEXT_FLAG_ENABLE_AUTO_EXPOSURE = (1 << 10),  ///< Compute exposure from the input color.
} FfxNssInitializationFlagBits;

/// Pass a string message
///
/// Used for debug messages.
///
/// @param [in] type                       The type of message.
/// @param [in] message                    A string message to pass.
///
///
/// @ingroup ffxNss
typedef void (*FfxNssMessage)(FfxMsgType type, const char* message);

/// A structure encapsulating the parameters required to initialize NSS.
///
/// @ingroup ffxNss
typedef struct FfxNssContextDescription
{
    FfxNssShaderQualityMode qualityMode;  ///< What shader quality mode to use
    uint32_t                flags;        ///< A collection of <c><i>FfxNssInitializationFlagBits</i></c>.
    FfxDimensions2D         renderSize;   ///< The size that rendering will be performed at. This must match the size when dispatching.
    FfxDimensions2D         upscaleSize;  ///< The size of the output resolution targeted by the upscaling process. This must match the size when dispatching.
    FfxDimensions2D         displaySize;  ///< The size of the presentation resolution targeted by the upscaling process.

    FfxInterface  backendInterface;  ///< A set of pointers to the backend implementation for FidelityFX SDK
    FfxNssMessage fpMessage;         ///< A pointer to a function that can receive messages from the runtime.
} FfxNssContextDescription;

/// @ingroup ffxNss
typedef enum FfxNssDispatchFlags
{
    FFX_NSS_DISPATCH_FLAG_ENABLE_DEBUG_CHECKING =
        (1 << 0),  ///< A bit indicating that dispatch parameters should be validated and errors reported via the message callback.
    FFX_NSS_DISPATCH_FLAG_DRAW_DEBUG_VIEW =
        (1 << 1),  ///< A bit indicating that the debug view pass should run, writing intermediate resources into the debugViews resource.
} FfxNssDispatchFlags;

/// A structure encapsulating the parameters for dispatching the various passes
/// of NSS.
///
/// @ingroup ffxNss
typedef struct FfxNssDispatchDescription
{
    FfxCommandList commandList;  ///< The <c><i>FfxCommandList</i></c> to record NSS rendering commands into.
    FfxResource    color;        ///< A <c><i>FfxResource</i></c> containing the color buffer for the current frame (at render resolution).
    FfxResource    depth;        ///< A <c><i>FfxResource</i></c> containing 32bit depth values for the current frame (at render resolution).
    FfxResource
                motionVectors;  ///< A <c><i>FfxResource</i></c> containing 2-dimensional motion vectors (at render resolution if <c><i>FFX_NSS_ENABLE_DISPLAY_RESOLUTION_MOTION_VECTORS</i></c> is not set).
    FfxResource output;         ///< A <c><i>FfxResource</i></c> containing the output color buffer for the current frame (at presentation resolution).
    FfxResource
        outputTm1;  ///< A <c><i>FfxResource</i></c> containing the output color buffer for the previous frame (at presentation resolution). Ignored when FFX_NSS_CONTEXT_FLAG_MANAGE_HISTORY is set; required otherwise. The caller is responsible for ping-ponging: pass frame N's output as frame N+1's outputTm1.

    FfxFloatCoords2D jitterOffset;            ///< The subpixel jitter offset applied to the camera.
    FfxDimensions2D  upscaleSize;             ///< The resolution that was used for rendering the output resources.
    FfxDimensions2D  renderSize;              ///< The resolution that was used for rendering the input resources.
    float            cameraNear;              ///< The distance to the near plane of the camera.
    float            cameraFar;               ///< The distance to the far plane of the camera.
    float            cameraFovAngleVertical;  ///< The camera angle field of view in the vertical direction (expressed in radians).

    FfxResource      exposure;           ///< Optional 1x1 exposure texture. Ignored when auto exposure is enabled.
    FfxFloatCoords2D motionVectorScale;  ///< The scale applied to the motion vectors.

    float    frameTimeDelta;  ///< The time elapsed since the last frame (expressed in milliseconds).
    bool     reset;           ///< A boolean value which when set to true, indicates the camera has moved discontinuously.
    uint32_t flags;           ///< A combination of <c><i>FfxNssDispatchFlags</i></c>.
    FfxResource
             debugViews;  ///< Output resource for debug views. Required when FFX_NSS_DISPATCH_FLAG_DRAW_DEBUG_VIEW is set. Must be display-resolution (upscaleSize) R11G11B10 or equivalent.
    uint32_t debugViewMode;  ///< Debug view display mode (see NssDebugViewMode). 0 = all tiles in 4×4 grid; 1–16 = single tile fullscreen.
} FfxNssDispatchDescription;

/// A structure encapsulating the parameters for automatic generation of a reactive mask
///
/// @ingroup ffxNss
typedef struct FfxNssGenerateReactiveDescription
{
    FfxCommandList commandList;      ///< The <c><i>FfxCommandList</i></c> to record NSS rendering commands into.
    FfxResource    colorOpaqueOnly;  ///< A <c><i>FfxResource</i></c> containing the opaque only color buffer for the current frame (at render resolution).
    FfxResource colorPreUpscale;  ///< A <c><i>FfxResource</i></c> containing the opaque+translucent color buffer for the current frame (at render resolution).
    FfxResource outReactive;      ///< A <c><i>FfxResource</i></c> containing the surface to generate the reactive mask into.
    FfxDimensions2D renderSize;   ///< The resolution that was used for rendering the input resources.
    float           scale;        ///< A value to scale the output
    float           cutoffThreshold;  ///< A threshold value to generate a binary reactive mask
    float           binaryValue;      ///< A value to set for the binary reactive mask
    uint32_t        flags;            ///< Flags to determine how to generate the reactive mask
} FfxNssGenerateReactiveDescription;

/// A structure encapsulating the NSS context.
///
/// This sets up an object which contains all persistent internal data and
/// resources that are required by NSS.
///
/// The <c><i>FfxNssContext</i></c> object should have a lifetime matching
/// your use of NSS. Before destroying the NSS context care should be taken
/// to ensure the GPU is not accessing the resources created or used by NSS.
/// It is therefore recommended that the GPU is idle before destroying the
/// NSS context.
///
/// @ingroup ffxNss
typedef struct FfxNssContext
{
    uint32_t data[FFX_NSS_CONTEXT_SIZE * 2];  ///< An opaque set of <c>uint32_t</c> which contain the data for the context.
} FfxNssContext;

/// Create a NSS context from the parameters
/// programmed to the <c><i>FfxNssCreateParams</i></c> structure.
///
/// The context structure is the main object used to interact with the NSS
/// API, and is responsible for the management of the internal resources used
/// by the NSS algorithm. When this API is called, multiple calls will be
/// made via the pointers contained in the <c><i>callbacks</i></c> structure.
/// These callbacks will attempt to retreive the device capabilities, and
/// create the internal resources, and pipelines required by NSS's
/// frame-to-frame function. Depending on the precise configuration used when
/// creating the <c><i>FfxNssContext</i></c> a different set of resources and
/// pipelines might be requested via the callback functions.
///
/// The flags included in the <c><i>flags</i></c> field of
/// <c><i>FfxNssContext</i></c> how match the configuration of your
/// application as well as the intended use of NSS. It is important that these
/// flags are set correctly (as well as a correct programmed
/// <c><i>FfxNssDispatchDescription</i></c>) to ensure correct operation. It is
/// recommended to consult the overview documentation for further details on
/// how NSS should be integerated into an application.
///
/// When the <c><i>FfxNssContext</i></c> is created, you should use the
/// <c><i>ffxNssContextDispatch</i></c> function each frame where NSS
/// upscaling should be applied. See the documentation of
/// <c><i>ffxNssContextDispatch</i></c> for more details.
///
/// The <c><i>FfxNssContext</i></c> should be destroyed when use of it is
/// completed, typically when an application is unloaded or NSS upscaling is
/// disabled by a user. To destroy the NSS context you should call
/// <c><i>ffxNssContextDestroy</i></c>.
///
/// @param [out] pContext                A pointer to a <c><i>FfxNssContext</i></c> structure to populate.
/// @param [in]  pContextDescription     A pointer to a <c><i>FfxNssContextDescription</i></c> structure.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_CODE_NULL_POINTER         The operation failed because either <c><i>context</i></c> or <c><i>contextDescription</i></c> was <c><i>NULL</i></c>.
/// @retval
/// FFX_ERROR_INCOMPLETE_INTERFACE      The operation failed because the <c><i>FfxNssContextDescription.callbacks</i></c>  was not fully specified.
/// @retval
/// FFX_ERROR_BACKEND_API_ERROR         The operation failed because of an error returned from the backend.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssContextCreate(FfxNssContext* pContext, const FfxNssContextDescription* pContextDescription);

/// Dispatch the various passes that constitute NSS.
///
/// NSS is a composite effect, meaning that it is compromised of multiple
/// constituent passes (implemented as one or more clears, copies and compute
/// dispatches). The <c><i>ffxNssContextDispatch</i></c> function is the
/// function which (via the use of the functions contained in the
/// <c><i>callbacks</i></c> field of the <c><i>FfxNssContext</i></c>
/// structure) utlimately generates the sequence of graphics API calls required
/// each frame.
///
/// As with the creation of the <c><i>FfxNssContext</i></c> correctly
/// programming the <c><i>FfxNssDispatchDescription</i></c> is key to ensuring
/// the correct operation of NSS. It is particularly important to ensure that
/// camera jitter is correctly applied to your application's projection matrix
/// (or camera origin for raytraced applications). NSS provides the
/// <c><i>ffxNssGetJitterPhaseCount</i></c> and
/// <c><i>ffxNssGetJitterOffset</i></c> entry points to help applications
/// correctly compute the camera jitter. Whatever jitter pattern is used by the
/// application it should be correctly programmed to the
/// <c><i>jitterOffset</i></c> field of the <c><i>dispatchDescription</i></c>
/// structure. For more guidance on camera jitter please consult the
/// documentation for <c><i>ffxNssGetJitterOffset</i></c> as well as the
/// accompanying overview documentation for NSS.
///
/// @param [in] pContext                 A pointer to a <c><i>FfxNssContext</i></c> structure.
/// @param [in] pDispatchDescription     A pointer to a <c><i>FfxNssDispatchDescription</i></c> structure.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_CODE_NULL_POINTER         The operation failed because either <c><i>context</i></c> or <c><i>dispatchDescription</i></c> was <c><i>NULL</i></c>.
/// @retval
/// FFX_ERROR_OUT_OF_RANGE              The operation failed because <c><i>dispatchDescription.renderSize</i></c> was larger than the maximum render resolution.
/// @retval
/// FFX_ERROR_NULL_DEVICE               The operation failed because the device inside the context was <c><i>NULL</i></c>.
/// @retval
/// FFX_ERROR_BACKEND_API_ERROR         The operation failed because of an error returned from the backend.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssContextDispatch(FfxNssContext* pContext, const FfxNssDispatchDescription* pDispatchDescription);

/// A helper function generate a Reactive mask from an opaque only texure and one containing translucent objects.
///
/// @param [in] pContext                 A pointer to a <c><i>FfxNssContext</i></c> structure.
/// @param [in] pParams                  A pointer to a <c><i>FfxNssGenerateReactiveDescription</i></c> structure
///
/// @retval
/// FFX_OK                              The operation completed successfully.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssContextGenerateReactiveMask(FfxNssContext* pContext, const FfxNssGenerateReactiveDescription* pParams);

/// Destroy the NSS context.
///
/// @param [out] pContext                A pointer to a <c><i>FfxNssContext</i></c> structure to destroy.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_CODE_NULL_POINTER         The operation failed because either <c><i>context</i></c> was <c><i>NULL</i></c>.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssContextDestroy(FfxNssContext* pContext);

/// A helper function to calculate the jitter phase count from display
/// resolution.
///
/// For more detailed information about the application of camera jitter to
/// your application's rendering please refer to the
/// <c><i>ffxNssGetJitterOffset</i></c> function.
///
/// The table below shows the jitter phase count which this function
/// would return for each of the quality presets.
///
/// @param [in] renderWidth             The render resolution width.
/// @param [in] displayWidth            The display resolution width.
///
/// @returns
/// The jitter phase count for the scaling factor between <c><i>renderWidth</i></c> and <c><i>displayWidth</i></c>.
///
/// @ingroup ffxNss
FFX_API int32_t ffxNssGetJitterPhaseCount(int32_t renderWidth, int32_t displayWidth);

/// A helper function to calculate the subpixel jitter offset.
///
/// NSS relies on the application to apply sub-pixel jittering while rendering.
/// This is typically included in the projection matrix of the camera. To make
/// the application of camera jitter simple, the NSS API provides a small set
/// of utility function which computes the sub-pixel jitter offset for a
/// particular frame within a sequence of separate jitter offsets. To begin, the
/// index within the jitter phase must be computed. To calculate the
/// sequence's length, you can call the <c><i>ffxNssGetJitterPhaseCount</i></c>
/// function. The index should be a value which is incremented each frame modulo
/// the length of the sequence computed by <c><i>ffxNssGetJitterPhaseCount</i></c>.
/// The index within the jitter phase  is passed to
/// <c><i>ffxNssGetJitterOffset</i></c> via the <c><i>index</i></c> parameter.
///
/// This function uses a Halton(2,3) sequence to compute the jitter offset.
/// The ultimate index used for the sequence is <c><i>index</i></c> %
/// <c><i>phaseCount</i></c>.
///
/// It is important to understand that the values returned from the
/// <c><i>ffxNssGetJitterOffset</i></c> function are in unit pixel space, and
/// in order to composite this correctly into a projection matrix we must
/// convert them into projection offsets. This is done as per the pseudo code
/// listing which is shown below.
///
///     const int32_t jitterPhaseCount = ffxNssGetJitterPhaseCount(renderWidth, displayWidth);
///
///     float jitterX = 0;
///     float jitterY = 0;
///     ffxNssGetJitterOffset(&jitterX, &jitterY, index, jitterPhaseCount);
///
///     const float jitterX = 2.0f * jitterX / (float)renderWidth;
///     const float jitterY = -2.0f * jitterY / (float)renderHeight;
///     const Matrix4 jitterTranslationMatrix = translateMatrix(Matrix3::identity, Vector3(jitterX, jitterY, 0));
///     const Matrix4 jitteredProjectionMatrix = jitterTranslationMatrix * projectionMatrix;
///
/// Jitter should be applied to all rendering. This includes opaque, alpha
/// transparent, and raytraced objects. For rasterized objects, the sub-pixel
/// jittering values calculated by the <c><i>iffxNssGetJitterOffset</i></c>
/// function can be applied to the camera projection matrix which is ultimately
/// used to perform transformations during vertex shading. For raytraced
/// rendering, the sub-pixel jitter should be applied to the ray's origin,
/// often the camera's position.
///
/// Whether you elect to use the <c><i>ffxNssGetJitterOffset</i></c> function
/// or your own sequence generator, you must program the
/// <c><i>jitterOffset</i></c> field of the
/// <c><i>FfxNssDispatchParameters</i></c> structure in order to inform NSS
/// of the jitter offset that has been applied in order to render each frame.
///
/// If not using the recommended <c><i>ffxNssGetJitterOffset</i></c> function,
/// care should be taken that your jitter sequence never generates a null vector;
/// that is value of 0 in both the X and Y dimensions.
///
/// @param [out] pOutX                   A pointer to a <c>float</c> which will contain the subpixel jitter offset for the x dimension.
/// @param [out] pOutY                   A pointer to a <c>float</c> which will contain the subpixel jitter offset for the y dimension.
/// @param [in] index                   The index within the jitter sequence.
/// @param [in] phaseCount              The length of jitter phase. See <c><i>ffxNssGetJitterPhaseCount</i></c>.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_INVALID_POINTER           Either <c><i>outX</i></c> or <c><i>outY</i></c> was <c>NULL</c>.
/// @retval
/// FFX_ERROR_INVALID_ARGUMENT          Argument <c><i>phaseCount</i></c> must be greater than 0.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssGetJitterOffset(float* pOutX, float* pOutY, int32_t index, int32_t phaseCount);

/// Query whether the preprocess and postprocess stages are executing as
/// fragment jobs or compute jobs for the given NSS context.
///
/// The answer is determined at context-creation time based on the requested
/// flags and the device capabilities detected at that point. A caller may
/// pass <c>NULL</c> for either output pointer if that value is not needed.
///
/// @param [in]  pContext                    A pointer to an initialised <c><i>FfxNssContext</i></c>.
/// @param [out] pOutPreProcessUsesFragment  Receives <c>true</c> if pre-process runs as a fragment job. May be <c>NULL</c>.
/// @param [out] pOutPostProcessUsesFragment Receives <c>true</c> if post-process runs as a fragment job. May be <c>NULL</c>.
///
/// @retval
/// FFX_OK                              The operation completed successfully.
/// @retval
/// FFX_ERROR_INVALID_POINTER           <c><i>pContext</i></c> was <c>NULL</c>.
///
/// @ingroup ffxNss
FFX_API FfxErrorCode ffxNssGetPipelineStageInfo(FfxNssContext* pContext, bool* pOutPreProcessUsesFragment, bool* pOutPostProcessUsesFragment);

/// A helper function to check if a resource is
/// <c><i>FFX_NSS_RESOURCE_IDENTIFIER_NULL</i></c>.
///
/// @param [in] resource                A <c><i>FfxResource</i></c>.
///
/// @returns
/// true                                The <c><i>resource</i></c> was not <c><i>FFX_NSS_RESOURCE_IDENTIFIER_NULL</i></c>.
/// @returns
/// false                               The <c><i>resource</i></c> was <c><i>FFX_NSS_RESOURCE_IDENTIFIER_NULL</i></c>.
///
/// @ingroup ffxNss
FFX_API bool ffxNssResourceIsNull(const FfxResource& resource);

#if defined(__cplusplus)
}
#endif  // #if defined(__cplusplus)
