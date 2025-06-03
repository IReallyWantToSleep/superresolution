package io.homo.superresolution.core.graphics.impl.shader.uniform;

public interface IShaderUniformBlock<B extends ShaderUniformBuffer, SELF extends IShaderUniform> extends IShaderUniform<B, SELF> {
    SELF setBuffer(B buffer);

    B buffer();
}