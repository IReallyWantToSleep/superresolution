package io.homo.superresolution.core.graphics.impl.shader;

import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShaderDescription {
    protected final EnumMap<ShaderType, ShaderSource> sourceMap = new EnumMap<>(ShaderType.class);
    protected final Map<String, String> definesMap = new HashMap<>();
    protected final Map<String, ShaderUniformDescription> shaderUniforms = new HashMap<>();
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

    public Map<String, ShaderUniformDescription> shaderUniforms() {
        return shaderUniforms;
    }

    public String shaderName() {
        return shaderName;
    }

    public ShaderSource fragment() {
        return sourceMap.get(ShaderType.FRAGMENT);
    }

    public ShaderSource vertex() {
        return sourceMap.get(ShaderType.VERTEX);
    }

    public ShaderSource compute() {
        return sourceMap.get(ShaderType.COMPUTE);
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
            return uniform(ShaderUniformDescription.builder(name, ShaderUniformType.UniformBuffer)
                    .binding(binding)
                    .bufferSize(bufferSize)
                    .build());
        }

        public Builder uniformSamplerTexture(String name, int binding) {
            return uniform(ShaderUniformDescription.builder(name, ShaderUniformType.SamplerTexture)
                    .binding(binding)
                    .build());
        }

        public Builder uniformStorageTexture(String name, ShaderUniformAccess access, int binding) {
            return uniform(ShaderUniformDescription.builder(name, ShaderUniformType.StorageTexture)
                    .binding(binding)
                    .access(access)
                    .build());
        }

        public Builder uniformStorageTexture(String name, int binding) {
            return uniform(ShaderUniformDescription.builder(name, ShaderUniformType.StorageTexture)
                    .binding(binding)
                    .access(ShaderUniformAccess.Both)
                    .build());
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
            if (source.getType() != ShaderType.FRAGMENT) throw new RuntimeException();
            description.sourceMap.put(ShaderType.FRAGMENT, source);
            return this;
        }

        public Builder vertex(ShaderSource source) {
            if (source.getType() != ShaderType.VERTEX) throw new RuntimeException();

            description.sourceMap.put(ShaderType.VERTEX, source);
            return this;
        }

        public Builder compute(ShaderSource source) {
            if (source.getType() != ShaderType.COMPUTE) throw new RuntimeException();

            description.sourceMap.put(ShaderType.COMPUTE, source);
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

        public Builder uniform(ShaderUniformDescription uniform) {
            description.shaderUniforms.put(uniform.name(), uniform);
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
