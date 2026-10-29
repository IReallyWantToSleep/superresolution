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

import io.homo.superresolution.core.graphics.impl.command.ResourceAccessType;
import io.homo.superresolution.core.graphics.impl.command.ResourceState;

public enum D3D12ResourceState {
    COMMON(0, ResourceAccessType.UNDEFINED),
    COMPUTE_READ(1, ResourceAccessType.SAMPLED_READ),
    UNORDERED_ACCESS(2, ResourceAccessType.STORAGE_READ_WRITE),
    COPY_SOURCE(3, ResourceAccessType.TRANSFER_SRC),
    COPY_DESTINATION(4, ResourceAccessType.TRANSFER_DST),
    RENDER_TARGET(5, ResourceAccessType.COLOR_ATTACHMENT_WRITE),
    DEPTH_WRITE(6, ResourceAccessType.DEPTH_ATTACHMENT_WRITE),
    PRESENT(7, ResourceAccessType.UNDEFINED);

    private final int nativeCode;
    private final ResourceState trackerState;

    D3D12ResourceState(int nativeCode, ResourceAccessType accessType) {
        this.nativeCode = nativeCode;
        this.trackerState = new ResourceState(accessType);
    }

    public int nativeCode() {
        return nativeCode;
    }

    public static D3D12ResourceState fromNativeCode(int nativeCode) {
        for (D3D12ResourceState state : values()) {
            if (state.nativeCode == nativeCode) {
                return state;
            }
        }
        throw new D3D12Exception("Unknown native D3D12 resource state code: " + nativeCode);
    }

    public static D3D12ResourceState fromAccessType(ResourceAccessType accessType) {
        return switch (accessType) {
            case UNDEFINED -> COMMON;
            case SAMPLED_READ, STORAGE_READ -> COMPUTE_READ;
            case STORAGE_WRITE, STORAGE_READ_WRITE -> UNORDERED_ACCESS;
            case COLOR_ATTACHMENT_WRITE -> RENDER_TARGET;
            case DEPTH_ATTACHMENT_WRITE -> DEPTH_WRITE;
            case TRANSFER_SRC -> COPY_SOURCE;
            case TRANSFER_DST -> COPY_DESTINATION;
        };
    }

    public ResourceAccessType accessType() {
        return trackerState.accessType();
    }

    ResourceState trackerState() {
        return trackerState;
    }
}
