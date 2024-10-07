package io.homo.superresolution.upscale.fsr2.types.impl;

import io.homo.superresolution.upscale.fsr2.types.enums.FfxResourceFlags;
import io.homo.superresolution.upscale.fsr2.types.enums.FfxResourceType;
import io.homo.superresolution.upscale.fsr2.types.enums.FfxSurfaceFormat;

public interface FfxResourceDescription {
    FfxResourceType type = null;
    FfxSurfaceFormat format = null;
    int width = 0;
    int height =0;
    int depth = 0;
    int mipCount = 0;
    FfxResourceFlags flags = null;
}