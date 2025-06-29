package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformBuffer;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;

public class GlShaderUniformBuffer<T extends IBuffer> extends GlShaderBaseUniform<T, GlShaderUniformBuffer<?>> implements IShaderUniformBuffer<T, GlShaderUniformBuffer<?>> {
    public GlShaderUniformBuffer(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
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
    public GlShaderUniformBuffer<?> set(IBuffer value) {
        return super.set((T) value);
    }

    @Override
    public void destroy() {

    }
}
