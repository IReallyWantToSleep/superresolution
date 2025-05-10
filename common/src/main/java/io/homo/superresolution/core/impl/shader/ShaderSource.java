package io.homo.superresolution.core.impl.shader;

import io.homo.superresolution.core.utils.FileReadHelper;

import java.util.*;

public class ShaderSource {
    private final Type type;
    private final String source;
    private final boolean isFilePath;
    private Map<String, String> shaderDefines = new HashMap<>();

    public ShaderSource(Type type, String content, boolean isFilePath) {
        this.type = type;
        this.source = content;
        this.isFilePath = isFilePath;
    }


    public ShaderSource(Type type, String content) {
        this(type, content, false);
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

    public void setShaderDefines(Map<String, String> shaderDefines) {
        this.shaderDefines = shaderDefines;
    }

    public Type getType() {
        return type;
    }

    public String getSource() {
        return addCustomDefines(isFilePath ? String.join("\n", FileReadHelper.readText(source)) : source, shaderDefines);
    }

    public boolean isFilePath() {
        return isFilePath;
    }


    public enum Type {
        VERTEX,
        FRAGMENT,
        COMPUTE
    }
}
