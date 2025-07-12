package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public interface IShaderUniformSamplerTexture<SELF extends IShaderUniform> extends IShaderUniform<ITexture, SELF> {
    SELF setTexture(ITexture texture);

    ITexture texture();

    default ShaderUniformType type() {
        return ShaderUniformType.SamplerTexture;
    }
}