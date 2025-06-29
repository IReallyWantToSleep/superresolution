package io.homo.superresolution.core.graphics.opengl.buffer;

import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.impl.IUniformStruct;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;

public class GlUniformBuffer<T extends IUniformStruct> {
    private final int bufferSize;
    private final T struct;
    private int uboId;
    private volatile boolean isClosed;

    public GlUniformBuffer(T struct) {
        this.struct = struct;
        this.bufferSize = (int) struct.size();
        validateBufferSize(bufferSize);
        this.uboId = Gl.DSA.createBuffer();
        initializeBuffer();
    }

    public T struct() {
        return struct;
    }

    private void initializeBuffer() {
        ByteBuffer data = struct.container();
        Gl.DSA.bufferData(uboId, GL45.GL_UNIFORM_BUFFER, data, GL45.GL_DYNAMIC_DRAW);
    }

    public void update() {
        checkNotClosed();
        validateStructSize();
        ByteBuffer data = struct.container();
        Gl.DSA.bufferSubData(uboId, 0, data);
    }

    public void partialUpdate(int offset, ByteBuffer data) {
        checkNotClosed();
        validateOffset(offset, data.remaining());
        Gl.DSA.bufferSubData(uboId, offset, data);
    }

    public void bind(int bindingPoint) {
        checkNotClosed();
        Gl.DSA.bindBufferBase(GL45.GL_UNIFORM_BUFFER, bindingPoint, uboId);
    }

    public synchronized void delete() {
        if (!isClosed) {
            Gl.DSA.deleteBuffer(uboId);
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
        if (struct.size() != bufferSize) {
            throw new IllegalArgumentException(
                    "Struct size mismatch! Expected: " + bufferSize +
                            ", Actual: " + struct.size()
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