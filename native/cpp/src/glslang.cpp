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
    IncludeResult *handleInclude(const char *headerName, const char *includerName, size_t inclusionDepth, bool isLocal)
    {
        JNIEnv *env = get_env();
        jmethodID method = env->GetStaticMethodID(
            env->FindClass(JAVA_GLSLANG_INCLUDER_HELPER),
            isLocal ? "cppIncludeLocal" : "cppIncludeSystem",
            "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;");

        jstring jHeader = env->NewStringUTF(headerName);
        jstring jIncluder = env->NewStringUTF(includerName);

        jstring result = static_cast<jstring>(env->CallStaticObjectMethod(
            env->FindClass(JAVA_GLSLANG_INCLUDER_HELPER),
            method,
            jHeader,
            jIncluder,
            static_cast<jint>(inclusionDepth)));

        const char *content = env->GetStringUTFChars(result, nullptr);
        size_t len = env->GetStringLength(result);

        char *contentCopy = new char[len + 1];
        strcpy(contentCopy, content);

        env->ReleaseStringUTFChars(result, content);
        env->DeleteLocalRef(result);
        env->DeleteLocalRef(jHeader);
        env->DeleteLocalRef(jIncluder);

        return new IncludeResult(headerName, contentCopy, len, nullptr);
    }
};

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_initGlslang(JNIEnv *env, jclass)
{
    check_env(env);
    return glslang::InitializeProcess() ? 0 : 1;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_destroyGlslang(JNIEnv *env, jclass)
{
    check_env(env);
    glslang::FinalizeProcess();
    return 0;
}

JNIEXPORT jobject JNICALL Java_io_homo_superresolution_core_SuperResolutionNative_compileShaderToSpirv(
    JNIEnv *env,
    jclass clazz,
    jstring shaderSrc,
    jstring outputFile,
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

    check_env(env);
    const char *shaderSource = env->GetStringUTFChars(shaderSrc, nullptr);
    const char *outputFilePath = outputFile ? env->GetStringUTFChars(outputFile, nullptr) : nullptr;

    EShLanguage shaderStage = static_cast<EShLanguage>(stage);
    glslang::TShader shader(shaderStage);
    shader.setStrings(&shaderSource, 1);

    TBuiltInResource resources = *GetDefaultResources();
    JniIncluder includer;

    shader.setEnvInput(glslang::EShSourceGlsl, shaderStage,
                       static_cast<glslang::EShClient>(client), client_version);
    shader.setEnvClient(static_cast<glslang::EShClient>(client),
                        static_cast<glslang::EShTargetClientVersion>(client_version));
    shader.setEnvTarget(glslang::EShTargetSpv,
                        static_cast<glslang::EShTargetLanguageVersion>(target_language_version));
    std::vector<std::string> extensions = {
        "GL_EXT_shader_16bit_storage",
        "GL_EXT_shader_explicit_arithmetic_types",
        "GL_NV_gpu_shader5"};
    shader.addProcesses(extensions);
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

    jclass resultClass = env->FindClass("io/homo/superresolution/core/glslang/GlslangCompileShaderResult");
    jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;IJLjava/lang/String;Ljava/lang/String;)V");

    if (!success)
    {
        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessed.c_str());
        jstring jLog = env->NewStringUTF(shader.getInfoLog());
        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        static_cast<jint>(1), // PREPROCESS_ERROR
                                        static_cast<jlong>(0),
                                        nullptr,
                                        jLog);

        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
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
        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessed.c_str());
        jstring jLog = env->NewStringUTF(shader.getInfoLog());
        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        static_cast<jint>(2), // PARSE_ERROR
                                        static_cast<jlong>(0),
                                        nullptr,
                                        jLog);

        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
    }

    glslang::TProgram program;
    program.addShader(&shader);
    success = program.link(EShMsgDefault);
    if (!success)
    {
        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessed.c_str());
        jstring jLog = env->NewStringUTF(program.getInfoLog());
        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        static_cast<jint>(3), // LINK_ERROR
                                        static_cast<jlong>(0),
                                        nullptr,
                                        jLog);

        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
    }

    std::vector<unsigned int> spirv;
    glslang::GlslangToSpv(*program.getIntermediate(shaderStage), spirv);

    jstring jSpirvPath = nullptr;
    if (outputFilePath)
    {
        std::ofstream out(outputFilePath, std::ios::binary);
        if (out)
        {
            out.write(reinterpret_cast<const char *>(spirv.data()), spirv.size() * sizeof(unsigned int));
            out.close();
            jSpirvPath = env->NewStringUTF(outputFilePath);
        }
    }

    jstring jSource = env->NewStringUTF(shaderSource);
    jstring jPreprocessed = env->NewStringUTF(preprocessed.c_str());
    jstring jLog = env->NewStringUTF(program.getInfoLog());

    jobject result = env->NewObject(resultClass, constructor,
                                    jSource,
                                    jPreprocessed,
                                    static_cast<jint>(0), // OK
                                    static_cast<jlong>(spirv.size() * sizeof(unsigned int)),
                                    jSpirvPath,
                                    jLog);

    env->ReleaseStringUTFChars(shaderSrc, shaderSource);
    if (outputFile)
        env->ReleaseStringUTFChars(outputFile, outputFilePath);

    return result;
}