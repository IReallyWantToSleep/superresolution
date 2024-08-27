package io.homo.superresolution.fsr2.types.enums;

public enum FfxGpuJobType {
    FFX_GPU_JOB_CLEAR_FLOAT(0),
    FFX_GPU_JOB_COPY(1),
    FFX_GPU_JOB_COMPUTE(2);

    private final int value;

    FfxGpuJobType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}