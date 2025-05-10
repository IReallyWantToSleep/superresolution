package io.homo.superresolution.fsr2;

// 枚举：FfxResourceStates
public enum FfxResourceStates {
    UNORDERED_ACCESS(1 << 0),
    COMPUTE_READ(1 << 1),
    COPY_SRC(1 << 2),
    COPY_DEST(1 << 3),
    GENERIC_READ(COPY_SRC.getValue() | COMPUTE_READ.getValue());

    private final int value;

    FfxResourceStates(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
