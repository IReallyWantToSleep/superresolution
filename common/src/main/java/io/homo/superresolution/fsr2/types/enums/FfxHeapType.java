package io.homo.superresolution.fsr2.types.enums;

public enum FfxHeapType {
    FFX_HEAP_TYPE_DEFAULT(0),
    FFX_HEAP_TYPE_UPLOAD(1);

    private final int value;

    FfxHeapType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}