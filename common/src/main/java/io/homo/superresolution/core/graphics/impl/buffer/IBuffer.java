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

package io.homo.superresolution.core.graphics.impl.buffer;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;

import java.nio.ByteBuffer;


public interface IBuffer extends GpuObject, Destroyable {
    /**
     * 获取数据
     */
    IBufferData data();

    /**
     * 上传数据
     */
    void upload();

    long getSize();

    BufferUsage getUsage();

    ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write);

    default ByteBuffer map(boolean write) {
        return map(0, Math.toIntExact(getSize()), write);
    }

    void unmap();

    void setBufferData(IBufferData bufferData);

}
