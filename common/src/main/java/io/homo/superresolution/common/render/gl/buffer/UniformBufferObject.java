package io.homo.superresolution.common.render.gl.buffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class UniformBufferObject<T extends UniformStruct> {
    private int uboId;

    public UniformBufferObject(int size) {
        uboId = GL15.glGenBuffers();
    }

    public void updateStruct(T struct) {
        struct.container().flip();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, struct.container().flip(), GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public void bind(int bindingPoint) {
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, uboId);
    }

    public void delete() {
        GL15.glDeleteBuffers(uboId);
    }
}