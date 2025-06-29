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
