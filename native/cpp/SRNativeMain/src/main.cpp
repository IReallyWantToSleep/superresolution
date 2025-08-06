#include "jni_header.h"
#include <string>
#include "define.h"
#include "utils.h"

JNIEXPORT jstring JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_getVersionInfo(JNIEnv *env, jclass)
{
    return (env)->NewStringUTF(SRLIB_VERSION);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_freeDirectBuffer(JNIEnv *env, jclass, jobject buffer)
{
    void *ptr = env->GetDirectBufferAddress(buffer);
    if (ptr)
        free(ptr);
}
