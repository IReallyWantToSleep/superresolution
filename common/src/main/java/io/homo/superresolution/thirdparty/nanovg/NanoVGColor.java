package io.homo.superresolution.thirdparty.nanovg;

public class NanoVGColor implements AutoCloseable {
    private final long nativeHandle;

    protected NanoVGColor(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    public long getNativeHandle() {
        return nativeHandle;
    }

    public native float nGetNanoVGColorR(long nativeHandle);

    public native float nGetNanoVGColorG(long nativeHandle);

    public native float nGetNanoVGColorB(long nativeHandle);

    public native float nGetNanoVGColorA(long nativeHandle);

    public float r() {
        return nGetNanoVGColorR(nativeHandle);
    }

    public float g() {
        return nGetNanoVGColorG(nativeHandle);
    }


    public float b() {
        return nGetNanoVGColorB(nativeHandle);
    }

    public float a() {
        return nGetNanoVGColorA(nativeHandle);
    }

    public native void nDelete(long nativeHandle);

    @Override
    public void close() {
        nDelete(nativeHandle);
    }
}
