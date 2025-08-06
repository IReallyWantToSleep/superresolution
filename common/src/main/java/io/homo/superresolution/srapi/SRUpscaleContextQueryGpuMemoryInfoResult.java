package io.homo.superresolution.srapi;

public class SRUpscaleContextQueryGpuMemoryInfoResult extends SRUpscaleContextQueryResult {
    public long gpuMemory;

    public SRUpscaleContextQueryGpuMemoryInfoResult(
            long nativePtr,
            long gpuMemory
    ) {
        super(nativePtr);
        this.gpuMemory = gpuMemory;
    }
}
