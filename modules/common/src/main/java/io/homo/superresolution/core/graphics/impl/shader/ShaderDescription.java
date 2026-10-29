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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShaderDescription {
    protected final EnumMap<ShaderType, ShaderSource> sourceMap = new EnumMap<>(ShaderType.class);
    protected final Map<String, String> definesMap = new HashMap<>();
    protected final ShaderResourcesLayout resourcesLayout = new ShaderResourcesLayout();
    protected String shaderName = UUID.randomUUID().toString();

    public static ShaderDescription.Builder graphics(
            ShaderSource fragment,
            ShaderSource vertex
    ) {
        return new Builder().fragment(fragment).vertex(vertex);
    }

    public static ShaderDescription.Builder compute(
            ShaderSource compute
    ) {
        return new Builder().compute(compute);
    }

    public static ShaderDescription.Builder create() {
        return new Builder();
    }

    public EnumMap<ShaderType, ShaderSource> sourceMap() {
        return sourceMap;
    }

    public Map<String, String> definesMap() {
        return definesMap;
    }

    public Map<String, ShaderResourceDescription> shaderUniforms() {
        return resourcesLayout.getResources();
    }

    public ShaderResourcesLayout resourcesLayout() {
        return resourcesLayout;
    }

    public String shaderName() {
        return shaderName;
    }

    public ShaderSource fragment() {
        return sourceMap.get(ShaderType.Fragment);
    }

    public ShaderSource vertex() {
        return sourceMap.get(ShaderType.Vertex);
    }

    public ShaderSource compute() {
        return sourceMap.get(ShaderType.Compute);
    }

    protected void updateShaderSource() {
        sourceMap.values().forEach((source -> source.addDefines(definesMap)));
        sourceMap.values().forEach(ShaderSource::updateSource);
    }

    public static class Builder {
        private final ShaderDescription description = new ShaderDescription();

        protected Builder() {

        }

        public Builder uniformBuffer(String name, int binding, int bufferSize) {
            description.resourcesLayout.addUniformBuffer(name, binding, bufferSize);
            return this;
        }

        public Builder uniformSamplerTexture(String name, int binding) {
            description.resourcesLayout.addSamplerTexture(name, binding);
            return this;
        }

        public Builder uniformStorageTexture(String name, ShaderResourceAccess access, int binding) {
            description.resourcesLayout.addStorageTexture(name, binding, access);
            return this;
        }

        public Builder uniformStorageTexture(String name, int binding) {
            description.resourcesLayout.addStorageTexture(name, binding);
            return this;
        }

        public ShaderSource fragmentSource() {
            return description.fragment();
        }

        public ShaderSource vertexSource() {
            return description.vertex();
        }

        public ShaderSource computeSource() {
            return description.compute();
        }

        public Builder fragment(ShaderSource source) {
            if (source.getType() != ShaderType.Fragment) {
                throw new RuntimeException();
            }
            description.sourceMap.put(ShaderType.Fragment, source);
            return this;
        }

        public Builder vertex(ShaderSource source) {
            if (source.getType() != ShaderType.Vertex) {
                throw new RuntimeException();
            }

            description.sourceMap.put(ShaderType.Vertex, source);
            return this;
        }

        public Builder compute(ShaderSource source) {
            if (source.getType() != ShaderType.Compute) {
                throw new RuntimeException();
            }

            description.sourceMap.put(ShaderType.Compute, source);
            return this;
        }

        public Builder addDefine(String key, String value) {
            description.definesMap.put(key, value);
            return this;
        }

        public Builder addDefines(Map<String, String> map) {
            description.definesMap.putAll(map);
            return this;
        }

        public Builder uniform(ShaderResourceDescription uniform) {
            description.resourcesLayout.addResource(uniform);
            return this;
        }

        public Builder name(String name) {
            description.shaderName = name;
            return this;
        }

        public ShaderDescription build() {
            description.updateShaderSource();
            return description;
        }
    }
}
