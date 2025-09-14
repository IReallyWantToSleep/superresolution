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

package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.impl.Destroyable;

import java.util.HashMap;
import java.util.Map;

public abstract class ShaderUniforms<
        SELF extends ShaderUniforms<?, ?, ?, ?, ?>,
        A extends IShaderProgram<SELF>,
        B extends IShaderUniformBuffer<?, ?>,
        C extends IShaderUniformSamplerTexture<?>,
        D extends IShaderUniformStorageTexture<?>
        > implements Destroyable {
    protected final Map<String, ShaderUniformDescription> shaderUniforms;
    protected final A program;
    protected final ShaderDescription description;

    public ShaderUniforms(A program, ShaderDescription description) {
        this.program = program;
        this.description = description;
        this.shaderUniforms = new HashMap<>(this.description.shaderUniforms());
    }

    public Map<String, ShaderUniformDescription> getShaderUniforms() {
        return shaderUniforms;
    }

    public A getProgram() {
        return program;
    }

    public ShaderDescription getDescription() {
        return description;
    }

    public abstract B uniformBuffer(String name);

    public abstract C samplerTexture(String name);

    public abstract D storageTexture(String name);

    public abstract IShaderUniform<?, ?> get(String name);
}
