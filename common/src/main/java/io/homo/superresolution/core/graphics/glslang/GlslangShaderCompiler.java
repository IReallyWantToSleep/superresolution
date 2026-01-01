/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    }
}