package io.homo.superresolution.upscale.fsr2.types.enums;

public enum FfxResourceFlags {
    FFX_RESOURCE_FLAGS_NONE(0),
    FFX_RESOURCE_FLAGS_ALIASABLE(1);

    private final int value;

    FfxResourceFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
