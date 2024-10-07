package io.homo.superresolution.upscale.fsr2.types.enums;

public enum FfxResourceViewType {
    FFX_RESOURCE_VIEW_UNORDERED_ACCESS(0),
    FFX_RESOURCE_VIEW_SHADER_READ(1);

    private final int value;

    FfxResourceViewType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}