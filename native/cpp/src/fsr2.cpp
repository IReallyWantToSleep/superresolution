#include "jni_header.h"
#include "ffx-fsr2-api/ffx_fsr2.h"
#include "ffx-fsr2-api/gl/ffx_fsr2_gl.h"
#include "ffx-fsr2-api/ffx_error.h"
#include <stdlib.h>
#include <memory>
#include <string>
#include "glfw3.h"
#include "utils.h"
#include "vulkan/vulkan.h"
JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2ContextDispatch(
    JNIEnv *env,
    jclass,
    jobject color,
    jobject depth,
    jobject motionVectors,
    jobject exposure,
    jobject reactive,
    jobject transparencyAndComposition,
    jobject output,
    jfloat jitterX,
    jfloat jitterY,
    jfloat motionVectorScaleWidth,
    jfloat motionVectorScaleHeight,
    jint renderSizeWidth,
    jint renderSizeHeight,
    jboolean enableSharpening,
    jfloat sharpness,
    jfloat frameTimeDelta,
    jfloat preExposure,
    jboolean reset,
    jfloat cameraNear,
    jfloat cameraFar,
    jfloat cameraFovY,
    jfloat viewSpaceToMetersFactor,
    jboolean deviceDepthNegativeOneToOne,
    jlong contextPtr)
{
    check_env(env);
    FfxFsr2DispatchDescription dispatchDesc = {};
    dispatchDesc.color = ffxResourceJavaToCpp(env, color);
    dispatchDesc.depth = ffxResourceJavaToCpp(env, depth);
    dispatchDesc.motionVectors = ffxResourceJavaToCpp(env, motionVectors);
    dispatchDesc.exposure = ffxResourceJavaToCpp(env, exposure);
    dispatchDesc.reactive = ffxResourceJavaToCpp(env, reactive);
    dispatchDesc.transparencyAndComposition = ffxResourceJavaToCpp(env, transparencyAndComposition);
    dispatchDesc.output = ffxResourceJavaToCpp(env, output);
    dispatchDesc.jitterOffset = {jitterX, jitterY};
    dispatchDesc.motionVectorScale = {float(static_cast<int>(motionVectorScaleWidth)), float(static_cast<int>(motionVectorScaleHeight))};
    dispatchDesc.renderSize = {static_cast<uint32_t>(renderSizeWidth), static_cast<uint32_t>(renderSizeHeight)};
    dispatchDesc.enableSharpening = ToCppBool(enableSharpening);
    dispatchDesc.sharpness = sharpness;
    dispatchDesc.frameTimeDelta = static_cast<float>(frameTimeDelta); // ms
    dispatchDesc.preExposure = preExposure;
    dispatchDesc.reset = ToCppBool(reset);
    dispatchDesc.cameraNear = cameraNear;
    dispatchDesc.cameraFar = cameraFar;
    dispatchDesc.cameraFovAngleVertical = cameraFovY;
    dispatchDesc.viewSpaceToMetersFactor = viewSpaceToMetersFactor;
    dispatchDesc.deviceDepthNegativeOneToOne = ToCppBool(deviceDepthNegativeOneToOne);
    FfxFsr2Context *context = reinterpret_cast<FfxFsr2Context *>(contextPtr);
    FfxErrorCode err = ffxFsr2ContextDispatch(context, &dispatchDesc);
    return static_cast<int>(err);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2GetJitterPhaseCount(JNIEnv *, jclass, jint renderWidth, jint screenWidth)
{
    return static_cast<int>(ffxFsr2GetJitterPhaseCount(renderWidth, screenWidth));
}

JNIEXPORT jfloatArray JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2GetJitterOffset(JNIEnv *env, jclass, jint frameIndex, jint jitterPhaseCount)
{
    float jitterX = 0;
    float jitterY = 0;
    ffxFsr2GetJitterOffset(&jitterX, &jitterY, frameIndex, jitterPhaseCount);
    jfloat jitter_c[] = {jitterX, jitterY};
    jfloatArray outJNIArray = (env)->NewFloatArray(2);
    (env)->SetFloatArrayRegion(outJNIArray, 0, 2, jitter_c);
    return outJNIArray;
};

JNIEXPORT void JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2ContextDestroy(JNIEnv *, jclass, jlong ptr)
{
    FfxFsr2Context *cPtr = reinterpret_cast<FfxFsr2Context *>(ptr);
    if (cPtr)
    {
        try
        {
            ffxFsr2ContextDestroy(cPtr);
            free(cPtr);
        }
        catch (...)
        {
            java_log("FSR2_CPP ffxFsr2ContextDestroy ERROR", 2);
            return;
        }
        java_log("FSR2_CPP ffxFsr2ContextDestroy", 0);
    }
};
