package io.homo.superresolution.core;

import io.homo.superresolution.core.glslang.GlslangCompileShaderResult;
import oiiaio.fsr.fsr2.impl.FfxResource;

public class SuperResolutionNative {
    public static native void ffxFsr2ContextDestroy(long contextPtr);

    public static native int ffxFsr2ContextDispatch(FfxResource color,
                                                    FfxResource depth,
                                                    FfxResource motionVectors,
                                                    FfxResource exposure,
                                                    FfxResource reactive,
                                                    FfxResource transparencyAndComposition,
                                                    FfxResource output,
                                                    float jitterOffsetX,
                                                    float jitterOffsetY,
                                                    float motionVectorScaleWidth,
                                                    float motionVectorScaleHeight,
                                                    int renderSizeWidth,
                                                    int renderSizeHeight,
                                                    boolean enableSharpening,
                                                    float sharpness,
                                                    float frameTimeDelta,
                                                    float preExposure,
                                                    boolean reset,
                                                    float cameraNear,
                                                    float cameraFar,
                                                    float cameraFovAngleVertical,
                                                    float viewSpaceToMetersFactor,
                                                    boolean deviceDepthNegativeOneToOne,
                                                    long contextPtr
    );

    public static native long[] ffxFsr2CreateGL(int scratchMemorySize, float fsr2Ratio, int width, int height, int flags);

    public static native FfxResource ffxGetTextureResourceVk(long image, long imageView, int width, int height, int imgFormat);

    public static native int[] ffxFsr2CreateVk(long device, long physicalDevice, int scratchMemorySize, float fsr2Ratio, int width, int height, int flags);

    public static native int ffxFsr2GetScratchMemorySizeVk(long physicalDevice);

    public static native long ffxGetCommandListVK(long cmdBuf);

    public static native String getVersionInfo();

    public static native int ffxFsr2GetJitterPhaseCount(int renderWidth, int screenWidth);

    public static native float[] ffxFsr2GetJitterOffset(int frameIndex, int jitterPhaseCount);

    public static native FfxResource ffxGetTextureResourceGL(long textureGL, int width, int height, int imgFormat);

    public static native int ffxFsr2GetScratchMemorySizeGL();
    
    public static native GlslangCompileShaderResult compileShaderToSpirv(
            String shaderSrc,
            String outputFile,
            int stage,
            int language,
            int client,
            int client_version,
            int target_language,
            int target_language_version,
            int default_version,
            int default_profile,
            boolean force_default_version_and_profile,
            boolean forward_compatible
    );

    public static native int initGlslang();

    public static native int destroyGlslang();
}
