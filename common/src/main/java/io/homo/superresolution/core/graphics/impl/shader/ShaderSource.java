package io.homo.superresolution.core.graphics.impl.shader;

import io.homo.superresolution.core.utils.FileReadHelper;

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
        cachedSource = addCustomDefines(isFilePath ? String.join("\n", FileReadHelper.readText(source)) : source, shaderDefines);
    }

    public String getSource() {
        if (cachedSource == null) updateSource();
        return cachedSource;
    }

    public boolean isFilePath() {
        return isFilePath;
    }
}
