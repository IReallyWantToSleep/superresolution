package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.impl.Destroyable;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public abstract class ShaderUniforms<
        SELF extends ShaderUniforms<?, ?, ?, ?, ?>,
        A extends IShaderProgram<SELF>,
        B extends IShaderUniformBlock<?, ?>,
        C extends IShaderUniformSamplerTexture<?>,
        D extends IShaderUniformStorageTexture<?>
        > implements Destroyable, AutoCloseable {
    protected final Map<String, ShaderUniformDescription> shaderUniforms;
    protected final A program;
    protected final ShaderDescription description;

    public ShaderUniforms(A program, ShaderDescription description) {
        this.program = program;
        this.description = description;
        this.shaderUniforms = new HashMap<>(this.description.shaderUniforms());
    }

    public Map<String, ShaderUniformDescription> getShaderUniforms() {
        return shaderUniforms;
    }

    public A getProgram() {
        return program;
    }

    public ShaderDescription getDescription() {
        return description;
    }

    public abstract B block(String name);

    public abstract C samplerTexture(String name);

    public abstract D storageTexture(String name);

    public abstract void close();
}
