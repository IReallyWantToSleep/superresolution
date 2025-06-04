package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniform;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformBlock;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformBuffer;
import io.homo.superresolution.core.graphics.opengl.OpenGLException;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;

public abstract class ShaderBaseUniform<T, SELF extends IShaderUniform<?, ?>> implements IShaderUniform<T, SELF> {
    private final String name;
    private final int binding;
    protected T current;
    protected GlShaderProgram program;

    public ShaderBaseUniform(
            String name,
            int binding
    ) {
        this.name = name;
        this.binding = binding;
    }

    public void bindProgram(GlShaderProgram program) {
        this.program = program;
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

    protected void check() {
        if (!RenderSystems.opengl().getShaderProgram().equals(program)) {
            throw new OpenGLException("正在设置着色器 %s 的Uniform数据，但当前着色器为 %s".formatted(RenderSystems.opengl().getShaderProgram(), program));
        }
    }
}
