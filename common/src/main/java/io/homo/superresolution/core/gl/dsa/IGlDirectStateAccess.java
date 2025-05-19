package io.homo.superresolution.core.gl.dsa;

import org.lwjgl.opengl.GL45C;

public interface IGlDirectStateAccess {
    /// 纹理
    int createTexture2D();

    int createTexture1D();

    void textureParameteri(int texture, int pname, int value);

    void textureStorage2D(int texture, int levels, int internalFormat, int width, int height);

    void textureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels);

    void textureStorage1D(int texture, int levels, int internalFormat, int width);

    void textureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long pixels);

    /// 顶点
    int createVertexArray();

    void vertexArrayVertexBuffer(int vao, int bindingIndex, int buffer, long offset, int stride);

    void enableVertexArrayAttrib(int vao, int index);

    void vertexArrayAttribFormat(int vao, int attribIndex, int size, int type, boolean normalized, int relativeOffset);

    void vertexArrayAttribBinding(int vao, int attribIndex, int bindingIndex);

    /// 帧缓冲区
    int createFramebuffer();

    void framebufferTexture(int framebuffer, int attachment, int texture, int level);


    /// Uniform部分
    void programUniform1i(int program, int location, int value);

    void programUniform1f(int program, int location, float value);

    void programUniform2f(int program, int location, float x, float y);

    void programUniform3f(int program, int location, float x, float y, float z);

    void programUniform4f(int program, int location, float x, float y, float z, float w);

    void programUniform1iv(int program, int location, int[] values);

    void programUniform1fv(int program, int location, float[] values);

    void programUniformMatrix2fv(int program, int location, boolean transpose, float[] matrix);

    void programUniformMatrix3fv(int program, int location, boolean transpose, float[] matrix);

    void programUniformMatrix4fv(int program, int location, boolean transpose, float[] matrix);

    default void programUniform1b(int program, int location, boolean value) {
        programUniform1i(program, location, value ? GL45C.GL_TRUE : GL45C.GL_FALSE);
    }

    /// 销毁部分
    void deleteTexture(int texture);

    void deleteVertexArray(int vao);

    void deleteFramebuffer(int fbo);

    void blitFramebuffer(int readFramebuffer, int drawFramebuffer,
                         int srcX0, int srcY0, int srcX1, int srcY1,
                         int dstX0, int dstY0, int dstX1, int dstY1,
                         int mask, int filter);

    void clearFramebuffer(int framebuffer, int buffer,
                          int drawbuffer, float[] value);

    void copyTextureSubImage2D(int texture, int level,
                               int xoffset, int yoffset,
                               int x, int y, int width, int height);

    void copyTextureSubImage1D(int texture, int level,
                               int xoffset,
                               int x, int y, int width);
}