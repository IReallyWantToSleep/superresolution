package io.homo.superresolution.core.gl;

import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.GL_MAX_IMAGE_UNITS;

public class GlState implements AutoCloseable {
    private static final int MAX_TEXTURES = 32;
    public int program;
    public int vao;
    public int vbo;
    public int ebo;
    public int wFbo;
    public int rFbo;

    public int texture2D;
    public int activeTextureNumber;
    public int[] textures;
    public int[] view;
    public int blendSrcRGB;
    public int blendDstRGB;
    public int blendSrcAlpha;
    public int blendDstAlpha;
    public boolean blendEnabled;
    public boolean cullFaceEnabled;
    public int cullFaceMode;
    public int frontFace;
    public boolean depthTestEnabled;
    public boolean scissorTestEnabled;
    public boolean[] colorMask;
    public int stencilFunc;
    public int stencilRef;
    public int stencilValueMask;
    public int stencilMask;
    public int stencilOpFail;
    public int stencilOpZFail;
    public int stencilOpZPass;
    public int uniformBufferBinding;
    public int unpackAlignment;
    public int unpackRowLength;
    public int unpackSkipPixels;
    public int unpackSkipRows;

    public GlState() {
        this.saveState();
    }

    public void saveState() {
        this.program = glGetInteger(GL_CURRENT_PROGRAM);
        this.vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        this.vbo = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        this.ebo = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        this.rFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        this.wFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        this.texture2D = glGetInteger(GL_TEXTURE_BINDING_2D);
        this.activeTextureNumber = glGetInteger(GL_ACTIVE_TEXTURE);
        this.textures = new int[MAX_TEXTURES];
        for (int i = 0; i < MAX_TEXTURES; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            this.textures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
        }
        this.view = new int[4];
        glGetIntegerv(GL_VIEWPORT, this.view);
        this.blendSrcRGB = glGetInteger(GL_BLEND_SRC_RGB);
        this.blendDstRGB = glGetInteger(GL_BLEND_DST_RGB);
        this.blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        this.blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        this.blendEnabled = glIsEnabled(GL_BLEND);
        this.cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        this.cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
        this.frontFace = glGetInteger(GL_FRONT_FACE);
        this.depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        this.scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST);
        int[] colorMaskArray = new int[4];
        glGetIntegerv(GL_COLOR_WRITEMASK, colorMaskArray);
        this.colorMask = new boolean[4];
        for (int i = 0; i < 4; i++) {
            this.colorMask[i] = colorMaskArray[i] != 0;
        }
        this.stencilFunc = glGetInteger(GL_STENCIL_FUNC);
        this.stencilRef = glGetInteger(GL_STENCIL_REF);
        this.stencilValueMask = glGetInteger(GL_STENCIL_VALUE_MASK);
        this.stencilMask = glGetInteger(GL_STENCIL_WRITEMASK);
        this.stencilOpFail = glGetInteger(GL_STENCIL_FAIL);
        this.stencilOpZFail = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL);
        this.stencilOpZPass = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS);
        this.uniformBufferBinding = glGetInteger(GL_UNIFORM_BUFFER_BINDING);
        this.unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        this.unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        this.unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        this.unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);
    }

    public void restore() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.wFbo);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.rFbo);

        for (int i = 0; i < MAX_TEXTURES; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, this.textures[i]);
        }
        glActiveTexture(this.activeTextureNumber);
        glBindVertexArray(this.vao);
        glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        glUseProgram(this.program);
        glViewport(this.view[0], this.view[1], this.view[2], this.view[3]);
        glBlendFuncSeparate(
                this.blendSrcRGB, this.blendDstRGB,
                this.blendSrcAlpha, this.blendDstAlpha
        );
        setGlCap(GL_BLEND, this.blendEnabled);
        setGlCap(GL_CULL_FACE, this.cullFaceEnabled);
        glCullFace(this.cullFaceMode);
        glFrontFace(this.frontFace);
        setGlCap(GL_DEPTH_TEST, this.depthTestEnabled);
        setGlCap(GL_SCISSOR_TEST, this.scissorTestEnabled);
        glColorMask(
                this.colorMask[0],
                this.colorMask[1],
                this.colorMask[2],
                this.colorMask[3]
        );
        glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
        glStencilMask(this.stencilMask);
        glStencilOp(this.stencilOpFail, this.stencilOpZFail, this.stencilOpZPass);
        glBindBuffer(GL_UNIFORM_BUFFER, this.uniformBufferBinding);
        glPixelStorei(GL_UNPACK_ALIGNMENT, this.unpackAlignment);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, this.unpackRowLength);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, this.unpackSkipPixels);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, this.unpackSkipRows);
    }

    private void setGlCap(int cap, boolean enabled) {
        if (enabled) glEnable(cap);
        else glDisable(cap);
    }

    @Override
    public void close() {
        restore();
    }
}