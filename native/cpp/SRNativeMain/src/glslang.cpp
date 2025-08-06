#include <glslang/Include/glslang_c_interface.h>
#include <glslang/Public/ShaderLang.h>
#include <glslang/Public/ResourceLimits.h>
#include <glslang/SPIRV/GlslangToSpv.h>
#include "jni_header.h"
#include "define.h"
#include "utils.h"
#include <cstdlib>
#include <cstring>
#include <vector>
#include <fstream>

class JniIncluder : public glslang::TShader::Includer
{
public:
    explicit JniIncluder(JNIEnv* env) : env(env) {}

    IncludeResult *includeLocal(const char *headerName, const char *includerName, size_t inclusionDepth) override
    {
        return handleInclude(headerName, includerName, inclusionDepth, true);
    }

    IncludeResult *includeSystem(const char *headerName, const char *includerName, size_t inclusionDepth) override
    {
        return handleInclude(headerName, includerName, inclusionDepth, false);
    }

    void releaseInclude(IncludeResult *result) override
    {
        if (result)
        {
            delete[] result->headerData;
            delete result;
        }
    }

private:
    JNIEnv* env;

    IncludeResult *handleInclude(const char *headerName, const char *includerName, size_t inclusionDepth, bool isLocal)
    {
        jclass helperClass = env->FindClass(JAVA_GLSLANG_INCLUDER_HELPER);
        jmethodID method = env->GetStaticMethodID(
            helperClass,
            isLocal ? "cppIncludeLocal" : "cppIncludeSystem",
            "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;");

        jstring jHeader = env->NewStringUTF(headerName);
        jstring jIncluder = env->NewStringUTF(includerName);

        jstring result = static_cast<jstring>(env->CallStaticObjectMethod(
            helperClass, method, jHeader, jIncluder, static_cast<jint>(inclusionDepth)));

        const char *content = env->GetStringUTFChars(result, nullptr);
        size_t len = env->GetStringUTFLength(result);

        char *contentCopy = new char[len + 1];
        memcpy(contentCopy, content, len);
        contentCopy[len] = '\0';

        env->ReleaseStringUTFChars(result, content);
        env->DeleteLocalRef(result);
        env->DeleteLocalRef(jHeader);
        env->DeleteLocalRef(jIncluder);
        env->DeleteLocalRef(helperClass);

        return new IncludeResult(headerName, contentCopy, len, nullptr);
    }
};


JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_initGlslang(JNIEnv *env, jclass)
{
    return glslang::InitializeProcess() ? 0 : 1;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_destroyGlslang(JNIEnv *env, jclass)
{
    glslang::FinalizeProcess();
    return 0;
}
JNIEXPORT jobject JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_compileShaderToSpirv(
    JNIEnv *env,
    jclass clazz,
    jstring shaderSrc,
    jint stage,
    jint language,
    jint client,
    jint client_version,
    jint target_language,
    jint target_language_version,
    jint default_version,
    jint default_profile,
    jboolean force_default_version_and_profile,
    jboolean forward_compatible)
{
    const char *shaderSource = env->GetStringUTFChars(shaderSrc, nullptr);

    EShLanguage shaderStage = static_cast<EShLanguage>(stage);
    glslang::TShader shader(shaderStage);
    shader.setStrings(&shaderSource, 1);

    TBuiltInResource resources = *GetDefaultResources();
    JniIncluder includer(env);

    shader.setEnvInput(glslang::EShSourceGlsl, shaderStage,
                       static_cast<glslang::EShClient>(client), client_version);
    shader.setEnvClient(static_cast<glslang::EShClient>(client),
                        static_cast<glslang::EShTargetClientVersion>(client_version));
    shader.setEnvTarget(glslang::EShTargetSpv,
                        static_cast<glslang::EShTargetLanguageVersion>(target_language_version));

    std::string preprocessed;
    bool success = shader.preprocess(
        &resources,
        default_version,
        static_cast<EProfile>(default_profile),
        force_default_version_and_profile,
        forward_compatible,
        EShMsgDefault,
        &preprocessed,
        includer);

    jclass resultClass = env->FindClass(JAVA_GLSLANG_COMPILE_RESULT);
    jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;IJLjava/nio/ByteBuffer;Ljava/lang/String;)V");

    auto makeResult = [&](int errorCode, const char *log, jlong size, jobject buffer)
    {
        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessed.c_str());
        jstring jLog = env->NewStringUTF(log ? log : "");
        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        static_cast<jint>(errorCode),
                                        size,
                                        buffer,
                                        jLog);
        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        return result;
    };

    if (!success)
    {
        return makeResult(1, shader.getInfoLog(), 0, nullptr); // PREPROCESS_ERROR
    }
    success = shader.parse(
        &resources,
        default_version,
        static_cast<EProfile>(default_profile),
        force_default_version_and_profile,
        forward_compatible,
        EShMsgDefault,
        includer);
    if (!success)
    {
        return makeResult(2, shader.getInfoLog(), 0, nullptr); // PARSE_ERROR
    }

    glslang::TProgram program;
    program.addShader(&shader);
    success = program.link(EShMsgDefault);
    if (!success)
    {
        return makeResult(3, program.getInfoLog(), 0, nullptr); // LINK_ERROR
    }

    std::vector<unsigned int> spirv;
    glslang::GlslangToSpv(*program.getIntermediate(shaderStage), spirv);

    size_t dataSize = spirv.size() * sizeof(unsigned int);
    jobject spirvBuffer = nullptr;
    if (dataSize > 0)
    {
        void *nativeBuf = malloc(dataSize);
        memcpy(nativeBuf, spirv.data(), dataSize);
        spirvBuffer = env->NewDirectByteBuffer(nativeBuf, dataSize);
    }

    return makeResult(0, program.getInfoLog(), static_cast<jlong>(dataSize), spirvBuffer);
}