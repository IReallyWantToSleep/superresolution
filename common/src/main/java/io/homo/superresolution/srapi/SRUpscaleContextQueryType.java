package io.homo.superresolution.srapi;

public enum SRUpscaleContextQueryType {
    VERSION_INFO(0),
    GPU_MEMORY_INFO(1);

    public final int value;

    SRUpscaleContextQueryType(int value) {
        this.value = value;
    }
}
