package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;

public interface IShaderUniformBuffer<B extends IBuffer, SELF extends IShaderUniform> extends IShaderUniform<B, SELF> {
    SELF setBuffer(B buffer);

    B buffer();

    default ShaderUniformType type() {
        return ShaderUniformType.UniformBuffer;
    }
}