package io.homo.superresolution.core.graphics.glslang;

import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionNative;
import io.homo.superresolution.core.graphics.glslang.enums.*;

import java.util.*;

public class GlslangShaderCompiler {

    public static void init() {
        SuperResolutionNative.initGlslang();
    }

    public static GlslangCompileShaderResult compileShaderToSpirv(
            String shaderSrc,
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

    public static String addCustomDefines(String source, Map<String, String> defines) {
        if (defines.isEmpty()) {
            return source;
        }
        StringBuilder definesBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : defines.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            definesBuilder.append("#define ")
                    .append(key);
            if (value != null && !value.isEmpty()) {
                definesBuilder.append(" ").append(value);
            }
            definesBuilder.append("\n");
        }
        String defineBlock = definesBuilder.toString().trim();
        String[] lines = source.split("\\R");
        List<String> linesList = new ArrayList<>(Arrays.asList(lines));
        int versionLine = -1;
        for (int i = 0; i < linesList.size(); i++) {
            if (linesList.get(i).trim().startsWith("#version")) {
                versionLine = i;
                break;
            }
        }
        if (versionLine == -1) {
            throw new IllegalArgumentException("Shader source must contain #version directive");
        }
        if (!defineBlock.isEmpty()) {
            String[] defineLines = defineBlock.split("\\R");
            for (int i = 0; i < defineLines.length; i++) {
                linesList.add(versionLine + 1 + i, defineLines[i]);
            }
        }
        String lineSeparator = source.contains("\r\n") ? "\r\n" : "\n";
        return String.join(lineSeparator, linesList);
    }


    public static void main(String[] args) {
        NativeLibManager.extract("I:\\super_resolution_moddev\\superresolution\\run");
        NativeLibManager.load("I:\\super_resolution_moddev\\superresolution\\run");
        init();
        Map<String, String> defines = new HashMap<>();
        defines.put("A", "114");
        String shaderSrc = addCustomDefines("""
                            //#version 430
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
                        """,
                defines
        );
/*
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
        System.out.println(result.error().getValue());*/
    }
}