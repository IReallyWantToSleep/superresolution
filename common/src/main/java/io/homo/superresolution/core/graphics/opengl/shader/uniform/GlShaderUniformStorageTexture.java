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

import io.homo.superresolution.core.graphics.impl.shader.uniform.IShaderUniformStorageTexture;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class GlShaderUniformStorageTexture extends GlShaderBaseUniform<ITexture, GlShaderUniformStorageTexture> implements IShaderUniformStorageTexture<GlShaderUniformStorageTexture> {
    public GlShaderUniformStorageTexture(String name, int binding, ShaderUniformAccess access) {
        super(name, binding, access);
    }

    @Override
    public GlShaderUniformStorageTexture set(ITexture value) {
        return super.set(value);
    }

    @Override
    public GlShaderUniformStorageTexture setTexture(ITexture texture) {
        return set(texture);
    }

    @Override
    public ITexture texture() {
        return current;
    }

    @Override
    public void destroy() {

    }
}
