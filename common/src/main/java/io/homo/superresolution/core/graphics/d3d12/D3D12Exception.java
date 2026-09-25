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

package io.homo.superresolution.core.graphics.d3d12;

public class D3D12Exception extends RuntimeException {
    private final Integer hresult;

    public D3D12Exception(String message) {
        super(message);
        this.hresult = null;
    }

    public D3D12Exception(String message, int hresult) {
        super(message + " (HRESULT 0x" + String.format("%08X", hresult) + ")");
        this.hresult = hresult;
    }

    public Integer hresult() {
        return hresult;
    }

    static void check(int hresult, String operation) {
        if (hresult < 0) {
            throw fromLastError(operation, hresult);
        }
    }

    static long requireHandle(long handle, String operation) {
        if (handle == 0) {
            throw fromLastError(operation, null);
        }
        return handle;
    }

    static D3D12Exception fromLastError(String operation, Integer hresult) {
        String detail = D3D12Native.nGetLastError();
        String message = operation;
        if (detail != null && !detail.isBlank()) {
            message += ": " + detail;
        }
        return hresult == null
                ? new D3D12Exception(message)
                : new D3D12Exception(message, hresult);
    }

    static UnsupportedOperationException unsupported(String capability) {
        return new UnsupportedOperationException(
                "D3D12 RHI capability is unavailable in stage 1: " + capability);
    }
}
