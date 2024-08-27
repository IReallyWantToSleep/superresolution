package io.homo.superresolution.fsr2.types.impl;

import io.homo.superresolution.fsr2.types.enums.FfxResourceStates;

public interface FfxResource {
    Object resource = null;
    String name = null;
    FfxResourceDescription description = null;
    FfxResourceStates state = null;
    boolean isDepth = false;
    long descriptorData = 0;
}