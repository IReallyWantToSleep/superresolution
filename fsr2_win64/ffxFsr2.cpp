#include "pch.h"
#include "io_homo_superresolution_fsr2_nativelib_ffx_fsr2_api.h"
#include "fsr2/gl/ffx_fsr2_gl.h"
#include "fsr2/ffx_fsr2.h"
#include "fsr2/ffx_error.h"
#include <stdlib.h>
#include <memory>
#include "glfw3.h"
#include "utils.h"
#include <windows.h>

bool fsr2FirstInit = true;
FfxFsr2Context fsr2Context;
std::unique_ptr<char[]> fsr2ScratchMemory;
FfxFsr2ContextDescription contextDesc;

static void up_env(JNIEnv* env) {
    set_env(env);
}
static void check_env(JNIEnv* env) {
    set_env(env);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_fsr2_nativelib_ffx_1fsr2_1api_ffxFsr2GetInterfaceGL(JNIEnv *env, jobject, jint scratchMemorySize, jfloat fsr2Ratio, jint width, jint height, jint flags)
{
    check_env(env);
    unsigned int renderWidth = static_cast<unsigned int>(width / fsr2Ratio);
    unsigned int renderHeight = static_cast<unsigned int>(height / fsr2Ratio);
    contextDesc = {
        .flags = static_cast<unsigned int>(flags),
        .maxRenderSize = {renderWidth, renderHeight},
        .displaySize = {static_cast<unsigned int>(width), static_cast<unsigned int>(height)},
        //.callbacks = {},
        .fpMessage =
            [](FfxFsr2MsgType type, const wchar_t *message){
            char cstr[256] = {};
            wcstombs_s(nullptr, cstr, sizeof(cstr), message, sizeof(cstr));
            cstr[255] = '\0';
            if (type == FFX_FSR2_MESSAGE_TYPE_WARNING) { 
                java_log(cstr,1); 
            }
            else {
                if (type == FFX_FSR2_MESSAGE_TYPE_ERROR) { 
                    java_log(cstr, 2);
                }else{
                    java_log(cstr, 0);
                }

            }
        },
    };
    fsr2ScratchMemory = std::make_unique<char[]>(scratchMemorySize);
    FfxErrorCode code = ffxFsr2GetInterfaceGL(&contextDesc.callbacks, fsr2ScratchMemory.get(), scratchMemorySize, java_glfwGetProcAddress);
    return static_cast<int>(code);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_fsr2_nativelib_ffx_1fsr2_1api_ffxFsr2GetScratchMemorySizeGL(JNIEnv * env, jobject)
{
    check_env(env);
    return static_cast<int>(ffxFsr2GetScratchMemorySizeGL());
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_fsr2_nativelib_ffx_1fsr2_1api_init
(JNIEnv* env, jobject) {
    up_env(env);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_fsr2_nativelib_ffx_1fsr2_1api_ffxFsr2CreateContext
(JNIEnv* env, jobject) {
    check_env(env);
    java_log("ffxFsr2ContextCreate with flags ",0);
    return static_cast<int>(ffxFsr2ContextCreate(&fsr2Context, &contextDesc));
};

JNIEXPORT jint JNICALL Java_io_homo_superresolution_fsr2_nativelib_ffx_1fsr2_1api_ffxFsr2Test
(JNIEnv* env, jobject) {
    check_env(env);
    MessageBox(NULL, L"", L"ÌבÊ¾", MB_OK);
    if (java_glfwGetProcAddress("glCreateTextures") != NULL) {
        (*java_glfwGetProcAddress("glCreateTextures"))();
        return *(int*)java_glfwGetProcAddress("glCreateTextures");
    }
    else {
        return 0;
    }
};
