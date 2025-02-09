package io.homo.superresolution.common.upscale.nis.struct;

import io.homo.superresolution.common.upscale.nis.enums.NISGPUArchitecture;

public class NISOptimizer {
    private boolean isUpscaling;
    private NISGPUArchitecture gpuArch;
    public int getOptimalBlockWidth() {
        switch (gpuArch) {
            case NVIDIA_Generic:
            case NVIDIA_Generic_fp16:
            case AMD_Generic:
            case Intel_Generic:
                return 32;
            default:
                return 32;
        }
    }

    public int getOptimalBlockHeight() {
        switch (gpuArch) {
            case NVIDIA_Generic:
                return isUpscaling ? 24 : 32;
            case NVIDIA_Generic_fp16:
                return 32;
            case AMD_Generic:
                return isUpscaling ? 24 : 32;
            case Intel_Generic:
                return isUpscaling ? 24 : 32;
            default:
                return isUpscaling ? 24 : 32;
        }
    }

    public int getOptimalThreadGroupSize() {
        switch (gpuArch) {
            case NVIDIA_Generic:
            case NVIDIA_Generic_fp16:
                return 128;
            case AMD_Generic:
            case Intel_Generic:
                return 256;
            default:
                return 256;
        }
    }
}
