package io.homo.superresolution.core.glslang;

import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.glslang.enums.*;

public class GlslangShaderCompiler {

    public static void init() {
        SuperResolutionNative.initGlslang();
    }

    public static GlslangCompileShaderResult compileShaderToSpirv(
            String shaderSrc,
            String outputFile,
            EShLanguage stage,
            EShSource language,
            EShClient client,
            EShTargetClientVersion clientVersion,
            EShTargetLanguage targetLanguage,
            EShTargetLanguageVersion targetLanguageVersion,
            int defaultVersion,
            EProfile defaultProfile,
            boolean forceDefaultVersionAndProfile,
            boolean forwardCompatible
    ) {
        return SuperResolutionNative.compileShaderToSpirv(
                shaderSrc,
                outputFile,
                stage.getValue(),
                language.getValue(),
                client.getValue(),
                clientVersion.getValue(),
                targetLanguage.getValue(),
                targetLanguageVersion.getValue(),
                defaultVersion,
                defaultProfile.getValue(),
                forceDefaultVersionAndProfile,
                forwardCompatible
        );
    }

    public static void main(String[] args) {
        // 检查并加载依赖库
        if (!NativeLibManager.check("I:\\super_resolution_moddev\\superresolution\\run")) {
            NativeLibManager.extract("I:\\super_resolution_moddev\\superresolution\\run");
        }
        NativeLibManager.load("I:\\super_resolution_moddev\\superresolution\\run");
        init();

        // 顶点着色器源代码
        String shaderSrc = """
                #define A 2.0
                #version 430
                #extension GL_GOOGLE_include_directive : enable
                #include "a.h"
                precision mediump float;
                
                layout (location = 0) in vec2 aPosition;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 0) out vec2 vTexCoord;
                void main() {
                    vTexCoord = aTexCoord;
                    gl_Position = vec4(aPosition, A, 1.0);
                }
                """;

        GlslangCompileShaderResult result =
                SuperResolutionNative.compileShaderToSpirv(
                        shaderSrc,
                        "I:\\super_resolution_moddev\\superresolution\\run\\test.spv",
                        EShLanguage.EShLangVertex.getValue(),
                        EShSource.EShSourceGlsl.getValue(),
                        EShClient.EShClientOpenGL.getValue(),
                        EShTargetClientVersion.EShTargetOpenGL_450.getValue(),
                        EShTargetLanguage.EShTargetSpv.getValue(),
                        EShTargetLanguageVersion.EShTargetSpv_1_0.getValue(),
                        430,
                        EProfile.ENoProfile.getValue(),
                        true,
                        false
                );
        System.out.println(result.preprocessedCode());
        System.out.println(result.sourceCode());
        System.out.println(result.spirVDataSize());
        System.out.println(result.spirVFilePath());
        System.out.println(result.log());
        System.out.println(result.error().getValue());
    }
}