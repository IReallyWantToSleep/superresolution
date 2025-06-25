package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformSamplerTexture;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class GlShaderUniformSamplerTexture extends GlShaderBaseUniform<ITexture, GlShaderUniformSamplerTexture> implements IShaderUniformSamplerTexture<GlShaderUniformSamplerTexture> {
    public GlShaderUniformSamplerTexture(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
    }

    @Override
    public GlShaderUniformSamplerTexture set(ITexture value) {
        return super.set(value);
    }

    @Override
    public GlShaderUniformSamplerTexture setTexture(ITexture texture) {
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
