package io.homo.superresolution.core.graphics.opengl.dsa;

import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45C;

import java.nio.*;

import static org.lwjgl.opengl.GL41.*;

public class CompatDirectStateAccessImpl implements IGlDirectStateAccess {
    private final boolean gl43 = GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] >= 3;

    @Override
    public void generateTextureMipmap(int texture) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public int createSampler() {
        int[] samplers = new int[1];
        glGenSamplers(samplers);
        return samplers[0];
    }

    @Override
    public void samplerParameteri(int sampler, int pname, int param) {
        glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void deleteSampler(int sampler) {
        glDeleteSamplers(sampler);
    }

    @Override
    public int createTextureView(int srcTexture, int target, int internalFormat,
                                 int minLevel, int numLevels, int minLayer, int numLayers) {
        int viewId = GL43.glGenTextures();
        GL43.glTextureView(
                viewId,
                target,
                srcTexture,
                internalFormat,
                minLevel,
                numLevels,
                minLayer,
                numLayers
        );
        throw new UnsupportedOperationException("glTextureView not available in OpenGL 4.1");
    }

    @Override
    public void blitFramebuffer(int readFramebuffer, int drawFramebuffer,
                                int srcX0, int srcY0, int srcX1, int srcY1,
                                int dstX0, int dstY0, int dstX1, int dstY1,
                                int mask, int filter) {
        int prevRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFramebuffer);

        glBlitFramebuffer(
                srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1,
                mask, filter
        );

        glBindFramebuffer(GL_READ_FRAMEBUFFER, prevRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    @Override
    public void clearFramebuffer(int framebuffer, int buffer, int drawbuffer, float[] value) {
        int prevFBO = glGetInteger(GL_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        switch (buffer) {
            case GL_COLOR:
                glClearBufferfv(GL_COLOR, drawbuffer, value);
                break;
            case GL_DEPTH:
                glClearBufferfv(GL_DEPTH, 0, value);
                break;
            case GL_STENCIL:
                int[] ivalue = new int[]{(int) value[0]};
                glClearBufferiv(GL_STENCIL, 0, ivalue);
                break;
            default:
                throw new IllegalArgumentException("Unsupported buffer type");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, prevFBO);
    }

    @Override
    public void copyTextureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                      int x, int y, int width, int height) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glCopyTexSubImage2D(GL_TEXTURE_2D, level, xoffset, yoffset, x, y, width, height);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public void copyTextureSubImage1D(int texture, int level, int xoffset,
                                      int x, int y, int width) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_1D);
        glBindTexture(GL_TEXTURE_1D, texture);
        glCopyTexSubImage1D(GL_TEXTURE_1D, level, xoffset, x, y, width);
        glBindTexture(GL_TEXTURE_1D, prevTex);
    }

    @Override
    public int createBuffer() {
        int[] buffers = new int[1];
        glGenBuffers(buffers);
        return buffers[0];
    }

    @Override
    public void bufferData(int buffer, int target, Buffer data, int usage) {
        int prevBuffer = glGetInteger(target);
        glBindBuffer(target, buffer);

        if (data instanceof ByteBuffer) {
            glBufferData(target, (ByteBuffer) data, usage);
        } else if (data instanceof FloatBuffer) {
            glBufferData(target, (FloatBuffer) data, usage);
        } else if (data instanceof IntBuffer) {
            glBufferData(target, (IntBuffer) data, usage);
        } else if (data instanceof ShortBuffer) {
            glBufferData(target, (ShortBuffer) data, usage);
        } else if (data instanceof LongBuffer) {
            throw new UnsupportedOperationException("LongBuffer not supported in OpenGL 4.1");
        } else if (data instanceof DoubleBuffer) {
            glBufferData(target, (DoubleBuffer) data, usage);
        }

        glBindBuffer(target, prevBuffer);
    }

    @Override
    public void bufferSubData(int buffer, int offset, Buffer data) {
        int prevBuffer = glGetInteger(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, buffer);

        if (data instanceof ByteBuffer) {
            glBufferSubData(GL_ARRAY_BUFFER, offset, (ByteBuffer) data);
        } else if (data instanceof FloatBuffer) {
            glBufferSubData(GL_ARRAY_BUFFER, offset, (FloatBuffer) data);
        } else if (data instanceof IntBuffer) {
            glBufferSubData(GL_ARRAY_BUFFER, offset, (IntBuffer) data);
        } else if (data instanceof ShortBuffer) {
            glBufferSubData(GL_ARRAY_BUFFER, offset, (ShortBuffer) data);
        } else if (data instanceof LongBuffer) {
            throw new UnsupportedOperationException("LongBuffer not supported in OpenGL 4.1");
        } else if (data instanceof DoubleBuffer) {
            glBufferSubData(GL_ARRAY_BUFFER, offset, (DoubleBuffer) data);
        }

        glBindBuffer(GL_ARRAY_BUFFER, prevBuffer);
    }

    @Override
    public void deleteBuffer(int buffer) {
        glDeleteBuffers(buffer);
    }

    @Override
    public void bindBufferBase(int target, int bindingPoint, int buffer) {
        glBindBufferBase(target, bindingPoint, buffer);
    }

    @Override
    public void bindVertexArray(int vao) {
        glBindVertexArray(vao);
    }

    @Override
    public void bindImageTexture(int unit, int texture, int level, boolean layered,
                                 int layer, int access, int format) {
        GL43.glBindImageTexture(
                unit,
                texture,
                level,
                layered,
                layer,
                access,
                format
        );
    }

    @Override
    public void bindTextureUnit(int unit, int texture) {
        //懒得实现
        throw new UnsupportedOperationException(
                "bindTextureUnit not available in OpenGL 4.1.");
    }

    @Override
    public void bindSampler(int unit, int sampler) {
        glBindSampler(unit, sampler);
    }

    @Override
    public int createTexture2D() {
        int[] textures = new int[1];
        glGenTextures(textures);
        return textures[0];
    }

    @Override
    public int createTexture1D() {
        int[] textures = new int[1];
        glGenTextures(textures);
        return textures[0];
    }

    @Override
    public void textureParameteri(int texture, int pname, int value) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, pname, value);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public void textureParameterf(int texture, int pname, float value) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameterf(GL_TEXTURE_2D, pname, value);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public void textureStorage2D(int target, int levels, int internalFormat,
                                 int width, int height) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, target);
        if (gl43) {
            GL43.glTexStorage2D(
                    GL_TEXTURE_2D,
                    levels,
                    internalFormat,
                    width,
                    height
            );
        } else {
            for (int level = 0; level < levels; level++) {
                int levelWidth = Math.max(1, width >> level);
                int levelHeight = Math.max(1, height >> level);

                glTexImage2D(
                        GL_TEXTURE_2D,
                        level,
                        internalFormat,
                        levelWidth,
                        levelHeight,
                        0,
                        getFormatFromInternal(internalFormat),
                        getTypeFromInternal(internalFormat),
                        (ByteBuffer) null
                );
            }
        }
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public void textureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                  int width, int height, int format, int type, long pixels) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexSubImage2D(GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, pixels);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    @Override
    public void textureStorage1D(int target, int levels, int internalFormat,
                                 int width) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_1D);
        glBindTexture(GL_TEXTURE_1D, target);
        if (gl43) {
            GL43.glTexStorage1D(
                    GL_TEXTURE_1D,
                    levels,
                    internalFormat,
                    width
            );
        } else {
            for (int level = 0; level < levels; level++) {
                int levelWidth = Math.max(1, width >> level);
                glTexImage1D(
                        target,
                        level,
                        internalFormat,
                        levelWidth,
                        0,
                        getFormatFromInternal(internalFormat),
                        getTypeFromInternal(internalFormat),
                        (ByteBuffer) null
                );
            }
        }
        glBindTexture(GL_TEXTURE_1D, prevTex);
    }

    @Override
    public void textureSubImage1D(int texture, int level, int xoffset, int width,
                                  int format, int type, long pixels) {
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_1D);
        glBindTexture(GL_TEXTURE_1D, texture);
        glTexSubImage1D(GL_TEXTURE_1D, level, xoffset, width, format, type, pixels);
        glBindTexture(GL_TEXTURE_1D, prevTex);
    }

    @Override
    public int checkNamedFramebufferStatus(int framebuffer, int target) {
        int prevFBO = glGetInteger(GL_FRAMEBUFFER_BINDING);
        glBindFramebuffer(target, framebuffer);
        int status = glCheckFramebufferStatus(target);
        glBindFramebuffer(target, prevFBO);
        return status;
    }

    @Override
    public void clearNamedFramebufferfv(int framebuffer, int buffer, int drawbuffer, float[] value) {
        clearFramebuffer(framebuffer, buffer, drawbuffer, value);
    }

    @Override
    public void clearNamedFramebufferfi(int framebuffer, int buffer, int drawbuffer,
                                        float depth, int stencil) {
        int prevFBO = glGetInteger(GL_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glClearBufferfi(buffer, drawbuffer, depth, stencil);
        glBindFramebuffer(GL_FRAMEBUFFER, prevFBO);
    }

    @Override
    public int createVertexArray() {
        int[] vaos = new int[1];
        glGenVertexArrays(vaos);
        return vaos[0];
    }

    @Override
    public void vertexArrayVertexBuffer(int vao, int bindingIndex, int buffer,
                                        long offset, int stride) {
        throw new UnsupportedOperationException(
                "vertexArrayVertexBuffer not available in OpenGL 4.1.");
    }

    @Override
    public void vertexArrayAttribFormat(int vao, int attribIndex, int size, int type,
                                        boolean normalized, int relativeOffset) {
        throw new UnsupportedOperationException(
                "vertexArrayAttribFormat not available in OpenGL 4.1.");
    }

    @Override
    public void vertexArrayAttribBinding(int vao, int attribIndex, int bindingIndex) {
        throw new UnsupportedOperationException(
                "vertexArrayAttribBinding not available in OpenGL 4.1.");
    }

    @Override
    public void enableVertexArrayAttrib(int vaobj, int index) {
        int prevVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        glBindVertexArray(vaobj);
        glEnableVertexAttribArray(index);
        glBindVertexArray(prevVAO);
    }

    @Override
    public int createFramebuffer() {
        int[] fbos = new int[1];
        glGenFramebuffers(fbos);
        return fbos[0];
    }

    @Override
    public void framebufferTexture(int framebuffer, int attachment, int texture, int level) {
        int prevFBO = glGetInteger(GL_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture(GL_FRAMEBUFFER, attachment, texture, level);
        glBindFramebuffer(GL_FRAMEBUFFER, prevFBO);
    }

    @Override
    public void deleteTexture(int texture) {
        glDeleteTextures(texture);
    }

    @Override
    public void deleteVertexArray(int vao) {
        glDeleteVertexArrays(vao);
    }

    @Override
    public void deleteFramebuffer(int fbo) {
        glDeleteFramebuffers(fbo);
    }

    private static int getFormatFromInternal(int internalFormat) {
        return switch (internalFormat) {
            case GL_R8, GL_R8_SNORM, GL_R16, GL_R16_SNORM, GL_R16F, GL_R32F, GL_R8I, GL_R8UI, GL_R16I, GL_R16UI,
                 GL_R32I, GL_R32UI -> GL_RED;
            case GL_RG8, GL_RG8_SNORM, GL_RG16, GL_RG16_SNORM, GL_RG16F, GL_RG32F, GL_RG8I, GL_RG8UI, GL_RG16I,
                 GL_RG16UI, GL_RG32I, GL_RG32UI -> GL_RG;
            case GL_RGB8, GL_SRGB8, GL_RGB8_SNORM, GL_RGB16, GL_RGB16_SNORM, GL_RGB16F, GL_RGB32F, GL_R11F_G11F_B10F,
                 GL_RGB8I, GL_RGB8UI, GL_RGB16I, GL_RGB16UI, GL_RGB32I, GL_RGB32UI -> GL_RGB;
            case GL_RGBA8, GL_SRGB8_ALPHA8, GL_RGBA8_SNORM, GL_RGBA16, GL_RGBA16_SNORM, GL_RGBA16F, GL_RGBA32F,
                 GL_RGBA8I, GL_RGBA8UI, GL_RGBA16I, GL_RGBA16UI, GL_RGBA32I, GL_RGBA32UI -> GL_RGBA;
            case GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32F -> GL_DEPTH_COMPONENT;
            case GL_DEPTH24_STENCIL8, GL_DEPTH32F_STENCIL8 -> GL_DEPTH_STENCIL;
            case GL_COMPRESSED_RED, GL_COMPRESSED_RG, GL_COMPRESSED_RGB, GL_COMPRESSED_RGBA, GL_COMPRESSED_SRGB,
                 GL_COMPRESSED_SRGB_ALPHA ->
                    throw new IllegalArgumentException("Compressed formats require direct DSA");
            default -> throw new IllegalArgumentException("Unsupported internal format: 0x" +
                    Integer.toHexString(internalFormat));
        };
    }

    private static int getTypeFromInternal(int internalFormat) {
        return switch (internalFormat) {
            case GL_R8, GL_RG8, GL_RGB8, GL_RGBA8, GL_SRGB8, GL_SRGB8_ALPHA8 -> GL_UNSIGNED_BYTE;
            case GL_R8_SNORM, GL_RG8_SNORM, GL_RGB8_SNORM, GL_RGBA8_SNORM -> GL_BYTE;
            case GL_R16, GL_RG16, GL_RGB16, GL_RGBA16 -> GL_UNSIGNED_SHORT;
            case GL_R16_SNORM, GL_RG16_SNORM, GL_RGB16_SNORM, GL_RGBA16_SNORM -> GL_SHORT;
            case GL_R16F, GL_RG16F, GL_RGB16F, GL_RGBA16F -> GL_HALF_FLOAT;
            case GL_R32F, GL_RG32F, GL_RGB32F, GL_RGBA32F -> GL_FLOAT;
            case GL_R11F_G11F_B10F -> GL_UNSIGNED_INT_10F_11F_11F_REV;
            case GL_R8UI, GL_RG8UI, GL_RGB8UI, GL_RGBA8UI -> GL_UNSIGNED_BYTE;
            case GL_R8I, GL_RG8I, GL_RGB8I, GL_RGBA8I -> GL_BYTE;
            case GL_R16UI, GL_RG16UI, GL_RGB16UI, GL_RGBA16UI -> GL_UNSIGNED_SHORT;
            case GL_R16I, GL_RG16I, GL_RGB16I, GL_RGBA16I -> GL_SHORT;
            case GL_R32UI, GL_RG32UI, GL_RGB32UI, GL_RGBA32UI -> GL_UNSIGNED_INT;
            case GL_R32I, GL_RG32I, GL_RGB32I, GL_RGBA32I -> GL_INT;
            case GL_DEPTH_COMPONENT16 -> GL_UNSIGNED_SHORT;
            case GL_DEPTH_COMPONENT24, GL_DEPTH24_STENCIL8 -> GL_UNSIGNED_INT;
            case GL_DEPTH_COMPONENT32F, GL_DEPTH32F_STENCIL8 -> GL_FLOAT;
            default -> throw new IllegalArgumentException("Unsupported internal format: 0x" +
                    Integer.toHexString(internalFormat));
        };
    }
}