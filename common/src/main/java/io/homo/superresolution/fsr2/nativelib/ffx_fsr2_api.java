package io.homo.superresolution.fsr2.nativelib;

public class ffx_fsr2_api {

    public ffx_fsr2_api(String path){
        System.load(path);
    }
    public native void init();
    public native int ffxFsr2GetScratchMemorySizeGL();
    public native int ffxFsr2CreateContext();
    public native int ffxFsr2GetInterfaceGL(int scratchMemorySize,float fsr2Ratio,int width,int height,int flags);
    public native int ffxFsr2Test();

}
