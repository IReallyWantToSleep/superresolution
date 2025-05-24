package io.homo.superresolution.core.gl.vertex;

import io.homo.superresolution.core.gl.Gl;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL45;

import java.nio.*;

import static io.homo.superresolution.core.gl.Gl.*;

public class GlVertexBuffer implements AutoCloseable {
    private final int id;
    private int target;

    public GlVertexBuffer() {
        id = Gl.DSA.createBuffer();
    }

    public void bind(int target) {
        this.target = target;
        glBindBuffer(target, id);
    }

    public void uploadData(float[] data, int usage) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();
        //glBufferData(target, buffer, usage);
        Gl.DSA.bufferData(id, target, buffer, usage);
    }

    public void uploadData(int[] data, int usage) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data).flip();
        //glBufferData(target, buffer, usage);
        Gl.DSA.bufferData(id, target, buffer, usage);
    }

    @Override
    public void close() {
        Gl.DSA.deleteBuffer(id);
    }

    public int getId() {
        return id;
    }
}