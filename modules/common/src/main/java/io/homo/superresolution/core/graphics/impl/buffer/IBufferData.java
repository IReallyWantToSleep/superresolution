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

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Objects;

@Deprecated
public interface IBufferData {
    /**
     * 数据源
     */
    Buffer container();

    /**
     * 获取数据源大小
     */
    long size();

    /**
     * 获取数据源指针
     */
    default long containerPtr() {
        return MemoryUtil.memAddress(container());
    }

    void free();

    default ByteBuffer asByteBuffer() {
        return MemoryUtil.memByteBuffer(containerPtr(), container().limit());
    }

    default void get(byte[] dest, long offset) {
        ByteBuffer buffer = asByteBuffer();
        Objects.requireNonNull(dest, "Destination array cannot be null");
        if (offset < 0 || offset + dest.length > size()) {
            throw new IndexOutOfBoundsException("Invalid offset or data length");
        }
        int position = buffer.position();
        buffer.position((int) offset);
        buffer.get(dest);
        buffer.position(position);

    }

    void put(byte[] src, long offset);

    void updatePartial(Buffer data, long offset, long length);

    void update(Buffer data);
}
