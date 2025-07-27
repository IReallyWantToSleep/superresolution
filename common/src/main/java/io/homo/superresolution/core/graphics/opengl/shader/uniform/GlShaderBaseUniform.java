package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniform;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;

public abstract class GlShaderBaseUniform<T, SELF extends IShaderUniform<?, ?>> implements IShaderUniform<T, SELF> {
    private final String name;
    private final int binding;
    protected T current;
    protected ShaderUniformAccess access;

    public GlShaderBaseUniform(
            String name,
            int binding,
            ShaderUniformAccess access
    ) {
        this.name = name;
        this.binding = binding;
        this.access = access;
    }

    @Override
    public ShaderUniformAccess access() {
        return access;
    }

    @Override
    public SELF set(T value) {
        this.current = value;
        return (SELF) this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int binding() {
        return binding;
    }
}
