package io.homo.superresolution.fsr2.types.enums;


public enum FfxResourceDimension {
    FFX_RESOURCE_DIMENSION_TEXTURE_1D(0),
    FFX_RESOURCE_DIMENSION_TEXTURE_2D(1);

    private final int value;

    FfxResourceDimension(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
