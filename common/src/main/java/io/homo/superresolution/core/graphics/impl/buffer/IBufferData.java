package io.homo.superresolution.core.graphics.impl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Objects;

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
