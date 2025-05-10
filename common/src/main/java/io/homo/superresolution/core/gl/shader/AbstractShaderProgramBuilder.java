package io.homo.superresolution.core.gl.shader;

import io.homo.superresolution.core.impl.shader.ShaderSource;

import java.util.*;

public abstract class AbstractShaderProgramBuilder<T extends AbstractGlShaderProgram> {
    private final Map<String, String> shaderDefineList = new HashMap<>();
    private final Map<ShaderSource.Type, ShaderSource> shaderSources = new EnumMap<>(ShaderSource.Type.class);
    private String shaderName = "";

    public AbstractShaderProgramBuilder() {
    }

    public AbstractShaderProgramBuilder<T> addShaderSource(ShaderSource source) {
        shaderSources.put(source.getType(), source);
        return this;
    }

    public AbstractShaderProgramBuilder<T> setShaderName(String shaderName) {
        this.shaderName = shaderName;
        return this;
    }

    public AbstractShaderProgramBuilder<T> addDefineText(String name, String value) {
        this.shaderDefineList.put(name, value);
        return this;
    }

    public abstract T build();

    @SuppressWarnings("unchecked")
    protected T updateShader(AbstractGlShaderProgram shader) {
        shaderSources.forEach((key, shaderSource) -> shaderSource.setShaderDefines(new HashMap<>(shaderDefineList)));
        shader.shaderDefineList = this.shaderDefineList;
        shader.shaderName = this.shaderName;
        shader.shaderSources.putAll(shaderSources);
        return (T) shader;
    }
}
