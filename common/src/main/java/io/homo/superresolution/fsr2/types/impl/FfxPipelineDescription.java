package io.homo.superresolution.fsr2.types.impl;

import io.homo.superresolution.fsr2.types.enums.FfxFilterType;

public interface FfxPipelineDescription {
    int contextFlags = 0;
    FfxFilterType[] samplers = null;
    int samplerCount = 0;
    int[] rootConstantBufferSizes = null;
    int rootConstantBufferCount = 0;
}