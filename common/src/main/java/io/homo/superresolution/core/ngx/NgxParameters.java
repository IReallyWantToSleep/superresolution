package io.homo.superresolution.core.ngx;

public final class NgxParameters implements AutoCloseable {
    long nativePointer;

    public boolean isValid() {
        return nativePointer != 0;
    }

    public void setUnsignedLong(String name, long value) {
        NgxNative.nParametersSetUnsignedLong(requirePointer(), name, value);
    }

    public void setFloat(String name, float value) {
        NgxNative.nParametersSetFloat(requirePointer(), name, value);
    }

    public void setDouble(String name, double value) {
        NgxNative.nParametersSetDouble(requirePointer(), name, value);
    }

    public void setUnsignedInt(String name, long value) {
        NgxNative.nParametersSetUnsignedInt(requirePointer(), name, value);
    }

    public void setInt(String name, int value) {
        NgxNative.nParametersSetInt(requirePointer(), name, value);
    }

    public void setPointer(String name, long value) {
        NgxNative.nParametersSetPointer(requirePointer(), name, value);
    }

    public int getUnsignedLong(String name, long[] outValue) {
        return NgxNative.nParametersGetUnsignedLong(requirePointer(), name, requireSingle(outValue));
    }

    public int getFloat(String name, float[] outValue) {
        return NgxNative.nParametersGetFloat(requirePointer(), name, requireSingle(outValue));
    }

    public int getDouble(String name, double[] outValue) {
        return NgxNative.nParametersGetDouble(requirePointer(), name, requireSingle(outValue));
    }

    public int getUnsignedInt(String name, long[] outValue) {
        return NgxNative.nParametersGetUnsignedInt(requirePointer(), name, requireSingle(outValue));
    }

    public int getInt(String name, int[] outValue) {
        return NgxNative.nParametersGetInt(requirePointer(), name, requireSingle(outValue));
    }

    public int getPointer(String name, long[] outValue) {
        return NgxNative.nParametersGetPointer(requirePointer(), name, requireSingle(outValue));
    }

    public void reset() {
        NgxNative.nParametersReset(requirePointer());
    }

    public int destroy() {
        if (nativePointer == 0) {
            return NgxConstants.RESULT_SUCCESS;
        }
        int result = NgxNative.nDestroyParameters(nativePointer);
        if (NgxConstants.succeeded(result)) {
            nativePointer = 0;
        }
        return result;
    }

    @Override
    public void close() {
        destroy();
    }

    private long requirePointer() {
        if (nativePointer == 0) {
            throw new IllegalStateException("NGX parameters are not allocated");
        }
        return nativePointer;
    }

    private static long[] requireSingle(long[] value) {
        if (value == null || value.length < 1) {
            throw new IllegalArgumentException("Output array must contain at least one element");
        }
        return value;
    }

    private static float[] requireSingle(float[] value) {
        if (value == null || value.length < 1) {
            throw new IllegalArgumentException("Output array must contain at least one element");
        }
        return value;
    }

    private static double[] requireSingle(double[] value) {
        if (value == null || value.length < 1) {
            throw new IllegalArgumentException("Output array must contain at least one element");
        }
        return value;
    }

    private static int[] requireSingle(int[] value) {
        if (value == null || value.length < 1) {
            throw new IllegalArgumentException("Output array must contain at least one element");
        }
        return value;
    }
}
