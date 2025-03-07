package io.homo.superresolution.common.render.gl.buffer;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class VertexBuffer implements AutoCloseable {
    private final int id;

    public VertexBuffer() {
        id = glGenBuffers();
    }

    public void bind(int target) {
        glBindBuffer(target, id);
    }

    public void uploadData(float[] data, int usage) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
    }

    @Override
    public void close() {
        glDeleteBuffers(id);
    }
}