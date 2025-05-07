#include "jni_header.h"
#include <string>
#include "define.h"
#include "utils.h"

JNIEXPORT jstring JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_getVersionInfo(JNIEnv *env, jclass)
{
    check_env(env);
    return (env)->NewStringUTF(SRLIB_VERSION);
}
