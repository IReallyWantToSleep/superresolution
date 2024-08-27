package io.homo.superresolution.fsr2.types.impl;

import io.homo.superresolution.fsr2.types.enums.FfxHeapType;
import io.homo.superresolution.fsr2.types.enums.FfxResourceStates;
import io.homo.superresolution.fsr2.types.enums.FfxResourceUsage;

public interface FfxCreateResourceDescription {
    FfxHeapType heapType = null;
    FfxResourceDescription resourceDescription = null;
    FfxResourceStates initalState = null;
    int initDataSize = 0;
    Object initData = null;
    String name = null;
    FfxResourceUsage usage = null;
    int id = 0;
}