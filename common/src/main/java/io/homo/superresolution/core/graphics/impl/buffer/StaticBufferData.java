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

package io.homo.superresolution.core.graphics.impl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;

public class StaticBufferData implements IBufferData {
    private final Buffer buffer;
    private final long size;

    public StaticBufferData(long size) {
        this.size = size;
        this.buffer = MemoryUtil.memCalloc(1, (int) size);
    }

    public StaticBufferData(Buffer buffer, boolean copy) {
        this.size = buffer.limit();
        if (copy) {
            this.buffer = MemoryUtil.memCalloc(1, (int) size);
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(buffer),
                    MemoryUtil.memAddress(this.buffer),
                    size
            );
        } else {
            this.buffer = buffer;
        }
    }

    public StaticBufferData(Buffer buffer) {
        this(buffer, true);
    }

    @Override
    public Buffer container() {
        return buffer;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void free() {
        MemoryUtil.memFree(buffer);
    }

    @Override
    public void put(byte[] src, long offset) {
        throw new RuntimeException();
    }

    @Override
    public void updatePartial(Buffer data, long offset, long length) {
        throw new RuntimeException();
    }

    @Override
    public void update(Buffer data) {
        throw new RuntimeException();
    }
}
