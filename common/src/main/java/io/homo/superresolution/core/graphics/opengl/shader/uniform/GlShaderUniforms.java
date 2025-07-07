package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.*;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Destroyable;

import java.util.HashMap;
import java.util.Map;

public class GlShaderUniforms extends ShaderUniforms<
        GlShaderUniforms,
        GlShaderProgram,
        GlShaderUniformBuffer,
        GlShaderUniformSamplerTexture,
        GlShaderUniformStorageTexture> {
    private final Map<String, GlShaderBaseUniform<?, ?>> uniformMap = new HashMap<>();

    public GlShaderUniforms(GlShaderProgram program, ShaderDescription description) {
        super(program, description);
        description.shaderUniforms().values().forEach((uniformDescription) -> {
            uniformMap.put(uniformDescription.name(), switch (uniformDescription.type()) {
                case Buffer ->
                        new GlShaderUniformBuffer(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
                case SamplerTexture ->
                        new GlShaderUniformSamplerTexture(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
                case StorageTexture ->
                        new GlShaderUniformStorageTexture(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
            });
        });
    }

    @Override
    public GlShaderUniformBuffer buffer(String name) {
        return (GlShaderUniformBuffer) uniformMap.get(name);
    }

    @Override
    public GlShaderUniformSamplerTexture samplerTexture(String name) {
        return (GlShaderUniformSamplerTexture) uniformMap.get(name);
    }

    @Override
    public GlShaderUniformStorageTexture storageTexture(String name) {
        return (GlShaderUniformStorageTexture) uniformMap.get(name);
    }

    public Map<String, GlShaderBaseUniform<?, ?>> getUniformMap() {
        return uniformMap;
    }

    @Override
    public void destroy() {
        uniformMap.values().forEach(Destroyable::destroy);
    }

}