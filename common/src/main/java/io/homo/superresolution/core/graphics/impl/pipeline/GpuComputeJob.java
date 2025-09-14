/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            PipelineJobResource<?> jobResource = resourceEntry.getValue();
            if (!program.getDescription().shaderUniforms().containsKey(resourceName)) {
                continue;
            }
            ShaderUniformType expectedType = program.uniforms().get(resourceName).type();
            Optional<?> resource = jobResource.getResource();
            if (resource.isEmpty()) continue;
            switch (expectedType) {
                case SamplerTexture -> {
                    if (jobResource.type != PipelineResourceType.SamplerTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 SamplerTexture，实际 " + jobResource.type);
                    }
                    program.uniforms().samplerTexture(resourceName)
                            .setTexture((ITexture) resource.get());
                }
                case StorageTexture -> {
                    if (jobResource.type != PipelineResourceType.StorageTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 StorageTexture，实际 " + jobResource.type);
                    }
                    program.uniforms().storageTexture(resourceName)
                            .setTexture((ITexture) resource.get());
                }
                case UniformBuffer -> {
                    if (jobResource.type != PipelineResourceType.UniformBuffer) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 Buffer，实际 " + jobResource.type);
                    }
                    program.uniforms().uniformBuffer(resourceName)
                            .setBuffer((IBuffer) resource.get());
                }
                default -> throw new UnsupportedOperationException(
                        "不支持的uniform类型: " + expectedType);
            }
        }
    }
}
