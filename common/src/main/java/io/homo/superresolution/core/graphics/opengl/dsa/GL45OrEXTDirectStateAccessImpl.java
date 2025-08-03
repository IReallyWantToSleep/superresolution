package io.homo.superresolution.core.graphics.opengl.dsa;

import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL45C;

import java.nio.*;

public class GL45OrEXTDirectStateAccessImpl implements IGlDirectStateAccess {

    @Override
    public void generateTextureMipmap(int texture) {
        GL45C.glGenerateTextureMipmap(texture);
    }

    @Override
    public int createSampler() {
        return GL45C.glCreateSamplers();
    }

    @Override
    public void samplerParameteri(int sampler, int pname, int param) {
        GL45C.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void deleteSampler(int sampler) {
        GL45C.glDeleteSamplers(sampler);
    }

    @Override
    public int createTextureView(int srcTexture, int target, int internalFormat,
                                 int minLevel, int numLevels, int minLayer, int numLayers) {
        int viewId = GL45C.glGenTextures();
        GL45C.glTextureView(
                viewId,
                target,
                srcTexture,
                internalFormat,
                minLevel,
                numLevels,
                minLayer,
                numLayers
        );
        return viewId;
    }

    @Override
    public void blitFramebuffer(int readFramebuffer, int drawFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL45C.glBlitNamedFramebuffer(
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
        GL45C.glClearNamedFramebufferfv(
                framebuffer,
                buffer,
                drawbuffer,
                value
        );
    }

    @Override
    public void copyTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL45C.glCopyTextureSubImage2D(
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
        GL45C.glCopyTextureSubImage1D(
                texture,
                level,
                xoffset,
                x,
                y,
                width
        );
    }

    @Override
    public int createBuffer() {
        return GL45C.glCreateBuffers();
    }

    @Override
    public void bufferData(int buffer, int target, Buffer data, int usage) {

        if (data instanceof ByteBuffer) {
            GL45C.glNamedBufferData(buffer, (ByteBuffer) data, usage);

        } else if (data instanceof FloatBuffer) {
            GL45C.glNamedBufferData(buffer, (FloatBuffer) data, usage);
        } else if (data instanceof IntBuffer) {
            GL45C.glNamedBufferData(buffer, (IntBuffer) data, usage);

        } else if (data instanceof ShortBuffer) {
            GL45C.glNamedBufferData(buffer, (ShortBuffer) data, usage);

        } else if (data instanceof LongBuffer) {
            GL45C.glNamedBufferData(buffer, (LongBuffer) data, usage);

        } else if (data instanceof DoubleBuffer) {
            GL45C.glNamedBufferData(buffer, (DoubleBuffer) data, usage);
        }
    }

    @Override
    public void bufferSubData(int buffer, int offset, Buffer data) {
        if (data instanceof ByteBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (ByteBuffer) data);

        } else if (data instanceof FloatBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (FloatBuffer) data);
        } else if (data instanceof IntBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (IntBuffer) data);

        } else if (data instanceof ShortBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (ShortBuffer) data);

        } else if (data instanceof LongBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (LongBuffer) data);

        } else if (data instanceof DoubleBuffer) {
            GL45C.glNamedBufferSubData(buffer, offset, (DoubleBuffer) data);
        }
    }

    @Override
    public void deleteBuffer(int buffer) {
        GL45C.glDeleteBuffers(buffer);
    }

    @Override
    public void bindBufferBase(int target, int bindingPoint, int buffer) {
        GL45C.glBindBufferBase(target, bindingPoint, buffer);
    }

    @Override
    public void bindVertexArray(int vao) {
        GL45C.glBindVertexArray(vao);
    }

    @Override
    public void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GL45C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    @Override
    public void bindTextureUnit(int unit, int texture) {
        GL45C.glBindTextureUnit(unit, texture);
    }

    @Override
    public void bindSampler(int unit, int sampler) {
        GL45C.glBindSampler(unit, sampler);
    }

    @Override
    public int createTexture2D() {
        return GL45C.glCreateTextures(GL45C.GL_TEXTURE_2D);
    }

    @Override
    public int createTexture1D() {
        return GL45C.glCreateTextures(GL45C.GL_TEXTURE_1D);
    }

    @Override
    public void textureParameteri(int texture, int pname, int value) {
        GL45C.glTextureParameteri(texture, pname, value);
    }

    @Override
    public void textureParameterf(int texture, int pname, float value) {
        GL45C.glTextureParameterf(texture, pname, value);

    }

    @Override
    public void textureStorage2D(int texture, int levels, int internalFormat, int width, int height) {
        GL45C.glTextureStorage2D(texture, levels, internalFormat, width, height);
    }

    @Override
    public void textureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
        GL45C.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void textureStorage1D(int texture, int levels, int internalFormat, int width) {
        GL45C.glTextureStorage1D(texture, levels, internalFormat, width);
    }

    @Override
    public void textureSubImage1D(int texture, int level, int xoffset, int width, int format, int type, long pixels) {
        GL45C.glTextureSubImage1D(texture, level, xoffset, width, format, type, pixels);
    }

    @Override
    public int checkNamedFramebufferStatus(int framebuffer, int target) {
        return GL45C.glCheckNamedFramebufferStatus(framebuffer, target);
    }

    @Override
    public void clearNamedFramebufferfv(int framebuffer, int buffer, int drawbuffer, float[] value) {
        GL45C.glClearNamedFramebufferfv(framebuffer, buffer, drawbuffer, value);
    }

    @Override
    public void clearNamedFramebufferfi(int framebuffer, int buffer, int drawbuffer, float depth, int stencil) {
        GL45C.glClearNamedFramebufferfi(framebuffer, buffer, drawbuffer, depth, stencil);
    }

    @Override
    public int createVertexArray() {
        return GL45C.glCreateVertexArrays();
    }

    @Override
    public void vertexArrayVertexBuffer(int vao, int bindingIndex, int buffer, long offset, int stride) {
        GL45C.glVertexArrayVertexBuffer(vao, bindingIndex, buffer, offset, stride);
    }

    @Override
    public void vertexArrayAttribFormat(int vao, int attribIndex, int size, int type, boolean normalized, int relativeOffset) {
        if (type == GL45.GL_FLOAT) {
            GL45C.glVertexArrayAttribFormat(vao, attribIndex, size, type, normalized, relativeOffset);
        } else {
            GL45C.glVertexArrayAttribIFormat(vao, attribIndex, size, type, relativeOffset);
        }
    }

    @Override
    public void vertexArrayAttribBinding(int vao, int attribIndex, int bindingIndex) {
        GL45C.glVertexArrayAttribBinding(vao, attribIndex, bindingIndex);
    }

    @Override
    public void enableVertexArrayAttrib(int vaobj, int index) {
        GL45C.glEnableVertexArrayAttrib(vaobj, index);
    }

    @Override
    public int createFramebuffer() {
        return GL45C.glCreateFramebuffers();
    }

    @Override
    public void framebufferTexture(int framebuffer, int attachment, int texture, int level) {
        GL45C.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
    }

    @Override
    public void deleteTexture(int texture) {
        GL45C.glDeleteTextures(texture);
    }

    @Override
    public void deleteVertexArray(int vao) {
        GL45C.glDeleteVertexArrays(vao);
    }

    @Override
    public void deleteFramebuffer(int fbo) {
        GL45C.glDeleteFramebuffers(fbo);
    }
}