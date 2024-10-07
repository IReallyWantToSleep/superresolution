package io.homo.superresolution.upscale;

import io.homo.superresolution.upscale.fsr2.types.FfxResource;

public class NativeApiHelper {

    public NativeApiHelper(String path){
        System.load(path);
    }
    public native int ffxFsr2GetScratchMemorySizeGL();
    public native int[] ffxFsr2CreateGL(int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    public native int ffxFsr2Test();
    public native String getVersionInfo();
    public native int ffxFsr2ContextDispatch(FfxResource color,
                                             FfxResource depth,
                                             FfxResource motionVectors,
                                             FfxResource exposure,
                                             FfxResource reactive,
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
                                             int width,
                                             int height
    );
    public native FfxResource ffxGetTextureResourceGL(long textureGL, int width, int height, int imgFormat);
    public native int ffxFsr2GetJitterPhaseCount(int renderWidth, int screenWidth);
    public native float[] ffxFsr2GetJitterOffset(int frameIndex,int jitterPhaseCount);
}
