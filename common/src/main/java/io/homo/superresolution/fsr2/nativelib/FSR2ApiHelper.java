package io.homo.superresolution.fsr2.nativelib;

import io.homo.superresolution.fsr2.types.FfxResource;

public class FSR2ApiHelper {

    public FSR2ApiHelper(String path){
        System.load(path);
    }
    public native int ffxFsr2GetScratchMemorySizeGL();
    public native int ffxFsr2CreateGL(int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    public native int ffxFsr2Test();
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
