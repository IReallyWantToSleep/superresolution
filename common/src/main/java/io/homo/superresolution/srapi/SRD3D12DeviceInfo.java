/*
 * Super Resolution
 * Copyright (c) 2026. Xiang Keshen
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

/**
 * Opaque Direct3D 12 device information passed through SRAPI.
 *
 * <p>{@code device} is the native address of an {@code ID3D12Device}.
 * Keeping the value opaque lets the common Java API expose D3D12 without
 * adding a platform-specific Java binding dependency.</p>
 */
public class SRD3D12DeviceInfo {
    public long device;

    public SRD3D12DeviceInfo() {
        this(0);
    }

    public SRD3D12DeviceInfo(long device) {
        this.device = device;
    }

    public long getDevice() {
        return device;
    }

    public void setDevice(long device) {
        this.device = device;
    }
}
