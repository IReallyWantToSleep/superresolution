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

package io.homo.superresolution.core.graphics.opengl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniform;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniforms;
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
                case UniformBuffer ->
                        new GlShaderUniformBuffer(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
                case SamplerTexture ->
                        new GlShaderUniformSamplerTexture(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
                case StorageTexture ->
                        new GlShaderUniformStorageTexture(uniformDescription.name(), uniformDescription.binding(), uniformDescription.access());
            });
        });
    }

    @Override
    public GlShaderUniformBuffer uniformBuffer(String name) {
        GlShaderBaseUniform<?, ?> uniform = uniformMap.get(name);
        if (!(uniform instanceof GlShaderUniformBuffer)) {
            throw new ClassCastException("Uniform " + name + " 不是UBO类型");
        }
        return (GlShaderUniformBuffer) uniform;
    }

    public GlShaderUniformSamplerTexture samplerTexture(String name) {
        GlShaderBaseUniform<?, ?> uniform = uniformMap.get(name);
        if (!(uniform instanceof GlShaderUniformSamplerTexture)) {
            throw new ClassCastException("Uniform " + name + " 不是采样纹理类型");
        }
        return (GlShaderUniformSamplerTexture) uniform;
    }

    public GlShaderUniformStorageTexture storageTexture(String name) {
        GlShaderBaseUniform<?, ?> uniform = uniformMap.get(name);
        if (!(uniform instanceof GlShaderUniformStorageTexture)) {
            throw new ClassCastException("Uniform " + name + " 不是存储纹理类型");
        }
        return (GlShaderUniformStorageTexture) uniform;
    }

    @Override
    public IShaderUniform<?, ?> get(String name) {
        return uniformMap.get(name);
    }

    public Map<String, GlShaderBaseUniform<?, ?>> getUniformMap() {
        return uniformMap;
    }

    @Override
    public void destroy() {
        uniformMap.values().forEach(Destroyable::destroy);
    }

}