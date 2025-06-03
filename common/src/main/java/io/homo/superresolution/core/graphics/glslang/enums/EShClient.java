package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShClient {
    EShClientNone(0),
    EShClientVulkan(1),
    EShClientOpenGL(2);
    private final int value;

    EShClient(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
