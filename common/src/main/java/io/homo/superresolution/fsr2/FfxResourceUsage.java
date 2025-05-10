package io.homo.superresolution.fsr2;

// 枚举：FfxResourceUsage
public enum FfxResourceUsage {
    READ_ONLY(0),
    RENDERTARGET(1 << 0),
    UAV(1 << 1);

    private final int value;

    FfxResourceUsage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
