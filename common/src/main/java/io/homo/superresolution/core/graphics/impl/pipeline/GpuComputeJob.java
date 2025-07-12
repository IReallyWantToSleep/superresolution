package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformType;
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
            String resourceName = resourceEntry.getKey();
            PipelineJobResource<?> resource = resourceEntry.getValue();
            if (!program.getDescription().shaderUniforms().containsKey(resourceName)) {
                continue;
            }
            ShaderUniformType expectedType = program.uniforms().get(resourceName).type();
            switch (expectedType) {
                case SamplerTexture -> {
                    if (resource.type != PipelineResourceType.SamplerTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 SamplerTexture，实际 " + resource.type);
                    }
                    program.uniforms().samplerTexture(resourceName)
                            .setTexture((ITexture) resource.getResource());
                }
                case StorageTexture -> {
                    if (resource.type != PipelineResourceType.StorageTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 StorageTexture，实际 " + resource.type);
                    }
                    program.uniforms().storageTexture(resourceName)
                            .setTexture((ITexture) resource.getResource());
                }
                case UniformBuffer -> {
                    if (resource.type != PipelineResourceType.UniformBuffer) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 Buffer，实际 " + resource.type);
                    }
                    program.uniforms().uniformBuffer(resourceName)
                            .setBuffer((IBuffer) resource.getResource());
                }
                default -> throw new UnsupportedOperationException(
                        "不支持的uniform类型: " + expectedType);
            }
        }
    }
}
