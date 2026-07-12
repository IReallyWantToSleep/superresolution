package io.homo.superresolution.core.ngx;

public final class NgxFeature implements AutoCloseable {
    long nativePointer;

    public boolean isValid() {
        return nativePointer != 0;
    }

    public int release() {
        if (nativePointer == 0) {
            return NgxConstants.RESULT_SUCCESS;
        }
        int result = NgxNative.nReleaseFeature(nativePointer);
        if (NgxConstants.succeeded(result)) {
            nativePointer = 0;
        }
        return result;
    }

    @Override
    public void close() {
        release();
    }
}
