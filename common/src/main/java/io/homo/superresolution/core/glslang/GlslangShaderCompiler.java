package io.homo.superresolution.core.glslang;

import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.glslang.enums.*;
import io.homo.superresolution.core.glslang.GlslangCompileShaderResult;

public class GlslangShaderCompiler {

    public static void init() {
        SuperResolutionNative.initGlslang();
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
                #version 430
                precision mediump float;
                
                layout (location = 0) in vec2 aPosition;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 0) out vec2 vTexCoord;
                void main() {
                    vTexCoord = aTexCoord;
                    gl_Position = vec4(aPosition, 0.0+1.0, 1.0);
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