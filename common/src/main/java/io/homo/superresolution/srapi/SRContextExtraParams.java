/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.srapi;

public class SRContextExtraParams {
    private long nativePtr;

    public SRContextExtraParams() {
        this.nativePtr = SuperResolutionNativeAPI.srCreateParams();
    }

    SRContextExtraParams(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    public SRReturnCode setBool(String name, boolean value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetBool(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setInt32(String name, int value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetInt32(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setUint32(String name, long value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetUint32(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setFloat(String name, float value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetFloat(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setDouble(String name, double value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetDouble(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setString(String name, String value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetString(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRReturnCode setPointer(String name, long value) {
        if (nativePtr == 0 || name == null) {
            return SRReturnCode.NULL_POINTER;
        }
        int code = SuperResolutionNativeAPI.srParamsSetPointer(nativePtr, name, value);
        return SRReturnCode.fromValue(code);
    }

    public SRContextExtraParam findParam(String name) {
        if (nativePtr == 0 || name == null) {
            return null;
        }
        long paramPtr = SuperResolutionNativeAPI.srFindParam(nativePtr, name).getNativePtr();
        if (paramPtr == 0) {
            return null;
        }
        return new SRContextExtraParam(paramPtr);
    }

    public boolean getBool(String name, boolean defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetBool(nativePtr, name, defaultValue);
    }

    public int getInt32(String name, int defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetInt32(nativePtr, name, defaultValue);
    }

    public long getUint32(String name, long defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetUint32(nativePtr, name, defaultValue);
    }

    public float getFloat(String name, float defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetFloat(nativePtr, name, defaultValue);
    }

    public double getDouble(String name, double defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetDouble(nativePtr, name, defaultValue);
    }

    public String getString(String name, String defaultValue) {
        if (nativePtr == 0 || name == null) {
            return defaultValue;
        }
        return SuperResolutionNativeAPI.srParamsGetString(nativePtr, name, defaultValue);
    }

    public long getPointer(String name) {
        if (nativePtr == 0 || name == null) {
            return 0L;
        }
        return SuperResolutionNativeAPI.srParamsGetPointer(nativePtr, name);
    }


    long getNativePtr() {
        return nativePtr;
    }

    public void destroy() {
        if (nativePtr != 0) {
            SuperResolutionNativeAPI.srDestroyParams(nativePtr);
            nativePtr = 0;
        }
    }

    public boolean isValid() {
        return nativePtr != 0;
    }
}
