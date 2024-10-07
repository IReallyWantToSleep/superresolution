package io.homo.superresolution.upscale.fsr2.types.enums;

public enum FfxResourceType {
    FFX_RESOURCE_TYPE_BUFFER(0),
    FFX_RESOURCE_TYPE_TEXTURE1D(1),
    FFX_RESOURCE_TYPE_TEXTURE2D(2),
    FFX_RESOURCE_TYPE_TEXTURE3D(3);

    private final int value;

    FfxResourceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}