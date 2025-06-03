package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShTargetClientVersion {
    EShTargetVulkan_1_0(1 << 22),
    EShTargetVulkan_1_1((1 << 22) | (1 << 12)),
    EShTargetVulkan_1_2((1 << 22) | (2 << 12)),
    EShTargetVulkan_1_3((1 << 22) | (3 << 12)),
    EShTargetVulkan_1_4((1 << 22) | (4 << 12)),
    EShTargetOpenGL_450(450);
    private final int value;

    EShTargetClientVersion(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
