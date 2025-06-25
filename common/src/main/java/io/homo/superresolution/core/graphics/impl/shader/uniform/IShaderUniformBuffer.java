package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;

public interface IShaderUniformBuffer<B extends IBufferData, SELF extends IShaderUniform> extends IShaderUniform<B, SELF> {
    SELF setBuffer(B buffer);

    B buffer();
}