package io.homo.superresolution.core.gl.dsa;

import org.lwjgl.opengl.GL45;

public class GL45DirectStateAccessImpl implements IGlDirectStateAccess {
    @Override
    public void blitFramebuffer(int readFramebuffer, int drawFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL45.glBlitNamedFramebuffer(
                readFramebuffer,
                drawFramebuffer,
                srcX0,
                srcY0,
                srcX1,
                srcY1,
                dstX0,
                dstY0,
                dstX1,
                dstY1,
                mask,
                filter
        );
    }

    @Override
    public void clearFramebuffer(int framebuffer, int buffer, int drawbuffer, float[] value) {
        GL45.glClearNamedFramebufferfv(
                framebuffer,
                buffer,
                drawbuffer,
                value
        );
    }

    @Override
    public void copyTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL45.glCopyTextureSubImage2D(
                texture,
                level,
                xoffset,
                yoffset,
                x,
                y,
                width,
                height
        );
    }

    @Override
    public void copyTextureSubImage1D(int texture, int level, int xoffset, int x, int y, int width) {
        GL45.glCopyTextureSubImage1D(
                texture,
                level,
                xoffset,
                x,
                y,
                width
        );
    }

    @Override
    public int createTexture2D() {
        return GL45.glCreateTextures(GL45.GL_TEXTURE_2D);
    }

    @Override
    public int createTexture1D() {
        return GL45.glCreateTextures(GL45.GL_TEXTURE_1D);
    }

    @Override
    public void textureParameteri(int texture, int pname, int value) {
        GL45.glTextureParameteri(texture, pname, value);
    }

    @Override
    public void textureStorage2D(int texture, int levels, int internalFormat, int width, int height) {
        GL45.glTextureStorage2D(texture, levels, internalFormat, width, height);
    }

    @Override
    public void textureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
        GL45.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void textureStorage1D(int texture, int levels, int internalFormat, int width) {
        GL45.glTextureStorage1D(texture, levels, internalFormat, width);
    }

    @Override
    public void textureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long pixels) {
        GL45.glTextureSubImage1D(texture, level, xoffset, width, format, type, pixels);
    }

    @Override
    public int createVertexArray() {
        return GL45.glCreateVertexArrays();
    }

    @Override
    public void vertexArrayVertexBuffer(int vao, int bindingIndex, int buffer, long offset, int stride) {
        GL45.glVertexArrayVertexBuffer(vao, bindingIndex, buffer, offset, stride);
    }

    @Override
    public void enableVertexArrayAttrib(int vao, int index) {
        GL45.glEnableVertexArrayAttrib(vao, index);
    }

    @Override
    public void vertexArrayAttribFormat(int vao, int attribIndex, int size, int type, boolean normalized, int relativeOffset) {
        GL45.glVertexArrayAttribFormat(vao, attribIndex, size, type, normalized, relativeOffset);
    }

    @Override
    public void vertexArrayAttribBinding(int vao, int attribIndex, int bindingIndex) {
        GL45.glVertexArrayAttribBinding(vao, attribIndex, bindingIndex);
    }

    @Override
    public int createFramebuffer() {
        return GL45.glCreateFramebuffers();
    }

    @Override
    public void framebufferTexture(int framebuffer, int attachment, int texture, int level) {
        GL45.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
    }

    @Override
    public void programUniform1i(int program, int location, int value) {
        GL45.glProgramUniform1i(program, location, value);
    }

    @Override
    public void programUniform1f(int program, int location, float value) {
        GL45.glProgramUniform1f(program, location, value);
    }

    @Override
    public void programUniform2f(int program, int location, float x, float y) {
        GL45.glProgramUniform2f(program, location, x, y);
    }

    @Override
    public void programUniform3f(int program, int location, float x, float y, float z) {
        GL45.glProgramUniform3f(program, location, x, y, z);
    }

    @Override
    public void programUniform4f(int program, int location, float x, float y, float z, float w) {
        GL45.glProgramUniform4f(program, location, x, y, z, w);
    }

    @Override
    public void programUniform1iv(int program, int location, int[] values) {
        GL45.glProgramUniform1iv(program, location, values);
    }

    @Override
    public void programUniform1fv(int program, int location, float[] values) {
        GL45.glProgramUniform1fv(program, location, values);
    }

    @Override
    public void programUniformMatrix2fv(int program, int location, boolean transpose, float[] matrix) {
        GL45.glProgramUniformMatrix2fv(program, location, transpose, matrix);
    }

    @Override
    public void programUniformMatrix3fv(int program, int location, boolean transpose, float[] matrix) {
        GL45.glProgramUniformMatrix3fv(program, location, transpose, matrix);
    }

    @Override
    public void programUniformMatrix4fv(int program, int location, boolean transpose, float[] matrix) {
        GL45.glProgramUniformMatrix4fv(program, location, transpose, matrix);
    }

    @Override
    public void programUniform1b(int program, int location, boolean value) {
        GL45.glProgramUniform1i(program, location, value ? 1 : 0);
    }

    @Override
    public void deleteTexture(int texture) {
        GL45.glDeleteTextures(texture);
    }

    @Override
    public void deleteVertexArray(int vao) {
        GL45.glDeleteVertexArrays(vao);
    }

    @Override
    public void deleteFramebuffer(int fbo) {
        GL45.glDeleteFramebuffers(fbo);
    }
}