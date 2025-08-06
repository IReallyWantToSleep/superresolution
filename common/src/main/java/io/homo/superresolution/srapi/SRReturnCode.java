package io.homo.superresolution.srapi;

public enum SRReturnCode {
    OK(0), ERROR(1);
    public final int value;

    SRReturnCode(int value) {
        this.value = value;
    }

    public static SRReturnCode fromValue(int value) {
        for (SRReturnCode v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown SRReturnCode value: " + value);
    }
}
