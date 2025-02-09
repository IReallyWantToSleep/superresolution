package io.homo.superresolution.common.upscale.nis.enums;

public enum NISGPUArchitecture {
    NVIDIA_Generic(0),
    AMD_Generic(1),
    Intel_Generic(2),
    NVIDIA_Generic_fp16(3);
    private final int value;
    NISGPUArchitecture(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
