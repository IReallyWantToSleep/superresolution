package io.homo.superresolution.srapi;

public class SRUpscaleContextQueryVersionInfoResult extends SRUpscaleContextQueryResult {
    public long gpuMemory;
    public long versionNumber;
    public long versionId;
    public String versionName;

    public SRUpscaleContextQueryVersionInfoResult(
            long nativePtr,
            long versionNumber,
            long versionId,
            String versionName
    ) {
        super(nativePtr);
        this.versionNumber = versionNumber;
        this.versionId = versionId;
        this.versionName = versionName;
    }


}
