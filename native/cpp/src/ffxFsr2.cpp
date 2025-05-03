#define JAVA_FFX_RESOURCE "oiiaio/fsr/fsr2/impl/FfxResource"
#define JAVA_FFX_CONTEXT "oiiaio/fsr/fsr2/impl/FfxFsr2Context"
#include "oiiaio_fsr_jni.h"
#include "ffx-fsr2-api/ffx_fsr2.h"
#include "ffx-fsr2-api/gl/ffx_fsr2_gl.h"
//#include "ffx-fsr2-api/vk/ffx_fsr2_vk.h"
#include "ffx-fsr2-api/ffx_error.h"
#include <stdlib.h>
#include <memory>
#include <string>
#include "glfw3.h"
#include "utils.h"
#include "vulkan/vulkan.h"

static void up_env(JNIEnv *env)
{
    set_env(env);
}
static void check_env(JNIEnv *env)
{
    set_env(env);
}

JNIEXPORT jlongArray JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2CreateGL(JNIEnv *env, jobject, jint ___scratchMemorySize, jfloat fsr2Ratio, jint width, jint height, jint flags)
{
    check_env(env);
    unsigned int renderWidth = static_cast<unsigned int>(width / fsr2Ratio);
    unsigned int renderHeight = static_cast<unsigned int>(height / fsr2Ratio);
    FfxFsr2ContextDescription contextDesc = {};
    contextDesc.flags = flags;
    contextDesc.maxRenderSize = {renderWidth, renderHeight};
    contextDesc.displaySize = {static_cast<unsigned int>(width), static_cast<unsigned int>(height)};
    contextDesc.callbacks = {};
    contextDesc.fpMessage = [](FfxFsr2MsgType type, const wchar_t *message)
    {
        char cstr[256] = {};
#ifdef ON_LINUX
        wcstombs(cstr, message, sizeof(cstr));

#else
        wcstombs_s(nullptr, cstr, sizeof(cstr), message, sizeof(cstr));

#endif
        cstr[255] = '\0';
        java_log(cstr, 0);
    };
    size_t scratchMemorySize = ffxFsr2GetScratchMemorySizeGL();
    FfxFsr2Context *fsr2Context = new FfxFsr2Context();
    void *fsr2ScratchMemory = calloc(scratchMemorySize, 1);
    FfxErrorCode code1 = ffxFsr2GetInterfaceGL(&contextDesc.callbacks, fsr2ScratchMemory, scratchMemorySize, java_glfwGetProcAddress);
    FfxErrorCode code2 = ffxFsr2ContextCreate(fsr2Context, &contextDesc);
    java_log("FSR2_CPP ffxFsr2CreateGL", 0);
    jlong contextPtr = reinterpret_cast<jlong>(fsr2Context);
    java_log(("FSR2_CPP FfxFsr2Context->" + std::to_string(contextPtr)).c_str(), 0);
    jlong result[] = {code1, code2, contextPtr};
    jlongArray outJNIArray = (env)->NewLongArray(3);
    (env)->SetLongArrayRegion(outJNIArray, 0, 3, result);
    return outJNIArray;
}
JNIEXPORT jint JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2GetScratchMemorySizeGL(JNIEnv *env, jobject)
{
    check_env(env);
    return static_cast<int>(ffxFsr2GetScratchMemorySizeGL());
}

JNIEXPORT jint JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2ContextDispatch(
    JNIEnv *env,
    jobject,
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

JNIEXPORT jobject JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxGetTextureResourceGL(JNIEnv *env, jobject, jlong texGL, jint width, jint height, jint type)
{
    FfxResource resource = ffxGetTextureResourceGL(texGL, width, height, type);
    jclass javaffxrescls = env->FindClass(JAVA_FFX_RESOURCE);
    jmethodID constrocMID = env->GetMethodID(javaffxrescls, "<init>", "(IZJIIIIIIII)V");
    jobject javaffxres_ojb = env->NewObject(javaffxrescls, constrocMID, (jint)texGL, resource.isDepth, (jlong)resource.descriptorData, (jint)resource.description.type, (jint)resource.description.format, resource.description.width, resource.description.height, resource.description.depth, resource.description.mipCount, (jint)resource.description.flags, (jint)resource.state);
    return javaffxres_ojb;
};

JNIEXPORT jstring JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_getVersionInfo(JNIEnv *env, jobject)
{
    return (env)->NewStringUTF(SRLIB_VERSION);
}

JNIEXPORT jint JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2GetJitterPhaseCount(JNIEnv *, jobject, jint renderWidth, jint screenWidth)
{
    return static_cast<int>(ffxFsr2GetJitterPhaseCount(renderWidth, screenWidth));
}

JNIEXPORT jfloatArray JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2GetJitterOffset(JNIEnv *env, jobject, jint frameIndex, jint jitterPhaseCount)
{
    float jitterX = 0;
    float jitterY = 0;
    ffxFsr2GetJitterOffset(&jitterX, &jitterY, frameIndex, jitterPhaseCount);
    jfloat jitter_c[] = {jitterX, jitterY};
    jfloatArray outJNIArray = (env)->NewFloatArray(2);
    (env)->SetFloatArrayRegion(outJNIArray, 0, 2, jitter_c);
    return outJNIArray;
};

JNIEXPORT void JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2ContextDestroy(JNIEnv *, jobject, jlong ptr)
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

// ffxFsr2CreateVk(long device,long physicalDevice,int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
JNIEXPORT jintArray JNICALL Java_oiiaio_fsr_fsr2_FfxFSR2_ffxFsr2CreateVk(JNIEnv *env, jobject, jlong devicePtr, jlong physicalDevicePtr, jint scratchMemorySize, jfloat fsr2Ratio, jint width, jint height, jint flags)
{
    check_env(env);
    /*
    unsigned int renderWidth = static_cast<unsigned int>(width / fsr2Ratio);
    unsigned int renderHeight = static_cast<unsigned int>(height / fsr2Ratio);
    FfxFsr2ContextDescription contextDesc = {};
    contextDesc.flags = flags;
    contextDesc.maxRenderSize = {renderWidth, renderHeight};
    contextDesc.displaySize = {static_cast<unsigned int>(width), static_cast<unsigned int>(height)};
    contextDesc.callbacks = {};
    contextDesc.device = ffxGetDeviceVK(reinterpret_cast<VkDevice>(devicePtr));
    contextDesc.fpMessage = [](FfxFsr2MsgType type, const wchar_t *message)
    {
        char cstr[256] = {};
#ifdef ON_LINUX
        wcstombs(cstr, message, sizeof(cstr));

#else
        wcstombs_s(nullptr, cstr, sizeof(cstr), message, sizeof(cstr));

#endif
        cstr[255] = '\0';
        java_log(cstr, 0);
    };
    std::unique_ptr<char[]> fsr2ScratchMemory = std::make_unique<char[]>(scratchMemorySize);
    FfxErrorCode code1 = ffxFsr2GetInterfaceVK(
        &contextDesc.callbacks,
        fsr2ScratchMemory.get(),
        scratchMemorySize,
        reinterpret_cast<VkPhysicalDevice>(devicePtr),
        java_getDeviceProcAddr,
        java_VkGetPhysicalDeviceMemoryProperties,
        java_VkGetPhysicalDeviceProperties2,
        java_VkGetPhysicalDeviceFeatures2,
        java_VkEnumerateDeviceExtensionProperties,
        java_VkGetPhysicalDeviceProperties);
    FfxErrorCode code2 = ffxFsr2ContextCreate(nullptr, &contextDesc); /////////////////////////////*/
    java_log("FSR2_CPP ffxFsr2CreateVk", 0);
    jint code_c[] = {0, 0};
    jintArray outJNIArray = (env)->NewIntArray(2);
    (env)->SetIntArrayRegion(outJNIArray, 0, 2, code_c);
    return outJNIArray;
};