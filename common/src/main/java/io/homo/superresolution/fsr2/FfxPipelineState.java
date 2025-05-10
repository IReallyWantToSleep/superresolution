package io.homo.superresolution.fsr2;

// 类：FfxPipelineState
public class FfxPipelineState {
    public Object rootSignature;
    public Object pipeline;
    public int uavCount;
    public int srvCount;
    public int constCount;
    public FfxResourceBinding[] uavResourceBindings = new FfxResourceBinding[8];
    public FfxResourceBinding[] srvResourceBindings = new FfxResourceBinding[16];
    public FfxResourceBinding[] cbResourceBindings = new FfxResourceBinding[2];
}
