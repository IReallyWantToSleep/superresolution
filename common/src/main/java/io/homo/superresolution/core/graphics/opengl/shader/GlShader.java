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

package io.homo.superresolution.core.graphics.opengl.shader;

import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.opengl.GL45;

public class GlShader implements Destroyable {
    private int id;

    public GlShader(ShaderType type) {
        this.id = GL45.glCreateShader(switch (type) {
            case VERTEX -> GL45.GL_VERTEX_SHADER;
            case COMPUTE -> GL45.GL_COMPUTE_SHADER;
            case FRAGMENT -> GL45.GL_FRAGMENT_SHADER;
        });
    }

    public int id() {
        return id;
    }

    @Override
    public void destroy() {
        GL45.glDeleteShader(this.id);
        this.id = -1;
    }
}
