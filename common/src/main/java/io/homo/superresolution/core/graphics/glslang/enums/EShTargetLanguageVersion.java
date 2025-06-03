package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShTargetLanguageVersion {
    EShTargetSpv_1_0((1 << 16)),
    EShTargetSpv_1_1((1 << 16) | (1 << 8)),
    EShTargetSpv_1_2((1 << 16) | (2 << 8)),
    EShTargetSpv_1_3((1 << 16) | (3 << 8)),
    EShTargetSpv_1_4((1 << 16) | (4 << 8)),
    EShTargetSpv_1_5((1 << 16) | (5 << 8)),
    EShTargetSpv_1_6((1 << 16) | (6 << 8));
    private final int value;

    EShTargetLanguageVersion(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
