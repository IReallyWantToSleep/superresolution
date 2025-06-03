package io.homo.superresolution.core.graphics.impl.shader.uniform;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public abstract class ShaderUniformBuffer {
    protected final int size;
    protected final ByteBuffer container;

    protected ShaderUniformBuffer(int size) {
        this.size = size;
        this.container = MemoryUtil.memCalloc(1, sizeof());
    }

    public ByteBuffer container() {
        return container;
    }

    public int sizeof() {
        return size;
    }

    public abstract void fillBuffer();
}
