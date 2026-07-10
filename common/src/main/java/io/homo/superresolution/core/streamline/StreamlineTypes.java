package io.homo.superresolution.core.streamline;

import java.util.Arrays;

public final class StreamlineTypes {
    private StreamlineTypes() {
    }

    public static final class LogLevel {
        public static final int OFF = 0;
        public static final int DEFAULT = 1;
        public static final int VERBOSE = 2;

        private LogLevel() {
        }
    }

    public static final class LogType {
        public static final int INFO = 0;
        public static final int WARNING = 1;
        public static final int ERROR = 2;

        private LogType() {
        }
    }

    public static final class EngineType {
        public static final int CUSTOM = 0;
        public static final int UNREAL = 1;
        public static final int UNITY = 2;

        private EngineType() {
        }
    }

    public static final class PreferenceFlags {
        public static final long DISABLE_COMMAND_LIST_STATE_TRACKING = 1L;
        public static final long DISABLE_DEBUG_TEXT = 1L << 1;
        public static final long USE_MANUAL_HOOKING = 1L << 2;
        public static final long ALLOW_OTA = 1L << 3;
        public static final long BYPASS_OS_VERSION_CHECK = 1L << 4;
        public static final long USE_DXGI_FACTORY_PROXY = 1L << 5;
        public static final long LOAD_DOWNLOADED_PLUGINS = 1L << 6;
        public static final long USE_FRAME_BASED_RESOURCE_TAGGING = 1L << 7;

        private PreferenceFlags() {
        }
    }

    public static final class ResourceType {
        public static final int TEX_2D = 0;
        public static final int BUFFER = 1;
        public static final int COMMAND_QUEUE = 2;
        public static final int COMMAND_BUFFER = 3;
        public static final int COMMAND_POOL = 4;
        public static final int FENCE = 5;
        public static final int SWAPCHAIN = 6;
        public static final int HOST_FENCE = 7;
        public static final int UNKNOWN = 8;

        private ResourceType() {
        }
    }

    public static final class ResourceLifecycle {
        public static final int ONLY_VALID_NOW = 0;
        public static final int VALID_UNTIL_PRESENT = 1;
        public static final int VALID_UNTIL_EVALUATE = 2;

        private ResourceLifecycle() {
        }
    }

    public static final class DlssMode {
        public static final int OFF = 0;
        public static final int MAX_PERFORMANCE = 1;
        public static final int BALANCED = 2;
        public static final int MAX_QUALITY = 3;
        public static final int ULTRA_PERFORMANCE = 4;
        public static final int ULTRA_QUALITY = 5;
        public static final int DLAA = 6;

        private DlssMode() {
        }
    }

    public static final class DlssGMode {
        public static final int OFF = 0;
        public static final int ON = 1;
        public static final int AUTO = 2;
        public static final int DYNAMIC = 3;

        private DlssGMode() {
        }
    }

    public static final class DlssGQueueParallelismMode {
        public static final int BLOCK_PRESENTING_CLIENT_QUEUE = 0;
        public static final int BLOCK_NO_CLIENT_QUEUES = 1;

        private DlssGQueueParallelismMode() {
        }
    }

    public static final class DlssPreset {
        public static final int DEFAULT = 0;
        public static final int PRESET_E = 5;
        public static final int PRESET_F = 6;
        public static final int PRESET_G = 7;
        public static final int PRESET_H = 8;
        public static final int PRESET_I = 9;
        public static final int PRESET_J = 10;
        public static final int PRESET_K = 11;
        public static final int PRESET_L = 12;
        public static final int PRESET_M = 13;
        public static final int PRESET_N = 14;
        public static final int PRESET_O = 15;

        private DlssPreset() {
        }
    }

    public static final class DlssGFlags {
        public static final int SHOW_ONLY_INTERPOLATED_FRAME = 1;
        public static final int DYNAMIC_RESOLUTION_ENABLED = 1 << 1;
        public static final int REQUEST_VRAM_ESTIMATE = 1 << 2;
        public static final int RETAIN_RESOURCES_WHEN_OFF = 1 << 3;
        public static final int ENABLE_FULLSCREEN_MENU_DETECTION = 1 << 4;

        private DlssGFlags() {
        }
    }

    public static final class PclHotKey {
        public static final int USE_PING_MESSAGE = 0;
        public static final int VK_F13 = 0x7C;
        public static final int VK_F14 = 0x7D;
        public static final int VK_F15 = 0x7E;

        private PclHotKey() {
        }
    }

    public static final class PclMarker {
        public static final int SIMULATION_START = 0;
        public static final int SIMULATION_END = 1;
        public static final int RENDER_SUBMIT_START = 2;
        public static final int RENDER_SUBMIT_END = 3;
        public static final int PRESENT_START = 4;
        public static final int PRESENT_END = 5;
        public static final int TRIGGER_FLASH = 7;
        public static final int LATENCY_PING = 8;
        public static final int OUT_OF_BAND_RENDER_SUBMIT_START = 9;
        public static final int OUT_OF_BAND_RENDER_SUBMIT_END = 10;
        public static final int OUT_OF_BAND_PRESENT_START = 11;
        public static final int OUT_OF_BAND_PRESENT_END = 12;
        public static final int CONTROLLER_INPUT_SAMPLE = 13;
        public static final int DELTA_T_CALCULATION = 14;
        public static final int LATE_WARP_PRESENT_START = 15;
        public static final int LATE_WARP_PRESENT_END = 16;
        public static final int CAMERA_CONSTRUCTED = 17;
        public static final int LATE_WARP_RENDER_SUBMIT_START = 18;
        public static final int LATE_WARP_RENDER_SUBMIT_END = 19;
        public static final int VENDOR_INTERNAL_ASYNC_PRESENT_START = 20;
        public static final int VENDOR_INTERNAL_ASYNC_PRESENT_END = 21;
        public static final int NUM_PRESENTS_IN_BATCH = 22;

        private PclMarker() {
        }
    }

    public static final class ReflexMode {
        public static final int OFF = 0;
        public static final int LOW_LATENCY = 1;
        public static final int LOW_LATENCY_WITH_BOOST = 2;

        private ReflexMode() {
        }
    }

    public static final class EvaluateInputKind {
        public static final int VIEWPORT = 0;
        public static final int RESOURCE_TAG = 1;
        public static final int CONSTANTS = 2;

        private EvaluateInputKind() {
        }
    }

    public static final class Version {
        public int major;
        public int minor;
        public int build;
    }

    public static final class VulkanInfo {
        public long device;
        public long instance;
        public long physicalDevice;
        public int computeQueueIndex;
        public int computeQueueFamily;
        public int graphicsQueueIndex;
        public int graphicsQueueFamily;
        public int opticalFlowQueueIndex;
        public int opticalFlowQueueFamily;
        public boolean useNativeOpticalFlowMode;
        public int computeQueueCreateFlags;
        public int graphicsQueueCreateFlags;
        public int opticalFlowQueueCreateFlags;
    }

    public static final class Viewport {
        public int value;

        public Viewport() {
        }

        public Viewport(int value) {
            this.value = value;
        }
    }

    public static final class FrameToken {
        public long nativeHandle;
        public int frameIndex;
    }

    public static final class Extent {
        public int top;
        public int left;
        public int width;
        public int height;
    }

    public static final class Resource {
        public int type = ResourceType.TEX_2D;
        public long nativeHandle;
        public long memory;
        public long view;
        public int state = -1;
        public int width;
        public int height;
        public int nativeFormat;
        public int mipLevels;
        public int arrayLayers;
        public long gpuVirtualAddress;
        public int flags;
        public int usage;
        public int reserved;
    }

    public static final class ResourceTag {
        public Resource resource;
        public int type;
        public int lifecycle = ResourceLifecycle.ONLY_VALID_NOW;
        public Extent extent;
    }

    public static final class Constants {
        public float[] cameraViewToClip = identityMatrix();
        public float[] clipToCameraView = identityMatrix();
        public float[] clipToLensClip = identityMatrix();
        public float[] clipToPrevClip = identityMatrix();
        public float[] prevClipToClip = identityMatrix();
        public float jitterOffsetX;
        public float jitterOffsetY;
        public float motionVectorScaleX;
        public float motionVectorScaleY;
        public float cameraPinholeOffsetX;
        public float cameraPinholeOffsetY;
        public float cameraPosX;
        public float cameraPosY;
        public float cameraPosZ;
        public float cameraUpX;
        public float cameraUpY;
        public float cameraUpZ;
        public float cameraRightX;
        public float cameraRightY;
        public float cameraRightZ;
        public float cameraFwdX;
        public float cameraFwdY;
        public float cameraFwdZ;
        public float cameraNear;
        public float cameraFar;
        public float cameraFov;
        public float cameraAspectRatio;
        public float motionVectorsInvalidValue;
        public byte depthInverted = 2;
        public byte cameraMotionIncluded = 2;
        public byte motionVectors3D = 2;
        public byte reset = 2;
        public byte orthographicProjection;
        public byte motionVectorsDilated;
        public byte motionVectorsJittered;
        public float minRelativeLinearDepthObjectSeparation = 40.0f;
    }

    public static final class EvaluateInput {
        public int kind;
        public Viewport viewport;
        public ResourceTag resourceTag;
        public Constants constants;

        public static EvaluateInput viewport(Viewport value) {
            EvaluateInput input = new EvaluateInput();
            input.kind = EvaluateInputKind.VIEWPORT;
            input.viewport = value;
            return input;
        }

        public static EvaluateInput resourceTag(ResourceTag value) {
            EvaluateInput input = new EvaluateInput();
            input.kind = EvaluateInputKind.RESOURCE_TAG;
            input.resourceTag = value;
            return input;
        }

        public static EvaluateInput constants(Constants value) {
            EvaluateInput input = new EvaluateInput();
            input.kind = EvaluateInputKind.CONSTANTS;
            input.constants = value;
            return input;
        }
    }

    public static final class FeatureRequirements {
        public int flags;
        public int maxNumCpuThreads;
        public int maxNumViewports;
        public int[] requiredTags = new int[0];
        public Version osVersionDetected = new Version();
        public Version osVersionRequired = new Version();
        public Version driverVersionDetected = new Version();
        public Version driverVersionRequired = new Version();
        public int vkNumComputeQueuesRequired;
        public int vkNumGraphicsQueuesRequired;
        public String[] vkDeviceExtensions = new String[0];
        public String[] vkInstanceExtensions = new String[0];
        public String[] vkFeatures12 = new String[0];
        public String[] vkFeatures13 = new String[0];
        public int vkNumOpticalFlowQueuesRequired;
    }

    public static final class FeatureVersion {
        public Version versionSl = new Version();
        public Version versionNgx = new Version();
    }

    public static final class DlssOptions {
        public int mode = DlssMode.OFF;
        public int outputWidth = -1;
        public int outputHeight = -1;
        public float sharpness;
        public float preExposure = 1.0f;
        public float exposureScale = 1.0f;
        public byte colorBuffersHdr = 1;
        public byte indicatorInvertAxisX;
        public byte indicatorInvertAxisY;
        public int dlaaPreset;
        public int qualityPreset;
        public int balancedPreset;
        public int performancePreset;
        public int ultraPerformancePreset;
        public int ultraQualityPreset;
        public byte useAutoExposure;
        public byte alphaUpscalingEnabled;
    }

    public static final class DlssOptimalSettings {
        public int optimalRenderWidth;
        public int optimalRenderHeight;
        public float optimalSharpness;
        public int renderWidthMin;
        public int renderHeightMin;
        public int renderWidthMax;
        public int renderHeightMax;
    }

    public static final class DlssState {
        public long estimatedVramUsage;
    }

    public static final class DlssGOptions {
        public int mode = DlssGMode.OFF;
        public int numFramesToGenerate = 1;
        public int flags;
        public int dynamicResWidth;
        public int dynamicResHeight;
        public int numBackBuffers;
        public int motionVectorDepthWidth;
        public int motionVectorDepthHeight;
        public int colorWidth;
        public int colorHeight;
        public int colorBufferFormat;
        public int motionVectorBufferFormat;
        public int depthBufferFormat;
        public int hudLessBufferFormat;
        public int uiBufferFormat;
        public byte reserved15 = 2;
        public int queueParallelismMode;
        public byte enableUserInterfaceRecomposition;
        public float dynamicTargetFrameRate;
        public StreamlineApiErrorListener onApiError;
    }

    public static final class DlssGState {
        public long estimatedVramUsage;
        public int status;
        public int minWidthOrHeight;
        public int numFramesActuallyPresented;
        public int numFramesToGenerateMax;
        public byte reserved4;
        public byte vsyncSupportAvailable;
        public long inputsProcessingCompletionFence;
        public long lastPresentInputsProcessingCompletionFenceValue;
        public byte dynamicMfgSupported;
    }

    public static final class PclOptions {
        public int virtualKey;
        public int threadId;
    }

    public static final class PclState {
        public int statsWindowMessage;
    }

    public static final class ReflexOptions {
        public int mode = ReflexMode.OFF;
        public int frameLimitUs;
        public boolean useMarkersToOptimize;
        public int virtualKey;
        public int threadId;
    }

    public static final class ReflexCameraData {
        public float[] worldToViewMatrix = identityMatrix();
        public float[] viewToClipMatrix = identityMatrix();
        public float[] previousRenderedWorldToViewMatrix = identityMatrix();
        public float[] previousRenderedViewToClipMatrix = identityMatrix();
    }

    public static final class ReflexPredictedCameraData {
        public float[] predictedWorldToViewMatrix = identityMatrix();
        public float[] predictedViewToClipMatrix = identityMatrix();
    }

    public static final class ReflexReport {
        public long frameId;
        public long inputSampleTime;
        public long simulationStartTime;
        public long simulationEndTime;
        public long renderSubmitStartTime;
        public long renderSubmitEndTime;
        public long presentStartTime;
        public long presentEndTime;
        public long driverStartTime;
        public long driverEndTime;
        public long osRenderQueueStartTime;
        public long osRenderQueueEndTime;
        public long gpuRenderStartTime;
        public long gpuRenderEndTime;
        public int gpuActiveRenderTimeUs;
        public int gpuFrameTimeUs;
    }

    public static final class ReflexReport2 {
        public long cameraConstructedTime;
        public int crossAdapterCopyTimeUs;
    }

    public static final class ReflexState {
        public static final int REPORT_COUNT = 64;
        public boolean lowLatencyAvailable;
        public boolean latencyReportAvailable;
        public int statsWindowMessage;
        public boolean flashIndicatorDriverControlled;
        public long[] frameReports = new long[REPORT_COUNT * 16];
        public long[] frameReports2 = new long[REPORT_COUNT * 2];

        public ReflexReport report(int index) {
            if (index < 0 || index >= REPORT_COUNT) {
                throw new IndexOutOfBoundsException(index);
            }
            int base = index * 16;
            ReflexReport result = new ReflexReport();
            result.frameId = frameReports[base];
            result.inputSampleTime = frameReports[base + 1];
            result.simulationStartTime = frameReports[base + 2];
            result.simulationEndTime = frameReports[base + 3];
            result.renderSubmitStartTime = frameReports[base + 4];
            result.renderSubmitEndTime = frameReports[base + 5];
            result.presentStartTime = frameReports[base + 6];
            result.presentEndTime = frameReports[base + 7];
            result.driverStartTime = frameReports[base + 8];
            result.driverEndTime = frameReports[base + 9];
            result.osRenderQueueStartTime = frameReports[base + 10];
            result.osRenderQueueEndTime = frameReports[base + 11];
            result.gpuRenderStartTime = frameReports[base + 12];
            result.gpuRenderEndTime = frameReports[base + 13];
            result.gpuActiveRenderTimeUs = (int) frameReports[base + 14];
            result.gpuFrameTimeUs = (int) frameReports[base + 15];
            return result;
        }

        public ReflexReport2 report2(int index) {
            if (index < 0 || index >= REPORT_COUNT) {
                throw new IndexOutOfBoundsException(index);
            }
            int base = index * 2;
            ReflexReport2 result = new ReflexReport2();
            result.cameraConstructedTime = frameReports2[base];
            result.crossAdapterCopyTimeUs = (int) frameReports2[base + 1];
            return result;
        }
    }

    static float[] identityMatrix() {
        return new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        };
    }

    static void requireMatrix(float[] value, String name) {
        if (value == null || value.length != 16) {
            throw new IllegalArgumentException(name + " must contain 16 floats");
        }
    }

    static void requireResourceTag(ResourceTag value) {
        if (value == null || value.resource == null) {
            throw new IllegalArgumentException("ResourceTag.resource is required");
        }
    }

    static void requireConstants(Constants value) {
        if (value == null) {
            throw new IllegalArgumentException("Constants are required");
        }
        requireMatrix(value.cameraViewToClip, "cameraViewToClip");
        requireMatrix(value.clipToCameraView, "clipToCameraView");
        requireMatrix(value.clipToLensClip, "clipToLensClip");
        requireMatrix(value.clipToPrevClip, "clipToPrevClip");
        requireMatrix(value.prevClipToClip, "prevClipToClip");
    }
}
