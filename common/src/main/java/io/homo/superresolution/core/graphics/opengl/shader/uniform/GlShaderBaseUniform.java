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

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniform;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;

public abstract class GlShaderBaseUniform<T, SELF extends IShaderUniform<?, ?>> implements IShaderUniform<T, SELF> {
    private final String name;
    private final int binding;
    protected T current;
    protected ShaderUniformAccess access;

    public GlShaderBaseUniform(
            String name,
            int binding,
            ShaderUniformAccess access
    ) {
        this.name = name;
        this.binding = binding;
        this.access = access;
    }

    @Override
    public ShaderUniformAccess access() {
        return access;
    }

    @Override
    public SELF set(T value) {
        this.current = value;
        return (SELF) this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int binding() {
        return binding;
    }
}
