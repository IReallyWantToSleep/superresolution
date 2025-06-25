package io.homo.superresolution.core.graphics.opengl.vertex;

import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class GlVertexBuffer implements IVertexBuffer {
    private final int id;
    private final int size;
    private final boolean dynamic;

    public GlVertexBuffer(int id, int size, boolean dynamic) {
        this.id = id;
        this.size = size;
        this.dynamic = dynamic;
    }

    public static GlVertexBuffer create(VertexBufferDescription description) {
        int bufferId = Gl.DSA.createBuffer();
        int usage = description.isDynamic() ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW;
        Gl.DSA.bufferData(bufferId, GL15.GL_ARRAY_BUFFER, MemoryUtil.memAlloc(description.getSizeInBytes()), usage);
        return new GlVertexBuffer(bufferId, description.getSizeInBytes(), description.isDynamic());
    }


    @Override
    public int handle() {
        return id;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public void updateData(float[] data, int offset, int length) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(length);
        buffer.put(data, offset, length);
        buffer.flip();
        Gl.DSA.bufferSubData(this.id, 0, buffer);
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(id);
    }
}