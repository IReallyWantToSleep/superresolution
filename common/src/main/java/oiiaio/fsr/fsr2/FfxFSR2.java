package oiiaio.fsr.fsr2;

import io.homo.superresolution.core.SuperResolutionNative;
import oiiaio.fsr.fsr2.impl.FfxFsr2Context;
import oiiaio.fsr.fsr2.impl.FfxFsr2DispatchDescription;
import oiiaio.fsr.fsr2.impl.FfxResource;

public class FfxFSR2 {
    public static void ffxFsr2ContextDestroy(FfxFsr2Context context) {
        SuperResolutionNative.ffxFsr2ContextDestroy(context.cppPointer);
    }

    public static FfxResource ffxGetTextureResourceGL(long textureGL, int width, int height, int imgFormat) {
        return SuperResolutionNative.ffxGetTextureResourceGL(textureGL, width, height, imgFormat);
    }

    public static int ffxFsr2ContextDispatch(FfxFsr2DispatchDescription dispatchDescription, FfxFsr2Context context) {
        return SuperResolutionNative.ffxFsr2ContextDispatch(
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

    public static FfxFsr2ContextCreateResult ffxFsr2CreateGL(float fsr2Ratio, int width, int height, int flags) {
        long[] srcResult = SuperResolutionNative.ffxFsr2CreateGL(
                SuperResolutionNative.ffxFsr2GetScratchMemorySizeGL(),
                fsr2Ratio,
                width,
                height,
                flags
        );
        FfxFsr2ContextCreateResult result = new FfxFsr2ContextCreateResult();
        result.ffxFsr2GetInterfaceErrorCode = (int) srcResult[0];
        result.ffxFsr2ContextCreateErrorCode = (int) srcResult[1];
        result.context = new FfxFsr2Context();
        result.context.cppPointer = srcResult[2];
        return result;
    }
}
