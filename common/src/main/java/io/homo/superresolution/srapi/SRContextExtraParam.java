/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

public class SRContextExtraParam {
    private final long nativePtr;

    protected SRContextExtraParam(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    public String getName() {
        if (nativePtr == 0) return null;
        return SuperResolutionNativeAPI.srParamGetName(nativePtr);
    }

    public SRParamValueType getValueType() {
        if (nativePtr == 0) return SRParamValueType.UNKNOWN;
        int typeValue = SuperResolutionNativeAPI.srParamGetValueType(nativePtr);
        return SRParamValueType.fromValue(typeValue);
    }

    public boolean getAsBool() {
        if (nativePtr == 0) return false;
        return SuperResolutionNativeAPI.srParamGetValueAsBool(nativePtr);
    }

    public int getAsInt32() {
        if (nativePtr == 0) return 0;
        return SuperResolutionNativeAPI.srParamGetValueAsInt32(nativePtr);
    }

    public long getAsUint32() {
        if (nativePtr == 0) return 0L;
        return SuperResolutionNativeAPI.srParamGetValueAsUint32(nativePtr);
    }

    public float getAsFloat() {
        if (nativePtr == 0) return 0.0f;
        return SuperResolutionNativeAPI.srParamGetValueAsFloat(nativePtr);
    }

    public double getAsDouble() {
        if (nativePtr == 0) return 0.0;
        return SuperResolutionNativeAPI.srParamGetValueAsDouble(nativePtr);
    }

    public String getAsString() {
        if (nativePtr == 0) return null;
        return SuperResolutionNativeAPI.srParamGetValueAsString(nativePtr);
    }

    public long getAsPointer() {
        if (nativePtr == 0) return 0L;
        return SuperResolutionNativeAPI.srParamGetValueAsPointer(nativePtr);
    }

    long getNativePtr() {
        return nativePtr;
    }

    public boolean isValid() {
        return nativePtr != 0;
    }
}
