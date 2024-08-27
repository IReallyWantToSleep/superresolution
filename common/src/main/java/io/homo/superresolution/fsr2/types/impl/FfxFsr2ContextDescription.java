package io.homo.superresolution.fsr2.types.impl;

import io.homo.superresolution.fsr2.types.FfxTypes;

public interface FfxFsr2ContextDescription {
    int flags = 0;
    FfxDimensions2D maxRenderSize = null;
    FfxDimensions2D displaySize = null;
    FfxFsr2Interface callbacks = null;
    FfxTypes.FfxDevice device = null;
    FfxFsr2Message fpMessage = null;
}
