package io.homo.superresolution.core.graphics.glslang.enums;

public enum EProfile {
    EBadProfile(0),
    ENoProfile((1)),
    ECoreProfile((1 << 1)),
    ECompatibilityProfile((1 << 2)),
    EEsProfile((1 << 3));
    private final int value;

    EProfile(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
