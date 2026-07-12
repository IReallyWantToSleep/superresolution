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

    public static final int RESOURCE_VK_IMAGE_VIEW = 0;
    public static final int RESOURCE_VK_BUFFER = 1;

    public static final int IDENTIFIER_APPLICATION_ID = 0;
    public static final int IDENTIFIER_PROJECT_ID = 1;

    public static final int LOGGING_OFF = 0;
    public static final int LOGGING_ON = 1;
    public static final int LOGGING_VERBOSE = 2;

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

    private NgxConstants() {
    }

    public static boolean succeeded(int result) {
        return (result & 0xFFF00000) != RESULT_FAIL;
    }
}
