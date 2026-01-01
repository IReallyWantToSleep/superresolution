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

package io.homo.superresolution.core.graphics.impl.shader;

import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceType;

import java.util.HashMap;
import java.util.Map;

public class ShaderResourcesLayout {
    private final Map<String, ShaderResourceDescription> resources = new HashMap<>();

    public ShaderResourcesLayout() {
    }

    public ShaderResourcesLayout addUniformBuffer(String name, int binding, int bufferSize) {
        resources.put(name, ShaderResourceDescription.builder(name, ShaderResourceType.UniformBuffer)
                .binding(binding)
                .bufferSize(bufferSize)
                .build());
        return this;
    }


    public ShaderResourcesLayout addSamplerTexture(String name, int binding) {
        resources.put(name, ShaderResourceDescription.builder(name, ShaderResourceType.SamplerTexture)
                .binding(binding)
                .build());
        return this;
    }

    public ShaderResourcesLayout addStorageTexture(String name, int binding, ShaderResourceAccess access) {
        resources.put(name, ShaderResourceDescription.builder(name, ShaderResourceType.StorageTexture)
                .binding(binding)
                .access(access)
                .build());
        return this;
    }

    public ShaderResourcesLayout addStorageTexture(String name, int binding) {
        return addStorageTexture(name, binding, ShaderResourceAccess.Both);
    }

    public ShaderResourcesLayout addResource(ShaderResourceDescription resource) {
        resources.put(resource.name(), resource);
        return this;
    }

    public Map<String, ShaderResourceDescription> getResources() {
        return resources;
    }

    public ShaderResourceDescription getResource(String name) {
        return resources.get(name);
    }

    public boolean hasResource(String name) {
        return resources.containsKey(name);
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ShaderResourcesLayout layout = new ShaderResourcesLayout();

        public Builder uniformBuffer(String name, int binding, int bufferSize) {
            layout.addUniformBuffer(name, binding, bufferSize);
            return this;
        }

        public Builder samplerTexture(String name, int binding) {
            layout.addSamplerTexture(name, binding);
            return this;
        }

        public Builder storageTexture(String name, int binding, ShaderResourceAccess access) {
            layout.addStorageTexture(name, binding, access);
            return this;
        }

        public Builder storageTexture(String name, int binding) {
            layout.addStorageTexture(name, binding);
            return this;
        }

        public Builder resource(ShaderResourceDescription resource) {
            layout.addResource(resource);
            return this;
        }

        public ShaderResourcesLayout build() {
            return layout;
        }
    }
}
