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
import java.util.Objects;

public class StaticBufferData implements IBufferData {
    private final Buffer buffer;
    private final long size;
    private final boolean owned;

    private StaticBufferData(Buffer buffer, long size, boolean owned) {
        this.buffer = buffer;
        this.size = size;
        this.owned = owned;
    }

    public static StaticBufferData create(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        Buffer buffer = MemoryUtil.memCalloc(1, (int) size);
        return new StaticBufferData(buffer, size, true);
    }

    public static StaticBufferData copy(Buffer buffer) {
        Objects.requireNonNull(buffer, "Buffer cannot be null");
        if (buffer.limit() <= 0) {
            throw new IllegalArgumentException("Buffer must have positive size");
        }

        long size = buffer.limit();
        Buffer internalBuffer = MemoryUtil.memCalloc(1, (int) size);
        MemoryUtil.memCopy(
                MemoryUtil.memAddress(buffer),
                MemoryUtil.memAddress(internalBuffer),
                size
        );
        return new StaticBufferData(internalBuffer, size, true);
    }

    public static StaticBufferData wrap(long address, long size) {
        if (address == 0) {
            throw new IllegalArgumentException("Address cannot be zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        Buffer buffer = MemoryUtil.memByteBuffer(address, (int) size);
        return new StaticBufferData(buffer, size, false);
    }

    public static StaticBufferData wrap(Buffer buffer) {
        Objects.requireNonNull(buffer, "Buffer cannot be null");
        if (buffer.limit() <= 0) {
            throw new IllegalArgumentException("Buffer must have positive size");
        }
        return new StaticBufferData(buffer, buffer.limit(), false);
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
        if (owned) {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public void put(byte[] src, long offset) {
        throw new UnsupportedOperationException("Static buffer does not support modification");
    }

    @Override
    public void updatePartial(Buffer data, long offset, long length) {
        throw new UnsupportedOperationException("Static buffer does not support modification");
    }

    @Override
    public void update(Buffer data) {
        throw new UnsupportedOperationException("Static buffer does not support modification");
    }
}