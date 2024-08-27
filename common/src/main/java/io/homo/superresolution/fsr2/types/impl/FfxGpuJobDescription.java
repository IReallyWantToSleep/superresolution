package io.homo.superresolution.fsr2.types.impl;

import io.homo.superresolution.fsr2.types.enums.FfxGpuJobType;

public interface FfxGpuJobDescription {
    FfxGpuJobType jobType = null;
    Object jobDescriptor = null;
}