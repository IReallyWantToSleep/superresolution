package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShTargetLanguage {
    EShTargetNone(0),
    EShTargetSpv(1);
    private final int value;

    EShTargetLanguage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
