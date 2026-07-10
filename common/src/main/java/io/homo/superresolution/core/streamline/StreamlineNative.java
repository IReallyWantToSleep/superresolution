package io.homo.superresolution.core.streamline;

public final class StreamlineNative {
    private StreamlineNative() {
    }

    static native int nOpen(
            boolean showConsole,
            int logLevel,
            long preferenceFlags,
            String[] pluginPaths,
            String logPath,
            int[] features,
            int applicationId,
            int engine,
            String engineVersion,
            String projectId,
            StreamlineLogListener logListener,
            long[] outHandle
    );

    static native int nClose(long session);

    public static native int nInit(String pluginPath, String logPath);

    public static native int nShutdown();

    public static native long nCreateVkInstance(long createInfoAddress);

    public static native long nCreateVkDevice(long instanceAddress, long physicalDeviceAddress, long createInfoAddress);

    public static native int nGetLastVkResult();

    public static native boolean nIsDLSSGSupported();

    public static native int nDLSSGSetOptions(boolean enabled, int framesToGenerate);

    public static native int nDLSSGGetState(StreamlineDLSSGState outState);

    static native int nSetVulkanInfo(
            long session,
            long device,
            long instance,
            long physicalDevice,
            int computeQueueIndex,
            int computeQueueFamily,
            int graphicsQueueIndex,
            int graphicsQueueFamily,
            int opticalFlowQueueIndex,
            int opticalFlowQueueFamily,
            boolean useNativeOpticalFlowMode,
            int computeQueueCreateFlags,
            int graphicsQueueCreateFlags,
            int opticalFlowQueueCreateFlags
    );

    static native int nIsFeatureSupported(long session, int feature, long physicalDevice, boolean[] outSupported);

    static native int nIsFeatureLoaded(long session, int feature, boolean[] outLoaded);

    static native int nSetFeatureLoaded(long session, int feature, boolean loaded);

    static native int nGetFeatureRequirements(long session, int feature, StreamlineTypes.FeatureRequirements outRequirements);

    static native int nGetFeatureVersion(long session, int feature, StreamlineTypes.FeatureVersion outVersion);

    static native int nGetNewFrameToken(long session, boolean hasFrameIndex, int frameIndex, StreamlineTypes.FrameToken outToken);

    static native int nSetTag(long session, int viewport, StreamlineTypes.ResourceTag[] tags, long commandBuffer);

    static native int nSetTagForFrame(
            long session,
            long frameToken,
            int viewport,
            StreamlineTypes.ResourceTag[] tags,
            long commandBuffer
    );

    static native int nSetConstants(long session, StreamlineTypes.Constants constants, long frameToken, int viewport);

    static native int nAllocateResources(long session, long commandBuffer, int feature, int viewport);

    static native int nFreeResources(long session, int feature, int viewport);

    static native int nEvaluateFeature(
            long session,
            int feature,
            long frameToken,
            StreamlineTypes.EvaluateInput[] inputs,
            long commandBuffer
    );

    static native int nGetFeatureFunction(long session, int feature, String name, long[] outAddress);

    static native int nDlssGetOptimalSettings(
            long session,
            StreamlineTypes.DlssOptions options,
            StreamlineTypes.DlssOptimalSettings outSettings
    );

    static native int nDlssGetState(long session, int viewport, StreamlineTypes.DlssState outState);

    static native int nDlssSetOptions(long session, int viewport, StreamlineTypes.DlssOptions options);

    static native int nDlssGGetState(
            long session,
            int viewport,
            StreamlineTypes.DlssGState outState,
            StreamlineTypes.DlssGOptions options
    );

    static native int nDlssGSetOptions(long session, int viewport, StreamlineTypes.DlssGOptions options);

    static native int nPclGetState(long session, StreamlineTypes.PclState outState);

    static native int nPclSetMarker(long session, int marker, long frameToken);

    static native int nPclSetOptions(long session, StreamlineTypes.PclOptions options);

    static native int nReflexGetState(long session, StreamlineTypes.ReflexState outState);

    static native int nReflexSleep(long session, long frameToken);

    static native int nReflexSetOptions(long session, StreamlineTypes.ReflexOptions options);

    static native int nReflexSetCameraData(
            long session,
            int viewport,
            long frameToken,
            StreamlineTypes.ReflexCameraData data
    );

    static native int nReflexGetPredictedCameraData(
            long session,
            int viewport,
            long frameToken,
            StreamlineTypes.ReflexPredictedCameraData outData
    );
}
