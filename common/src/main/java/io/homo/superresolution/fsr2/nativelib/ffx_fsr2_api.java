package io.homo.superresolution.fsr2.nativelib;

import io.homo.superresolution.fsr2.types.FfxResource;

public class ffx_fsr2_api {

    public ffx_fsr2_api(String path){
        System.load(path);
    }
    public native void init();
    public native int ffxFsr2GetScratchMemorySizeGL();
    public native int ffxFsr2CreateContext();
    public native int ffxFsr2GetInterfaceGL(int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    public native int ffxFsr2Test();
    public native int ffxFsr2ContextDestroy();
    public native String getGPUInfoNV();
    public native String getGPUInfoAMD();
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
}
