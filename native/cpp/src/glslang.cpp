#include "glslang/Public/ShaderLang.h"
#include "jni_header.h"
#include "define.h"
#include "utils.h"
#include <cstdlib>
#include <cstring>

jstring invokeIncludeLocal(
    const char *headerName,
    const char *includerName,
    int inclusionDepth)
{
    JNIEnv *env = get_env();
    jclass helperClass = env->FindClass(JAVA_GLSLANG_INCLUDER_HELPER);
    if (!helperClass)
        return nullptr;

    jmethodID method = env->GetStaticMethodID(
        helperClass,
        "cppIncludeLocal",
        "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;");
    if (!method)
        return nullptr;

    jstring jHeader = env->NewStringUTF(headerName);
    jstring jIncluder = env->NewStringUTF(includerName);

    jstring result = static_cast<jstring>(env->CallStaticObjectMethod(
        helperClass,
        method,
        jHeader,
        jIncluder,
        inclusionDepth));

    env->DeleteLocalRef(jHeader);
    env->DeleteLocalRef(jIncluder);
    return result;
}

jstring invokeIncludeSystem(
    const char *headerName,
    const char *includerName,
    int inclusionDepth)
{
    JNIEnv *env = get_env();
    jclass helperClass = env->FindClass(JAVA_GLSLANG_INCLUDER_HELPER);
    if (!helperClass)
        return nullptr;

    jmethodID method = env->GetStaticMethodID(
        helperClass,
        "cppIncludeSystem",
        "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;");
    if (!method)
        return nullptr;

    jstring jHeader = env->NewStringUTF(headerName);
    jstring jIncluder = env->NewStringUTF(includerName);

    jstring result = static_cast<jstring>(env->CallStaticObjectMethod(
        helperClass,
        method,
        jHeader,
        jIncluder,
        inclusionDepth));

    env->DeleteLocalRef(jHeader);
    env->DeleteLocalRef(jIncluder);
    return result;
}

class ShaderIncluder : public glslang::TShader::Includer
{
public:
    IncludeResult *includeLocal(
        const char *headerName,
        const char *includerName,
        size_t inclusionDepth) override
    {
        jstring result = invokeIncludeLocal(headerName, includerName, inclusionDepth);
        if (!result)
            return nullptr;

        JNIEnv *env = get_env();
        jboolean isCopy = JNI_TRUE;
        const char *content = env->GetStringUTFChars(result, &isCopy);
        size_t len = strlen(content) + 1;
        char *persistentContent = strdup(content);
        env->ReleaseStringUTFChars(result, content);
        env->DeleteLocalRef(result);

        char *headerCopy = strdup(headerName);
        // java_log(headerCopy, 1);
        // java_log(persistentContent, 1);
        return new IncludeResult(
            headerCopy,
            persistentContent,
            len,
            nullptr);
    }

    IncludeResult *includeSystem(
        const char *headerName,
        const char *includerName,
        size_t inclusionDepth) override
    {
        jstring result = invokeIncludeSystem(headerName, includerName, inclusionDepth);
        if (!result)
            return nullptr;

        JNIEnv *env = get_env();
        jboolean isCopy = JNI_TRUE;
        const char *content = env->GetStringUTFChars(result, &isCopy);
        size_t len = strlen(content) + 1;
        char *persistentContent = strdup(content);
        env->ReleaseStringUTFChars(result, content);
        env->DeleteLocalRef(result);

        char *headerCopy = strdup(headerName);
        // java_log(headerCopy, 1);
        // java_log(persistentContent, 1);
        return new IncludeResult(
            headerCopy,
            persistentContent,
            len,
            nullptr);
    }

    void releaseInclude(glslang::TShader::Includer::IncludeResult *result) override
    {
        if (result)
        {
            free((char *)result->headerData);
            delete result;
        }
    }
};

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_initGlslang(JNIEnv *env, jclass)
{
    check_env(env);
    return glslang::InitializeProcess();
};

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_destroyGlslang(JNIEnv *env, jclass)
{
    check_env(env);
    glslang::FinalizeProcess();
    return 0;
};
