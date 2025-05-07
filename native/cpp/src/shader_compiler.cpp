#include <glslang/Include/glslang_c_interface.h>
#include <glslang/Public/resource_limits_c.h>
#include <stdint.h>
#include <stdio.h>
#include "jni_header.h"
#include "define.h"
#include "utils.h"

enum class GlslangCompileShaderError
{
    OK = 0,
    PREPROCESS_ERROR = 1,
    PARSE_ERROR = 2,
    LINK_ERROR = 3
};

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
    const char *shaderSource = env->GetStringUTFChars(shaderSrc, NULL);
    const char *outputFilePath = outputFile ? env->GetStringUTFChars(outputFile, NULL) : NULL;
    glslang_input_t input = {
        .language = (glslang_source_t)language,
        .stage = (glslang_stage_t)stage,
        .client = (glslang_client_t)client,
        .client_version = (glslang_target_client_version_t)client_version,
        .target_language = (glslang_target_language_t)target_language,
        .target_language_version = (glslang_target_language_version_t)target_language_version,
        .code = shaderSource,
        .default_version = default_version,
        .default_profile = (glslang_profile_t)default_profile,
        .force_default_version_and_profile = force_default_version_and_profile,
        .forward_compatible = forward_compatible,
        .messages = GLSLANG_MSG_DEFAULT_BIT,
        .resource = glslang_default_resource(),
    };
    glslang_shader_t *shader = glslang_shader_create(&input);
    if (!shader)
    {
        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return NULL;
    }
    if (!glslang_shader_preprocess(shader, &input))
    {
        const char *infoLog = glslang_shader_get_info_log(shader);
        const char *preprocessedCode = glslang_shader_get_preprocessed_code(shader);
        jclass resultClass = env->FindClass("io/homo/superresolution/core/glslang/GlslangCompileShaderResult");
        jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                                 "(Ljava/lang/String;Ljava/lang/String;IJLjava/lang/String;Ljava/lang/String;)V");

        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = preprocessedCode ? env->NewStringUTF(preprocessedCode) : NULL;
        jstring jLog = env->NewStringUTF(infoLog ? infoLog : "");
        jstring jSpirvPath = NULL;

        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        (jint)GlslangCompileShaderError::PREPROCESS_ERROR,
                                        (jlong)0,
                                        jSpirvPath,
                                        jLog);

        glslang_shader_delete(shader);
        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
    }
    const char *preprocessedCode = glslang_shader_get_preprocessed_code(shader);

    if (!glslang_shader_parse(shader, &input))
    {
        const char *infoLog = glslang_shader_get_info_log(shader);

        jclass resultClass = env->FindClass("io/homo/superresolution/core/glslang/GlslangCompileShaderResult");
        jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                                 "(Ljava/lang/String;Ljava/lang/String;IJLjava/lang/String;Ljava/lang/String;)V");

        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessedCode);
        jstring jLog = env->NewStringUTF(infoLog ? infoLog : "");
        jstring jSpirvPath = NULL;
        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        (jint)GlslangCompileShaderError::PARSE_ERROR,
                                        (jlong)0,
                                        jSpirvPath,
                                        jLog);
        glslang_shader_delete(shader);
        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
    }
    glslang_program_t *program = glslang_program_create();
    glslang_program_add_shader(program, shader);

    if (!glslang_program_link(program, GLSLANG_MSG_SPV_RULES_BIT | GLSLANG_MSG_VULKAN_RULES_BIT))
    {
        const char *infoLog = glslang_program_get_info_log(program);

        jclass resultClass = env->FindClass("io/homo/superresolution/core/glslang/GlslangCompileShaderResult");
        jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                                 "(Ljava/lang/String;Ljava/lang/String;IJLjava/lang/String;Ljava/lang/String;)V");

        jstring jSource = env->NewStringUTF(shaderSource);
        jstring jPreprocessed = env->NewStringUTF(preprocessedCode);
        jstring jLog = env->NewStringUTF(infoLog ? infoLog : "");
        jstring jSpirvPath = NULL;

        jobject result = env->NewObject(resultClass, constructor,
                                        jSource,
                                        jPreprocessed,
                                        (jint)GlslangCompileShaderError::LINK_ERROR,
                                        (jlong)0,
                                        jSpirvPath,
                                        jLog);

        glslang_program_delete(program);
        glslang_shader_delete(shader);
        env->ReleaseStringUTFChars(shaderSrc, shaderSource);
        if (outputFile)
            env->ReleaseStringUTFChars(outputFile, outputFilePath);
        return result;
    }

    glslang_program_SPIRV_generate(program, (glslang_stage_t)stage);
    size_t spirvSize = glslang_program_SPIRV_get_size(program);
    uint32_t *spirvData = (uint32_t *)malloc(spirvSize * sizeof(uint32_t));
    glslang_program_SPIRV_get(program, spirvData);
    jstring jSpirvPath = NULL;
    if (outputFilePath)
    {
        FILE *file = fopen(outputFilePath, "wb");
        if (file)
        {
            fwrite(spirvData, sizeof(uint32_t), spirvSize, file);
            fclose(file);
            jSpirvPath = env->NewStringUTF(outputFilePath);
        }
    }

    const char *spirvMessages = glslang_program_SPIRV_get_messages(program);
    jstring jLog = env->NewStringUTF(spirvMessages ? spirvMessages : "");

    jclass resultClass = env->FindClass("io/homo/superresolution/core/glslang/GlslangCompileShaderResult");
    jmethodID constructor = env->GetMethodID(resultClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;IJLjava/lang/String;Ljava/lang/String;)V");

    jobject result = env->NewObject(resultClass, constructor,
                                    env->NewStringUTF(shaderSource),
                                    env->NewStringUTF(preprocessedCode),
                                    (jint)GlslangCompileShaderError::OK,
                                    (jlong)(spirvSize * sizeof(uint32_t)),
                                    jSpirvPath,
                                    jLog);
    free(spirvData);
    glslang_program_delete(program);
    glslang_shader_delete(shader);
    env->ReleaseStringUTFChars(shaderSrc, shaderSource);
    if (outputFile)
        env->ReleaseStringUTFChars(outputFile, outputFilePath);

    return result;
}