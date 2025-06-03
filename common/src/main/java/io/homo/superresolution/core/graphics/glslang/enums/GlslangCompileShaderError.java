package io.homo.superresolution.core.graphics.glslang.enums;

public enum GlslangCompileShaderError {
    OK(0),
    PREPROCESS_ERROR(1),
    PARSE_ERROR(2),
    LINK_ERROR(3);
    private final int value;

    GlslangCompileShaderError(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
