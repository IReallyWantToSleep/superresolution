package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.Gl;

import static io.homo.superresolution.core.graphics.opengl.Gl.glBindTextureUnit;

public class ShaderUniformSamplerTexture extends ShaderBaseUniform<ITexture, ShaderUniformSamplerTexture> implements IShaderUniformSamplerTexture<ShaderUniformSamplerTexture> {
    public ShaderUniformSamplerTexture(String name, int binding) {
        super(name, binding);
    }

    @Override
    public ShaderUniformSamplerTexture set(ITexture value) {
        glBindTextureUnit(binding(), value.getTextureId());
        return super.set(value);
    }

    @Override
    public ShaderUniformSamplerTexture setTexture(ITexture texture) {
        return set(texture);
    }

    @Override
    public ITexture texture() {
        return current;
    }

    @Override
    public void destroy() {

    }
}
