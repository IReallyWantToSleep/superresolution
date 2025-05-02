package io.homo.superresolution.core.gl.vertex;

import static io.homo.superresolution.core.gl.Gl.*;

public class VertexArray implements AutoCloseable {
    private final int id;

    public VertexArray() {
        id = glGenVertexArrays();
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteVertexArrays(id);
    }
}