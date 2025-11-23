package io.homo.superresolution.thirdparty.nanovg;

public class NanoVGPaint implements AutoCloseable {
    private long nativeHandle;

    public NanoVGPaint(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    public long getNativeHandle() {
        return nativeHandle;
    }

    public native void nDelete(long nativeHandle);

    @Override
    public void close() throws Exception {
        nDelete(nativeHandle);
    }
}
