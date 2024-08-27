package io.homo.superresolution.fsr2.types.impl;

public interface FfxComputeJobDescription {
    FfxPipelineState pipeline = null;
    int[] dimensions = null;
    FfxResourceInternal[] srvs = null;
    String[] srvNames = null;
    FfxResourceInternal[] uavs = null;
    int[] uavMip = null;
    String[] uavNames = null;
    FfxConstantBuffer[] cbs = null;
    String[] cbNames = null;
    int[] cbSlotIndex = null;
}