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

package io.homo.superresolution.shadercompat;

import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

class GlTextureInfoGetter {
    public static int getInternalFormat(int target, int name) {
        int prevTex = glGetInteger(target == GL11.GL_TEXTURE_2D ? GL_TEXTURE_BINDING_2D : GL_TEXTURE_BINDING_1D);
        glBindTexture(target, name);
        int[] params = new int[1];
        //GL42.glGetInternalformativ()
        GL11.glGetTexLevelParameteriv(target, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT, params);
        glBindTexture(target, prevTex);
        return params[0];
    }

    public static int getWidth(int target, int name) {
        int prevTex = glGetInteger(target == GL11.GL_TEXTURE_2D ? GL_TEXTURE_BINDING_2D : GL_TEXTURE_BINDING_1D);
        glBindTexture(target, name);
        int[] params = new int[1];
        GL11.glGetTexLevelParameteriv(target, 0, GL11.GL_TEXTURE_WIDTH, params);
        glBindTexture(target, prevTex);
        return params[0];
    }

    public static int getHeight(int target, int name) {
        int prevTex = glGetInteger(target == GL11.GL_TEXTURE_2D ? GL_TEXTURE_BINDING_2D : GL_TEXTURE_BINDING_1D);
        glBindTexture(target, name);
        int[] params = new int[1];
        GL11.glGetTexLevelParameteriv(target, 0, GL11.GL_TEXTURE_HEIGHT, params);
        glBindTexture(target, prevTex);
        return params[0];
    }
}
