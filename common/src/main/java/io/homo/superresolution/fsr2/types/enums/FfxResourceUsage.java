package io.homo.superresolution.fsr2.types.enums;

public enum FfxResourceUsage {
    FFX_RESOURCE_USAGE_READ_ONLY(0),
    FFX_RESOURCE_USAGE_RENDERTARGET(1),
    FFX_RESOURCE_USAGE_UAV(2);

    private final int value;

    FfxResourceUsage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}