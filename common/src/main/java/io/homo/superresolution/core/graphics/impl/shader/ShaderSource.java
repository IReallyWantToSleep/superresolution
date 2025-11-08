/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.impl.shader;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.shader.ShaderCompiler;
import io.homo.superresolution.core.utils.FileReadHelper;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShaderSource {
    private final ShaderType type;
    private final String source;
    private final boolean isFilePath;
    private final Map<String, String> shaderDefines = new HashMap<>();
    private String cachedSource = null;

    public ShaderSource(ShaderType type, String content, boolean isFilePath) {
        this.type = type;
        this.source = content;
        this.isFilePath = isFilePath;
    }

    public static ShaderSource text(ShaderType type, String content) {
        return new ShaderSource(type, content, false);
    }

    public static ShaderSource file(ShaderType type, String path) {
        return new ShaderSource(type, path, true);
    }

    public static String addCustomDefines(String source, Map<String, String> defines) {
        if (Gl.isLegacy()) {
            ShaderCompiler.LOGGER.debug("添加SR_GL41_COMPAT定义");
            defines.put("SR_GL41_COMPAT", "1");
        }

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

    public Map<String, String> getShaderDefines() {
        return shaderDefines;
    }

    public ShaderSource addDefine(String key, String value) {
        shaderDefines.put(key, value);
        return this;
    }

    public ShaderSource addDefines(Map<String, String> map) {
        shaderDefines.putAll(map);
        return this;
    }

    public ShaderType getType() {
        return type;
    }

    public void updateSource() {
        String shaderSource = source;
        if (isFilePath) {
            if (!Platform.currentPlatform.isDevelopmentEnvironment()) {
                shaderSource = String.join("\n", FileReadHelper.readText(source));
            }
            Path commonProjectPath = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath())
                    .getParent()
                    .getParent()
                    .resolve("common")
                    .resolve("src")
                    .resolve("main")
                    .resolve("resources");
            Path shaderPath = Path.of(commonProjectPath.toAbsolutePath().toString(), source);
            if (shaderPath.toFile().exists()) {
                try {
                    shaderSource = Files.readString(shaderPath);
                    SuperResolution.LOGGER.info("加载Shader {}", shaderPath);

                } catch (Throwable e) {
                    shaderSource = String.join("\n", FileReadHelper.readText(source));
                }
            } else {
                shaderSource = String.join("\n", FileReadHelper.readText(source));
            }
        }

        cachedSource = addCustomDefines(shaderSource, shaderDefines);
    }

    public String getSource() {
        if (cachedSource == null) updateSource();
        return cachedSource;
    }

    public boolean isFilePath() {
        return isFilePath;
    }
}
