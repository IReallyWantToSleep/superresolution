package io.homo.superresolution.core.graphics.impl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Objects;

public class DynamicBufferData implements IBufferData {
    private final Buffer buffer;
    private final long size;
    private boolean dirty = false;

    public DynamicBufferData(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.size = size;
        this.buffer = MemoryUtil.memCalloc(1, (int) size);
    }

    public DynamicBufferData(Buffer buffer, boolean copy) {
        Objects.requireNonNull(buffer, "Buffer cannot be null");
        if (buffer.limit() <= 0) {
            throw new IllegalArgumentException("Buffer must have positive size");
        }

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

    public void update(Buffer data) {
        Objects.requireNonNull(data, "Data buffer cannot be null");
        if (data.limit() != size) {
            throw new IllegalArgumentException("Data size must match buffer size");
        }

        MemoryUtil.memCopy(
                MemoryUtil.memAddress(data),
                MemoryUtil.memAddress(buffer),
                size
        );
        markDirty();
    }

    public void updatePartial(Buffer data, long offset, long length) {
        Objects.requireNonNull(data, "Data buffer cannot be null");
        if (offset < 0 || length < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        if (data.remaining() < length) {
            throw new IllegalArgumentException("Not enough data in input buffer");
        }

        MemoryUtil.memCopy(
                MemoryUtil.memAddress(data),
                MemoryUtil.memAddress(buffer) + offset,
                length
        );
        markDirty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public boolean isDirty() {
        return this.dirty;
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
        Objects.requireNonNull(src, "Source array cannot be null");
        if (offset < 0 || offset + src.length > size) {
            throw new IndexOutOfBoundsException("Invalid offset or data length");
        }

        ByteBuffer byteBuffer = asByteBuffer();
        int position = byteBuffer.position();
        byteBuffer.position((int) offset);
        byteBuffer.put(src);
        byteBuffer.position(position);
        markDirty();
    }
}
