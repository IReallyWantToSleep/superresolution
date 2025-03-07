package io.homo.superresolution.common.render.gl.vertex;

import static io.homo.superresolution.common.render.gl.Gl.*;

public class VertexArray implements AutoCloseable {
    private final int id;

    public VertexArray() {
        id = glGenVertexArrays();
        bind();
    }

    public void bind() {
        glBindVertexArray(id);
    }

    @Override
    public void close() {
        glDeleteVertexArrays(id);
    }
}