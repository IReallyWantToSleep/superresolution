package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformBuffer;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;

public class GlShaderUniformBuffer extends GlShaderBaseUniform<IBuffer, GlShaderUniformBuffer> implements IShaderUniformBuffer<IBuffer, GlShaderUniformBuffer> {
    public GlShaderUniformBuffer(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
    }

    @Override
    public GlShaderUniformBuffer setBuffer(IBuffer buffer) {
        return set(buffer);
    }

    @Override
    public IBuffer buffer() {
        return current;
    }

    @Override
    public GlShaderUniformBuffer set(IBuffer value) {
        return super.set((IBuffer) value);
    }

    @Override
    public void destroy() {

    }
}
