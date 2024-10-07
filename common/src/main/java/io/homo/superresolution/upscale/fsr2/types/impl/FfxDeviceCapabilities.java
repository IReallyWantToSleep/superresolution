package io.homo.superresolution.upscale.fsr2.types.impl;

import io.homo.superresolution.upscale.fsr2.types.enums.FfxShaderModel;

public interface FfxDeviceCapabilities {
    FfxShaderModel minimumSupportedShaderModel = null;
    int waveLaneCountMin = 0;
    int waveLaneCountMax = 0;
    boolean fp16Supported = false;
    boolean raytracingSupported = false;
}