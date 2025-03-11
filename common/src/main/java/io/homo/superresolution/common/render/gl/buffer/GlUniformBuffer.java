package io.homo.superresolution.common.render.gl.buffer;

import io.homo.superresolution.common.render.impl.IUniformStruct;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;

public class GlUniformBuffer<T extends IUniformStruct> {
    private final int bufferSize;
    private final T struct;
    private int uboId;
    private volatile boolean isClosed;

    public GlUniformBuffer(T struct) {
        this.struct = struct;
        this.bufferSize = struct.sizeof();
        validateBufferSize(bufferSize);
        this.uboId = GL15.glGenBuffers();
        initializeBuffer();
    }

    public T struct() {
        return struct;
    }

    private void initializeBuffer() {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        try {
            ByteBuffer data = struct.container();
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
        } finally {
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
    }

    public void update() {
        checkNotClosed();
        validateStructSize();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        try {
            ByteBuffer data = struct.container();
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        } finally {
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
    }

    public void partialUpdate(int offset, ByteBuffer data) {
        checkNotClosed();
        validateOffset(offset, data.remaining());

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        try {
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, data);
        } finally {
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
    }

    public void bind(int bindingPoint) {
        checkNotClosed();
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, uboId);
    }

    public synchronized void delete() {
        if (!isClosed) {
            GL15.glDeleteBuffers(uboId);
            uboId = 0;
            isClosed = true;
        }
    }

    private void checkNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("UBO already deleted");
        }
    }

    private void validateBufferSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
    }

    private void validateStructSize() {
        if (struct.sizeof() != bufferSize) {
            throw new IllegalArgumentException(
                    "Struct size mismatch! Expected: " + bufferSize +
                            ", Actual: " + struct.sizeof()
            );
        }
    }

    private void validateOffset(int offset, int dataSize) {
        if (offset < 0 || (offset + dataSize) > bufferSize) {
            throw new IndexOutOfBoundsException(
                    "Invalid update range: offset=" + offset +
                            ", dataSize=" + dataSize +
                            ", bufferSize=" + bufferSize
            );
        }
    }

    public int getId() {
        return uboId;
    }

    public int getSize() {
        return bufferSize;
    }
}