package io.homo.superresolution.common.upscale.nis.enums;

public enum NISHDRMode {
    None(0),
    Linear(1),
    PQ(2);
    private final int value;
    NISHDRMode(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
