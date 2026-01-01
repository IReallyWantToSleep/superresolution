/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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
