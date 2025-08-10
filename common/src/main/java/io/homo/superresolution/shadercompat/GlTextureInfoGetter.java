package io.homo.superresolution.shadercompat;

import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

class GlTextureInfoGetter {
    public static int getInternalFormat(int target, int name) {
        int prevTex = glGetInteger(target == GL11.GL_TEXTURE_2D ? GL_TEXTURE_BINDING_2D : GL_TEXTURE_BINDING_1D);
        glBindTexture(target, name);
        int[] params = new int[1];
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
