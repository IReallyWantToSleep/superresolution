package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;

public interface IShaderUniform<T, SELF extends IShaderUniform> extends Destroyable {
    SELF set(T value);

    String name();

    int binding();

    ShaderUniformAccess access();

    ShaderUniformType type();
}
