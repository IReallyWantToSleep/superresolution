/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.impl.grape;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.IPipeline;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class GpuComputeJob<SELF extends GpuComputeJob<?>> {
    protected Map<String, GrapeJobResource<?>> resources = new HashMap<>();

    public SELF resource(String key, GrapeJobResource<?> resource) {
        resources.put(key, resource);
        return (SELF) this;
    }

    public GrapeJobResource<?> resource(String key) {
        return resources.get(key);
    }

    protected void setupProgramResources(IPipeline pipeline) {
        for (var resourceEntry : resources.entrySet()) {
            String resourceName = resourceEntry.getKey();
            GrapeJobResource<?> jobResource = resourceEntry.getValue();
            if (!pipeline.shader().getDescription().shaderUniforms().containsKey(resourceName)) {
                continue;
            }
            ShaderResourceDescription description = pipeline.shader().getDescription().resourcesLayout().getResource(resourceName);
            ShaderResourceType expectedType = description.type();
            Optional<?> resource = jobResource.getResource();
            if (resource.isEmpty()) continue;
            switch (expectedType) {
                case SamplerTexture -> {
                    if (jobResource.type != GrapeResourceType.SamplerTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 SamplerTexture，实际 " + jobResource.type);
                    }
                    pipeline.descriptorSet().samplerTexture(
                            resourceName,
                            description.binding(),
                            (ITexture) resource.get()
                    );
                }
                case StorageTexture -> {
                    if (jobResource.type != GrapeResourceType.StorageTexture) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 StorageTexture，实际 " + jobResource.type);
                    }
                    pipeline.descriptorSet().storageImage(
                            resourceName,
                            description.binding(),
                            (ITexture) resource.get()
                    );
                }
                case UniformBuffer -> {
                    if (jobResource.type != GrapeResourceType.UniformBuffer) {
                        throw new IllegalArgumentException("资源类型不匹配: " + resourceName +
                                " 期望 Buffer，实际 " + jobResource.type);
                    }
                    pipeline.descriptorSet().uniformBuffer(
                            resourceName,
                            description.binding(),
                            (IBuffer) resource.get()
                    );
                }
                default -> throw new UnsupportedOperationException(
                        "不支持的uniform类型: " + expectedType);
            }
        }
        pipeline.descriptorSet().update();
    }
}
