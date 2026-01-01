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

package io.homo.superresolution.core.graphics.impl.vertex;

import io.homo.superresolution.core.graphics.impl.GpuObject;

import java.nio.ByteBuffer;

public interface IVertexBuffer extends GpuObject {
    int getSizeInBytes();

    default int getVertexCount() {
        return getSizeInBytes() / getVertexFormat().stride();
    }

    boolean isDynamic();

    VertexFormat getVertexFormat();

    ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write);

    default ByteBuffer map(boolean write) {
        return map(0, getSizeInBytes(), write);
    }

    void unmap();

    void updateData(ByteBuffer data, int offsetInBytes);

    void updateData(byte[] data, int offsetInBytes, int lengthInBytes);

    default void updateData(ByteBuffer data) {
        updateData(data, 0);
    }

    default void updateData(byte[] data) {
        updateData(data, 0, data.length);
    }
    
    void destroy();
}
