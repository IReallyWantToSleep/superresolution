package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformBlock;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ShaderUniformBlock<T extends ShaderUniformBuffer> extends ShaderBaseUniform<T, ShaderUniformBlock<?>> implements IShaderUniformBlock<T, ShaderUniformBlock<?>> {
    private final int uboId;

    public ShaderUniformBlock(String name, int binding) {
        super(name, binding);
        uboId = Gl.DSA.createBuffer();
        Gl.DSA.bufferData(uboId, GL45.GL_UNIFORM_BUFFER, null, GL45.GL_DYNAMIC_DRAW);
    }

    @Override
    public ShaderUniformBlock<?> setBuffer(T buffer) {
        return set(buffer);
    }

    @Override
    public T buffer() {
        return current;
    }

    @Override
    public ShaderUniformBlock<?> set(ShaderUniformBuffer value) {
        Gl.DSA.bufferSubData(uboId, 0, value.container());
        Gl.DSA.bindBufferBase(GL45.GL_UNIFORM_BUFFER, binding(), uboId);
        return super.set((T) value);
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(uboId);
    }
}
