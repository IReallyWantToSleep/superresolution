package io.homo.superresolution.core.gl.vertex;

import io.homo.superresolution.core.gl.Gl;

import static io.homo.superresolution.core.gl.Gl.*;

public class GlVertexArray implements AutoCloseable {
    private final int id;

    public GlVertexArray() {
        id = Gl.DSA.createVertexArray();
    }

    public int id() {
        return id;
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        Gl.DSA.deleteVertexArray(id);
    }

}