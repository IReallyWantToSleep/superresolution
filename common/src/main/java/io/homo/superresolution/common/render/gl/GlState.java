package io.homo.superresolution.common.render.gl;

import static org.lwjgl.opengl.GL32.*;

public class GlState implements AutoCloseable {
    private static final int MAX_TEXTURES = 32;

    public int program;
    public int vao;
    public int vbo;
    public int ebo;
    public int fbo;
    public int texture2D;
    public int activeTextureNumber;
    public int[] textures;
    public int[] view;

    public GlState() {
        this.saveState();
    }

    public void saveState() {
        this.program = glGetInteger(GL_CURRENT_PROGRAM);
        this.vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        this.vbo = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        this.ebo = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        this.fbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        this.texture2D = glGetInteger(GL_TEXTURE_BINDING_2D);
        this.activeTextureNumber = glGetInteger(GL_ACTIVE_TEXTURE);
        int textureIndex = 0;
        this.textures = new int[MAX_TEXTURES];
        while (textureIndex < MAX_TEXTURES) {
            glActiveTexture(GL_TEXTURE0 + textureIndex);
            this.textures[textureIndex] = glGetInteger(GL_TEXTURE_BINDING_2D);
            textureIndex++;
        }
        this.view = new int[4];
        glGetIntegerv(GL_VIEWPORT, this.view);
    }

    public void restore() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (this.fbo != 0 && glIsFramebuffer(this.fbo)) {
            glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        }
        int textureIndex = 0;
        while (textureIndex < MAX_TEXTURES) {
            glActiveTexture(GL_TEXTURE0 + textureIndex);
            glBindTexture(GL_TEXTURE_2D, glIsTexture(this.textures[textureIndex]) ? this.textures[textureIndex] : 0);
            textureIndex++;
        }
        glActiveTexture(this.activeTextureNumber);
        glBindTexture(GL_TEXTURE_2D, glIsTexture(this.texture2D) ? this.texture2D : 0);
        glBindVertexArray(glIsVertexArray(this.vao) ? this.vao : 0);
        glBindBuffer(GL_ARRAY_BUFFER, glIsBuffer(this.vbo) ? this.vbo : 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glIsBuffer(this.ebo) ? this.ebo : 0);
        glUseProgram(glIsProgram(this.program) ? this.program : 0);
        glViewport(this.view[0], this.view[1], this.view[2], this.view[3]);
    }

    @Override
    public void close() {
        restore();
    }
}