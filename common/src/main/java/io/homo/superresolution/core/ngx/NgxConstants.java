/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.ngx;

public final class NgxConstants {
    public static final int VERSION_API = 0x0000015;

    public static final int RESULT_SUCCESS = 0x00000001;
    public static final int RESULT_FAIL = 0xBAD00000;

    public static final int ENGINE_CUSTOM = 0;
    public static final int ENGINE_UNREAL = 1;
    public static final int ENGINE_UNITY = 2;
    public static final int ENGINE_OMNIVERSE = 3;

    public static final int FEATURE_SUPER_SAMPLING = 1;
    public static final int FEATURE_IMAGE_SIGNAL_PROCESSING = 9;
    public static final int FEATURE_DEEP_RESOLVE = 10;
    public static final int FEATURE_FRAME_GENERATION = 11;
    public static final int FEATURE_RAY_RECONSTRUCTION = 13;

    public static final int RESOURCE_VK_IMAGE_VIEW = 0;
    public static final int RESOURCE_VK_BUFFER = 1;

    public static final int IDENTIFIER_APPLICATION_ID = 0;
    public static final int IDENTIFIER_PROJECT_ID = 1;

    public static final int LOGGING_OFF = 0;
    public static final int LOGGING_ON = 1;
    public static final int LOGGING_VERBOSE = 2;
    public static final int LOGGING_NUM = 3;


    public static final int DLSS_FLAG_NONE = 0;
    public static final int DLSS_FLAG_HDR = 1 << 0;
    public static final int DLSS_FLAG_MV_LOW_RES = 1 << 1;
    public static final int DLSS_FLAG_MV_JITTERED = 1 << 2;
    public static final int DLSS_FLAG_DEPTH_INVERTED = 1 << 3;
    public static final int DLSS_FLAG_AUTO_EXPOSURE = 1 << 6;
    public static final int DLSS_FLAG_ALPHA_UPSCALING = 1 << 7;

    public static final int TONE_MAPPER_STRING = 0;
    public static final int TONE_MAPPER_REINHARD = 1;
    public static final int TONE_MAPPER_ONE_OVER_LUMA = 2;
    public static final int TONE_MAPPER_ACES = 3;

    public static final String CREATION_NODE_MASK = "CreationNodeMask";
    public static final String VISIBILITY_NODE_MASK = "VisibilityNodeMask";
    public static final String WIDTH = "Width";
    public static final String HEIGHT = "Height";
    public static final String OUT_WIDTH = "OutWidth";
    public static final String OUT_HEIGHT = "OutHeight";
    public static final String PERF_QUALITY_VALUE = "PerfQualityValue";
    public static final String COLOR = "Color";
    public static final String OUTPUT = "Output";
    public static final String DEPTH = "Depth";
    public static final String MOTION_VECTORS = "MotionVectors";
    public static final String SHARPNESS = "Sharpness";
    public static final String RESET = "Reset";
    public static final String JITTER_OFFSET_X = "Jitter.Offset.X";
    public static final String JITTER_OFFSET_Y = "Jitter.Offset.Y";
    public static final String MOTION_VECTOR_SCALE_X = "MV.Scale.X";
    public static final String MOTION_VECTOR_SCALE_Y = "MV.Scale.Y";
    public static final String TRANSPARENCY_MASK = "TransparencyMask";
    public static final String EXPOSURE_TEXTURE = "ExposureTexture";
    public static final String DLSS_CREATE_FLAGS = "DLSS.Feature.Create.Flags";
    public static final String DLSS_ENABLE_OUTPUT_SUBRECTS = "DLSS.Enable.Output.Subrects";
    public static final String DLSS_BIAS_CURRENT_COLOR_MASK = "DLSS.Input.Bias.Current.Color.Mask";
    public static final String DLSS_PRE_EXPOSURE = "DLSS.Pre.Exposure";
    public static final String DLSS_EXPOSURE_SCALE = "DLSS.Exposure.Scale";
    public static final String DLSS_INDICATOR_INVERT_X = "DLSS.Indicator.Invert.X.Axis";
    public static final String DLSS_INDICATOR_INVERT_Y = "DLSS.Indicator.Invert.Y.Axis";
    public static final String FRAME_TIME_DELTA_MS = "FrameTimeDeltaInMsec";

    public static final int DLSS_DENOISE_MODE_OFF = 0;
    public static final int DLSS_DENOISE_MODE_DL_UNIFIED = 1;
    public static final int DLSS_ROUGHNESS_MODE_UNPACKED = 0;
    public static final int DLSS_ROUGHNESS_MODE_PACKED = 1;
    public static final int DLSS_DEPTH_TYPE_LINEAR = 0;
    public static final int DLSS_DEPTH_TYPE_HARDWARE = 1;
    public static final int RAY_RECONSTRUCTION_PRESET_DEFAULT = 0;
    public static final int RAY_RECONSTRUCTION_PRESET_D = 4;
    public static final int RAY_RECONSTRUCTION_PRESET_E = 5;
    public static final int RAY_RECONSTRUCTION_PRESET_F = 6;

    public static final String DLSS_DENOISE_MODE = "DLSS.Denoise.Mode";
    public static final String DLSS_ROUGHNESS_MODE = "DLSS.Roughness.Mode";
    public static final String DLSS_USE_HARDWARE_DEPTH = "DLSS.Use.HW.Depth";
    public static final String DLSS_DIFFUSE_ALBEDO = "DLSS.Input.DiffuseAlbedo";
    public static final String DLSS_SPECULAR_ALBEDO = "DLSS.Input.SpecularAlbedo";
    public static final String DLSS_GBUFFER_SPECULAR_MVEC = "GBuffer.SpecularMvec";
    public static final String DLSSD_ALPHA = "DLSSD.Alpha";
    public static final String DLSSD_OUTPUT_ALPHA = "DLSSD.OutputAlpha";
    public static final String DLSSD_RESPONSIVITY_MASK = "DLSSD.ResponsivityMask";
    public static final String SUPER_SAMPLING_DENOISING_AVAILABLE = "SuperSamplingDenoising.Available";
    public static final String SUPER_SAMPLING_DENOISING_NEEDS_UPDATED_DRIVER =
            "SuperSamplingDenoising.NeedsUpdatedDriver";
    public static final String SUPER_SAMPLING_DENOISING_MIN_DRIVER_VERSION_MAJOR =
            "SuperSamplingDenoising.MinDriverVersionMajor";
    public static final String SUPER_SAMPLING_DENOISING_MIN_DRIVER_VERSION_MINOR =
            "SuperSamplingDenoising.MinDriverVersionMinor";
    public static final String SUPER_SAMPLING_DENOISING_FEATURE_INIT_RESULT =
            "SuperSamplingDenoising.FeatureInitResult";
    public static final String RAY_RECONSTRUCTION_PRESET_DLAA =
            "RayReconstruction.Hint.Render.Preset.DLAA";
    public static final String RAY_RECONSTRUCTION_PRESET_QUALITY =
            "RayReconstruction.Hint.Render.Preset.Quality";
    public static final String RAY_RECONSTRUCTION_PRESET_BALANCED =
            "RayReconstruction.Hint.Render.Preset.Balanced";
    public static final String RAY_RECONSTRUCTION_PRESET_PERFORMANCE =
            "RayReconstruction.Hint.Render.Preset.Performance";
    public static final String RAY_RECONSTRUCTION_PRESET_ULTRA_PERFORMANCE =
            "RayReconstruction.Hint.Render.Preset.UltraPerformance";
    public static final String RAY_RECONSTRUCTION_PRESET_ULTRA_QUALITY =
            "RayReconstruction.Hint.Render.Preset.UltraQuality";

    public static final String[] DLSS_GBUFFER = {
            "GBuffer.Albedo",
            "GBuffer.Roughness",
            "GBuffer.Metallic",
            "GBuffer.Specular",
            "GBuffer.Subsurface",
            "GBuffer.Normals",
            "GBuffer.ShadingModelId",
            "GBuffer.MaterialId",
            "GBuffer.Attrib.8",
            "GBuffer.Attrib.9",
            "GBuffer.Attrib.10",
            "GBuffer.Attrib.11",
            "GBuffer.Attrib.12",
            "GBuffer.Attrib.13",
            "GBuffer.Attrib.14",
            "GBuffer.Attrib.15"
    };

    public static final String DLSSFG_BACKBUFFER_FORMAT = "DLSSG.BackbufferFormat";
    public static final String DLSSFG_BACKBUFFER = "DLSSG.Backbuffer";
    public static final String DLSSFG_MVECS = "DLSSG.MVecs";
    public static final String DLSSFG_DEPTH = "DLSSG.Depth";
    public static final String DLSSFG_HUDLESS = "DLSSG.HUDLess";
    public static final String DLSSFG_UI = "DLSSG.UI";
    public static final String DLSSFG_UI_ALPHA = "DLSSG.UIAlpha";
    public static final String DLSSFG_BIDIRECTIONAL_DISTORTION_FIELD = "DLSSG.BidirectionalDistortionField";
    public static final String DLSSFG_OUTPUT_INTERPOLATED = "DLSSG.OutputInterpolated";
    public static final String DLSSFG_OUTPUT_REAL = "DLSSG.OutputReal";
    public static final String DLSSFG_OUTPUT_DISABLE_INTERPOLATION = "DLSSG.OutputDisableInterpolation";
    public static final String DLSSFG_MULTI_FRAME_COUNT = "DLSSG.MultiFrameCount";
    public static final String DLSSFG_MULTI_FRAME_INDEX = "DLSSG.MultiFrameIndex";
    public static final String DLSSFG_MULTI_FRAME_COUNT_MAX = "DLSSG.MultiFrameCountMax";
    public static final String DLSSFG_INTERNAL_WIDTH = "DLSSG.InternalWidth";
    public static final String DLSSFG_INTERNAL_HEIGHT = "DLSSG.InternalHeight";
    public static final String DLSSFG_DYNAMIC_RESOLUTION = "DLSSG.DynamicResolution";
    public static final String DLSSFG_MVEC_JITTERED = "DLSSG.MvecJittered";

    public static final String FRAME_GENERATION_AVAILABLE = "FrameGeneration.Available";
    public static final String FRAME_GENERATION_FEATURE_INIT_RESULT = "FrameGeneration.FeatureInitResult";
    public static final String FRAME_GENERATION_NEEDS_UPDATED_DRIVER = "FrameGeneration.NeedsUpdatedDriver";
    public static final String FRAME_GENERATION_MIN_DRIVER_VERSION_MAJOR = "FrameGeneration.MinDriverVersionMajor";
    public static final String FRAME_GENERATION_MIN_DRIVER_VERSION_MINOR = "FrameGeneration.MinDriverVersionMinor";

    private NgxConstants() {
    }

    public static boolean succeeded(int result) {
        return (result & 0xFFF00000) != RESULT_FAIL;
    }
}
