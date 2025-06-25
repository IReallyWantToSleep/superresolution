package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class GlShaderUniformStorageTexture extends GlShaderBaseUniform<ITexture, GlShaderUniformStorageTexture> implements IShaderUniformStorageTexture<GlShaderUniformStorageTexture> {
    public GlShaderUniformStorageTexture(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
    }

    @Override
    public GlShaderUniformStorageTexture set(ITexture value) {
        return super.set(value);
    }

    @Override
    public GlShaderUniformStorageTexture setTexture(ITexture texture) {
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
