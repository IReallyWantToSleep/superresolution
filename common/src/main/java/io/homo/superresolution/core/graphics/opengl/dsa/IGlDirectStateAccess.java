package io.homo.superresolution.core.graphics.opengl.dsa;

import java.nio.Buffer;

public interface IGlDirectStateAccess {
    /// 采样器
    int createSampler();

    void samplerParameteri(int sampler, int pname, int param);

    void deleteSampler(int sampler);

    /// 纹理
    int createTexture2D();

    int createTexture1D();

    void textureParameteri(int texture, int pname, int value);

    void textureParameterf(int texture, int pname, float value);

    void textureStorage2D(int texture, int levels, int internalFormat, int width, int height);

    void textureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels);

    void textureStorage1D(int texture, int levels, int internalFormat, int width);

    void textureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long pixels);

    int createTextureView(int srcTexture, int target, int internalFormat,
                          int minLevel, int numLevels, int minLayer, int numLayers);

    void generateTextureMipmap(int texture);


    /// 顶点
    int createVertexArray();

    void bindVertexArray(int vao);

    void vertexArrayVertexBuffer(int vao, int bindingIndex, int buffer, long offset, int stride);

    void enableVertexArrayAttrib(int vao, int index);

    void vertexArrayAttribFormat(int vao, int attribIndex, int size, int type, boolean normalized, int relativeOffset);

    void vertexArrayAttribBinding(int vao, int attribIndex, int bindingIndex);

    /// 帧缓冲区
    int createFramebuffer();

    void framebufferTexture(int framebuffer, int attachment, int texture, int level);

    int checkNamedFramebufferStatus(int framebuffer, int target);

    void clearNamedFramebufferfv(int framebuffer, int buffer, int drawbuffer, float[] value);

    void clearNamedFramebufferfi(int framebuffer, int buffer, int drawbuffer, float depth, int stencil);

    /// 着色器部分
    void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format);

    void bindTextureUnit(int unit, int texture);

    void bindSampler(int unit, int sampler);

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

    // Buffer相关
    int createBuffer();

    void bufferData(int buffer, int target, Buffer data, int usage);

    void bufferSubData(int buffer, int offset, Buffer data);

    void deleteBuffer(int buffer);

    void bindBufferBase(int target, int bindingPoint, int buffer);
}