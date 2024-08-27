package io.homo.superresolution.fsr2.types.impl;

public interface FfxFsr2Interface {
    void fpCreateBackendContext();
    void fpGetDeviceCapabilities();
    void fpDestroyBackendContext();
    void fpCreateResource();
    void fpRegisterResource();
    void fpUnregisterResources();
    void fpGetResourceDescription();
    void fpDestroyResource();
    void fpCreatePipeline();
    void fpDestroyPipeline();
    void fpScheduleGpuJob();
    void fpExecuteGpuJobs();
    byte[] scratchBuffer = null;
    long scratchBufferSize = 1;
}