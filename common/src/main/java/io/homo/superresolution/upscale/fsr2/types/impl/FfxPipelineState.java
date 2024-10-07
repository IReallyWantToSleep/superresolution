package io.homo.superresolution.upscale.fsr2.types.impl;

import io.homo.superresolution.upscale.fsr2.types.FfxTypes;

public interface FfxPipelineState {
    FfxTypes.FfxRootSignature rootSignature = null;
    FfxTypes.FfxPipeline pipeline = null;
    int uavCount = 0;
    int srvCount = 0;
    int constCount = 0;
    FfxResourceBinding[] uavResourceBindings = null;
    FfxResourceBinding[] srvResourceBindings = null;
    FfxResourceBinding[] cbResourceBindings = null;
}