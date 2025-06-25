package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformBuffer;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.opengl.GL45;

public class GlShaderUniformBuffer<T extends IBufferData> extends GlShaderBaseUniform<T, GlShaderUniformBuffer<?>> implements IShaderUniformBuffer<T, GlShaderUniformBuffer<?>> {
    private final int uboId;

    public GlShaderUniformBuffer(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
        uboId = Gl.DSA.createBuffer();
        Gl.DSA.bufferData(uboId, GL45.GL_UNIFORM_BUFFER, null, GL45.GL_DYNAMIC_DRAW);
    }

    public int getUboId() {
        return uboId;
    }

    @Override
    public GlShaderUniformBuffer<?> setBuffer(T buffer) {
        return set(buffer);
    }

    @Override
    public T buffer() {
        return current;
    }

    @Override
    public GlShaderUniformBuffer<?> set(IBufferData value) {
        Gl.DSA.bufferSubData(uboId, 0, value.container());
        return super.set((T) value);
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(uboId);
    }
}
