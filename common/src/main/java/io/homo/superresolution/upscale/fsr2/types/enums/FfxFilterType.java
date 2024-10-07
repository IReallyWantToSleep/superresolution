package io.homo.superresolution.upscale.fsr2.types.enums;

public enum FfxFilterType {
    FFX_FILTER_TYPE_POINT(0),
    FFX_FILTER_TYPE_LINEAR(1);

    private final int value;

    FfxFilterType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}