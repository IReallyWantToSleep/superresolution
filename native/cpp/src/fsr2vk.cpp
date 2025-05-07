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

// ffxFsr2CreateVk(long device,long physicalDevice,int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
JNIEXPORT jintArray JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_ffxFsr2CreateVk(JNIEnv *env, jclass, jlong devicePtr, jlong physicalDevicePtr, jint scratchMemorySize, jfloat fsr2Ratio, jint width, jint height, jint flags)
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
