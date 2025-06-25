package io.homo.superresolution.core.graphics.impl.shader;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniforms;

public interface IShaderProgram<UNIFORM extends ShaderUniforms> extends GpuObject {
    void compile();

    boolean isCompiled();

    void destroy();

    ShaderDescription getDescription();

    UNIFORM uniforms();
}
