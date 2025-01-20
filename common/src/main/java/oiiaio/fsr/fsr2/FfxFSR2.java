package oiiaio.fsr.fsr2;

import oiiaio.fsr.fsr2.impl.FfxFsr2Context;
import oiiaio.fsr.fsr2.impl.FfxFsr2DispatchDescription;
import oiiaio.fsr.fsr2.impl.FfxResource;

public class FfxFSR2 {
    public FfxFSR2(String path){
        System.load(path);
    }
    private native void ffxFsr2ContextDestroy(long contextPtr);
    private native int ffxFsr2ContextDispatch(FfxResource color,
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
    private native long[] ffxFsr2CreateGL(int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    private native FfxResource ffxGetTextureResourceVk(long image,long imageView, int width, int height, int imgFormat);
    private native int[] ffxFsr2CreateVk(long device,long physicalDevice,int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    private native int ffxFsr2GetScratchMemorySizeVk(long physicalDevice);
    private native long ffxGetCommandListVK(long cmdBuf);

    public native String getVersionInfo();
    public native int ffxFsr2GetJitterPhaseCount(int renderWidth, int screenWidth);
    public native float[] ffxFsr2GetJitterOffset(int frameIndex,int jitterPhaseCount);
    public native FfxResource ffxGetTextureResourceGL(long textureGL, int width, int height, int imgFormat);
    public native int ffxFsr2GetScratchMemorySizeGL();

    public void ffxFsr2ContextDestroy(FfxFsr2Context context){
        ffxFsr2ContextDestroy(context.cppPointer);
    }

    public int ffxFsr2ContextDispatch(FfxFsr2DispatchDescription dispatchDescription,FfxFsr2Context context){
        return ffxFsr2ContextDispatch(
                dispatchDescription.color,
                dispatchDescription.depth,
                dispatchDescription.motionVectors,
                dispatchDescription.exposure,
                dispatchDescription.reactive,
                dispatchDescription.transparencyAndComposition,
                dispatchDescription.output,
                dispatchDescription.jitterOffset.getX(),
                dispatchDescription.jitterOffset.getY(),
                dispatchDescription.motionVectorScale.getX(),
                dispatchDescription.motionVectorScale.getY(),
                dispatchDescription.renderSize.getWidth(),
                dispatchDescription.renderSize.getHeight(),
                dispatchDescription.enableSharpening,
                dispatchDescription.sharpness,
                dispatchDescription.frameTimeDelta,
                dispatchDescription.preExposure,
                dispatchDescription.reset,
                dispatchDescription.cameraNear,
                dispatchDescription.cameraFar,
                dispatchDescription.cameraFovAngleVertical,
                dispatchDescription.viewSpaceToMetersFactor,
                dispatchDescription.deviceDepthNegativeOneToOne,
                context.cppPointer
        );
    }

    public FfxFsr2ContextCreateResult ffxFsr2CreateGL(float fsr2Ratio, int width, int height, int flags) {
        long[] srcResult = ffxFsr2CreateGL(ffxFsr2GetScratchMemorySizeGL(),fsr2Ratio,width,height,flags);
        FfxFsr2ContextCreateResult result = new FfxFsr2ContextCreateResult();
        result.ffxFsr2GetInterfaceErrorCode = (int) srcResult[0];
        result.ffxFsr2ContextCreateErrorCode = (int) srcResult[1];
        result.context = new FfxFsr2Context();
        result.context.cppPointer = srcResult[2];
        return result;
    }
}
