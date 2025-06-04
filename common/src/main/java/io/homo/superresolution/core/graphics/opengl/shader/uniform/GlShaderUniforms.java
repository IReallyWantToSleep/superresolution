package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.*;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Destroyable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GlShaderUniforms extends ShaderUniforms<
        GlShaderUniforms,
        GlShaderProgram,
        ShaderUniformBlock<?>,
        ShaderUniformSamplerTexture,
        ShaderUniformStorageTexture> implements AutoCloseable {
    private final Map<String, ShaderBaseUniform<?, ?>> uniformMap = new HashMap<>();
    private final GlState glState;

    public GlShaderUniforms(GlShaderProgram program, ShaderDescription description) {
        super(program, description);
        glState = new GlState();
        description.shaderUniforms().values().forEach((uniformDescription) -> {
            uniformMap.put(uniformDescription.name(), switch (uniformDescription.type()) {
                case Block -> new ShaderUniformBlock<>(uniformDescription.name(), uniformDescription.binding());
                case SamplerTexture ->
                        new ShaderUniformSamplerTexture(uniformDescription.name(), uniformDescription.binding());
                case StorageTexture ->
                        new ShaderUniformStorageTexture(uniformDescription.name(), uniformDescription.binding());
            });
        });
    }

    @Override
    public ShaderUniformBlock<?> block(String name) {
        return (ShaderUniformBlock<?>) uniformMap.get(name);
    }

    @Override
    public ShaderUniformSamplerTexture samplerTexture(String name) {
        return (ShaderUniformSamplerTexture) uniformMap.get(name);
    }

    @Override
    public ShaderUniformStorageTexture storageTexture(String name) {
        return (ShaderUniformStorageTexture) uniformMap.get(name);
    }

    @Override
    public void destroy() {
        uniformMap.values().forEach(Destroyable::destroy);
    }

    @Override
    public void close() {
        glState.close();
    }
}