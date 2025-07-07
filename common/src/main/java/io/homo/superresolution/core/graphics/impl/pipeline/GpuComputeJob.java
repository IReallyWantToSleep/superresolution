package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.HashMap;
import java.util.Map;

public abstract class GpuComputeJob<SELF extends GpuComputeJob<?>> {
    protected Map<String, PipelineJobResource<?>> resources = new HashMap<>();

    public SELF resource(String key, PipelineJobResource<?> resource) {
        resources.put(key, resource);
        return (SELF) this;
    }

    public PipelineJobResource<?> resource(String key) {
        return resources.get(key);
    }

    protected void setupProgramResources(IShaderProgram<?> program) {
        for (var resourceEntry : resources.entrySet()) {
            if (program.getDescription().shaderUniforms().containsKey(resourceEntry.getKey())) {
                switch (resourceEntry.getValue().type) {
                    case Image -> program.uniforms().storageTexture(
                            resourceEntry.getKey()
                    ).setTexture((ITexture) resourceEntry.getValue().getResource());
                    case Texture -> program.uniforms().samplerTexture(
                            resourceEntry.getKey()
                    ).setTexture((ITexture) resourceEntry.getValue().getResource());
                    case UniformBuffer -> program.uniforms().buffer(
                            resourceEntry.getKey()
                    ).setBuffer((IBuffer) resourceEntry.getValue().getResource());
                }
            }
        }
    }
}
