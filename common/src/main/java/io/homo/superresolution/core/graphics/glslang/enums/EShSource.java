package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShSource {
    EShSourceNone(0),
    EShSourceGlsl(1),
    EShSourceHlsl(2);
    private final int value;

    EShSource(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
