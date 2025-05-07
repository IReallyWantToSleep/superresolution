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
#include "define.h"

JNIEXPORT jlongArray JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2CreateGL(JNIEnv *env, jclass, jint ___scratchMemorySize, jfloat fsr2Ratio, jint width, jint height, jint flags)
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
JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2GetScratchMemorySizeGL(JNIEnv *env, jclass)
{
    check_env(env);
    return static_cast<int>(ffxFsr2GetScratchMemorySizeGL());
}

JNIEXPORT jobject JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxGetTextureResourceGL(JNIEnv *env, jclass, jlong texGL, jint width, jint height, jint type)
{
    FfxResource resource = ffxGetTextureResourceGL(texGL, width, height, type);
    jclass javaffxrescls = env->FindClass(JAVA_FFX_RESOURCE);
    jmethodID constrocMID = env->GetMethodID(javaffxrescls, "<init>", "(IZJIIIIIIII)V");
    jobject javaffxres_ojb = env->NewObject(javaffxrescls, constrocMID, (jint)texGL, resource.isDepth, (jlong)resource.descriptorData, (jint)resource.description.type, (jint)resource.description.format, resource.description.width, resource.description.height, resource.description.depth, resource.description.mipCount, (jint)resource.description.flags, (jint)resource.state);
    return javaffxres_ojb;
};
