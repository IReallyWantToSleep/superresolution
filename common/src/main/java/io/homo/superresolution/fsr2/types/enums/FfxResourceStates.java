package io.homo.superresolution.fsr2.types.enums;

public enum FfxResourceStates {
    FFX_RESOURCE_STATE_UNORDERED_ACCESS(1),
    FFX_RESOURCE_STATE_COMPUTE_READ((1 << 1)),
    FFX_RESOURCE_STATE_COPY_SRC((1 << 2)),
    FFX_RESOURCE_STATE_COPY_DEST((1 << 3)),
    FFX_RESOURCE_STATE_GENERIC_READ(FFX_RESOURCE_STATE_COPY_SRC.value | FFX_RESOURCE_STATE_COMPUTE_READ.value);

    private final int value;

    FfxResourceStates(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}