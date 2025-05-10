package io.homo.superresolution.fsr2;

// 枚举：FfxResourceFlags
public enum FfxResourceFlags {
    NONE(0),
    ALIASABLE(1 << 0);

    private final int value;

    FfxResourceFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
