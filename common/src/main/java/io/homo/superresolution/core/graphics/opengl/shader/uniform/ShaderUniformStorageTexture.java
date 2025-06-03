package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class ShaderUniformStorageTexture extends ShaderBaseUniform<ITexture, ShaderUniformStorageTexture> implements IShaderUniformStorageTexture<ShaderUniformStorageTexture> {
    public ShaderUniformStorageTexture(String name, int binding) {
        super(name, binding);
    }

    @Override
    public ShaderUniformStorageTexture set(ITexture value) {
        return super.set(value);
    }

    @Override
    public ShaderUniformStorageTexture setTexture(ITexture texture) {
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
