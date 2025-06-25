package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.PerformanceInfo;

import static org.lwjgl.opengl.GL45.*;

public class GlState implements AutoCloseable {
    public static final long STATE_PROGRAM = 1L << 0;
    public static final long STATE_VAO = 1L << 1;
    public static final long STATE_VBO = 1L << 2;
    public static final long STATE_EBO = 1L << 3;
    public static final long STATE_READ_FBO = 1L << 4;
    public static final long STATE_DRAW_FBO = 1L << 5;
    public static final long STATE_TEXTURE_2D = 1L << 6;
    public static final long STATE_ACTIVE_TEXTURE = 1L << 7;
    public static final long STATE_TEXTURES = 1L << 8;
    public static final long STATE_VIEWPORT = 1L << 9;
    public static final long STATE_BLEND_FUNC = 1L << 10;
    public static final long STATE_BLEND_ENABLE = 1L << 11;
    public static final long STATE_CULL_FACE_ENABLE = 1L << 12;
    public static final long STATE_CULL_FACE_MODE = 1L << 13;
    public static final long STATE_FRONT_FACE = 1L << 14;
    public static final long STATE_DEPTH_TEST = 1L << 15;
    public static final long STATE_SCISSOR_TEST = 1L << 16;
    public static final long STATE_COLOR_MASK = 1L << 17;
    public static final long STATE_STENCIL_FUNC = 1L << 18;
    public static final long STATE_STENCIL_OP = 1L << 19;
    public static final long STATE_STENCIL_MASK = 1L << 20;
    public static final long STATE_UNIFORM_BUFFER = 1L << 21;
    public static final long STATE_UNPACK = 1L << 22;
    public static final long STATE_VERTEX_ATTRIB_ARRAY = 1L << 23;
    public static final long STATE_VERTEX_BINDING_DIVISOR = 1L << 24;
    public static final long STATE_COPY_READ_BUFFER = 1L << 25;
    public static final long STATE_COPY_WRITE_BUFFER = 1L << 26;
    public static final long STATE_PIXEL_PACK_BUFFER = 1L << 27;
    public static final long STATE_PIXEL_UNPACK_BUFFER = 1L << 28;
    public static final long STATE_TRANSFORM_FEEDBACK_BUFFER = 1L << 29;
    public static final long STATE_ATOMIC_COUNTER_BUFFER = 1L << 30;
    public static final long STATE_DISPATCH_INDIRECT_BUFFER = 1L << 31;
    public static final long STATE_DRAW_INDIRECT_BUFFER = 1L << 32;
    public static final long STATE_SHADER_STORAGE_BUFFER = 1L << 33;

    public static final long STATE_ALL = ~0L;
    public static final long STATE_VERTEX_OPERATIONS = STATE_VERTEX_ATTRIB_ARRAY | STATE_VERTEX_BINDING_DIVISOR;
    public static final long STATE_BUFFER_OPERATIONS = STATE_COPY_READ_BUFFER | STATE_COPY_WRITE_BUFFER |
            STATE_PIXEL_PACK_BUFFER | STATE_PIXEL_UNPACK_BUFFER |
            STATE_TRANSFORM_FEEDBACK_BUFFER | STATE_ATOMIC_COUNTER_BUFFER |
            STATE_DISPATCH_INDIRECT_BUFFER | STATE_DRAW_INDIRECT_BUFFER |
            STATE_SHADER_STORAGE_BUFFER;
    private static final long DEFAULT_MASK = STATE_ALL;

    private static final int MAX_TEXTURES = 32;
    private final long stateMask;
    private int program;
    private int vao;
    private int vbo;
    private int ebo;
    private int wFbo;
    private int rFbo;
    private int texture2D;
    private int activeTextureNumber;
    private int[] textures;
    private int[] view;
    private int blendSrcRGB;
    private int blendDstRGB;
    private int blendSrcAlpha;
    private int blendDstAlpha;
    private boolean blendEnabled;
    private boolean cullFaceEnabled;
    private int cullFaceMode;
    private int frontFace;
    private boolean depthTestEnabled;
    private boolean scissorTestEnabled;
    private boolean[] colorMask;
    private int stencilFunc;
    private int stencilRef;
    private int stencilValueMask;
    private int stencilMask;
    private int stencilOpFail;
    private int stencilOpZFail;
    private int stencilOpZPass;
    private int uniformBufferBinding;
    private int unpackAlignment;
    private int unpackRowLength;
    private int unpackSkipPixels;
    private int unpackSkipRows;
    private int copyReadBuffer;
    private int copyWriteBuffer;
    private int pixelPackBuffer;
    private int pixelUnpackBuffer;
    private int transformFeedbackBuffer;
    private int atomicCounterBuffer;
    private int dispatchIndirectBuffer;
    private int drawIndirectBuffer;
    private int shaderStorageBuffer;

    public GlState() {
        this(DEFAULT_MASK);
    }

    public GlState(long stateMask) {
        this.stateMask = stateMask;
        this.saveState();
    }


    public void saveState() {
        int originalActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);

        if ((stateMask & STATE_PROGRAM) != 0) {
            this.program = glGetInteger(GL_CURRENT_PROGRAM);
        }
        if ((stateMask & STATE_VAO) != 0) {
            this.vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        }
        if ((stateMask & STATE_VBO) != 0) {
            this.vbo = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        }
        if ((stateMask & STATE_EBO) != 0) {
            this.ebo = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        }
        if ((stateMask & STATE_READ_FBO) != 0) {
            this.rFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        }
        if ((stateMask & STATE_DRAW_FBO) != 0) {
            this.wFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        }

        if ((stateMask & STATE_TEXTURES) != 0) {
            this.textures = new int[MAX_TEXTURES];
            for (int i = 0; i < MAX_TEXTURES; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                this.textures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            }
            glActiveTexture(originalActiveTexture);
        }

        if ((stateMask & STATE_ACTIVE_TEXTURE) != 0) {
            this.activeTextureNumber = originalActiveTexture;
        }
        if ((stateMask & STATE_TEXTURE_2D) != 0) {
            this.texture2D = glGetInteger(GL_TEXTURE_BINDING_2D);
        }

        if ((stateMask & STATE_VIEWPORT) != 0) {
            this.view = new int[4];
            glGetIntegerv(GL_VIEWPORT, this.view);
        }
        if ((stateMask & STATE_BLEND_FUNC) != 0) {
            this.blendSrcRGB = glGetInteger(GL_BLEND_SRC_RGB);
            this.blendDstRGB = glGetInteger(GL_BLEND_DST_RGB);
            this.blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
            this.blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        }

        if ((stateMask & STATE_BLEND_ENABLE) != 0) {
            this.blendEnabled = glIsEnabled(GL_BLEND);
        }
        if ((stateMask & STATE_CULL_FACE_ENABLE) != 0) {
            this.cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        }
        if ((stateMask & STATE_CULL_FACE_MODE) != 0) {
            this.cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
        }
        if ((stateMask & STATE_FRONT_FACE) != 0) {
            this.frontFace = glGetInteger(GL_FRONT_FACE);
        }
        if ((stateMask & STATE_DEPTH_TEST) != 0) {
            this.depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        }
        if ((stateMask & STATE_SCISSOR_TEST) != 0) {
            this.scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST);
        }

        if ((stateMask & STATE_COLOR_MASK) != 0) {
            int[] colorMaskArray = new int[4];
            glGetIntegerv(GL_COLOR_WRITEMASK, colorMaskArray);
            this.colorMask = new boolean[4];
            for (int i = 0; i < 4; i++) {
                this.colorMask[i] = colorMaskArray[i] != 0;
            }
        }
        if ((stateMask & STATE_STENCIL_FUNC) != 0) {
            this.stencilFunc = glGetInteger(GL_STENCIL_FUNC);
            this.stencilRef = glGetInteger(GL_STENCIL_REF);
            this.stencilValueMask = glGetInteger(GL_STENCIL_VALUE_MASK);
        }
        if ((stateMask & STATE_STENCIL_OP) != 0) {
            this.stencilOpFail = glGetInteger(GL_STENCIL_FAIL);
            this.stencilOpZFail = glGetInteger(GL_STENCIL_PASS_DEPTH_FAIL);
            this.stencilOpZPass = glGetInteger(GL_STENCIL_PASS_DEPTH_PASS);
        }
        if ((stateMask & STATE_STENCIL_MASK) != 0) {
            this.stencilMask = glGetInteger(GL_STENCIL_WRITEMASK);
        }

        if ((stateMask & STATE_UNIFORM_BUFFER) != 0) {
            this.uniformBufferBinding = glGetInteger(GL_UNIFORM_BUFFER_BINDING);
        }
        if ((stateMask & STATE_UNPACK) != 0) {
            this.unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
            this.unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
            this.unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
            this.unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);
        }
        if ((stateMask & STATE_COPY_READ_BUFFER) != 0) {
            this.copyReadBuffer = glGetInteger(GL_COPY_READ_BUFFER_BINDING);
        }
        if ((stateMask & STATE_COPY_WRITE_BUFFER) != 0) {
            this.copyWriteBuffer = glGetInteger(GL_COPY_WRITE_BUFFER_BINDING);
        }
        if ((stateMask & STATE_PIXEL_PACK_BUFFER) != 0) {
            this.pixelPackBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING);
        }
        if ((stateMask & STATE_PIXEL_UNPACK_BUFFER) != 0) {
            this.pixelUnpackBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        }
        if ((stateMask & STATE_TRANSFORM_FEEDBACK_BUFFER) != 0) {
            this.transformFeedbackBuffer = glGetInteger(GL_TRANSFORM_FEEDBACK_BUFFER_BINDING);
        }
        if ((stateMask & STATE_ATOMIC_COUNTER_BUFFER) != 0) {
            this.atomicCounterBuffer = glGetInteger(GL_ATOMIC_COUNTER_BUFFER_BINDING);
        }
        if ((stateMask & STATE_DISPATCH_INDIRECT_BUFFER) != 0) {
            this.dispatchIndirectBuffer = glGetInteger(GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        }
        if ((stateMask & STATE_DRAW_INDIRECT_BUFFER) != 0) {
            this.drawIndirectBuffer = glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING);
        }
        if ((stateMask & STATE_SHADER_STORAGE_BUFFER) != 0) {
            this.shaderStorageBuffer = glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        }
    }

    public void restore() {
        if ((stateMask & STATE_DRAW_FBO) != 0) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.wFbo);
        }
        if ((stateMask & STATE_READ_FBO) != 0) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, this.rFbo);
        }

        if ((stateMask & STATE_TEXTURES) != 0 && this.textures != null) {
            int originalActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
            for (int i = 0; i < MAX_TEXTURES; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, this.textures[i]);
            }
            glActiveTexture(originalActiveTexture);
        }
        if ((stateMask & STATE_ACTIVE_TEXTURE) != 0) {
            glActiveTexture(this.activeTextureNumber);
        }
        if ((stateMask & STATE_TEXTURE_2D) != 0) {
            glBindTexture(GL_TEXTURE_2D, this.texture2D);
        }


        if ((stateMask & STATE_VAO) != 0) {
            glBindVertexArray(this.vao);
        }
        if ((stateMask & STATE_VBO) != 0) {
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
        }
        if ((stateMask & STATE_EBO) != 0) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        }
        if ((stateMask & STATE_PROGRAM) != 0) {
            glUseProgram(this.program);
        }

        if ((stateMask & STATE_VIEWPORT) != 0 && this.view != null) {
            glViewport(this.view[0], this.view[1], this.view[2], this.view[3]);
        }
        if ((stateMask & STATE_BLEND_FUNC) != 0) {
            glBlendFuncSeparate(
                    this.blendSrcRGB, this.blendDstRGB,
                    this.blendSrcAlpha, this.blendDstAlpha
            );
        }


        if ((stateMask & STATE_BLEND_ENABLE) != 0) {
            setGlCap(GL_BLEND, this.blendEnabled);
        }
        if ((stateMask & STATE_CULL_FACE_ENABLE) != 0) {
            setGlCap(GL_CULL_FACE, this.cullFaceEnabled);
        }
        if ((stateMask & STATE_CULL_FACE_MODE) != 0) {
            glCullFace(this.cullFaceMode);
        }
        if ((stateMask & STATE_FRONT_FACE) != 0) {
            glFrontFace(this.frontFace);
        }
        if ((stateMask & STATE_DEPTH_TEST) != 0) {
            setGlCap(GL_DEPTH_TEST, this.depthTestEnabled);
        }
        if ((stateMask & STATE_SCISSOR_TEST) != 0) {
            setGlCap(GL_SCISSOR_TEST, this.scissorTestEnabled);
        }


        if ((stateMask & STATE_COLOR_MASK) != 0 && this.colorMask != null) {
            glColorMask(
                    this.colorMask[0],
                    this.colorMask[1],
                    this.colorMask[2],
                    this.colorMask[3]
            );
        }
        if ((stateMask & STATE_STENCIL_FUNC) != 0) {
            glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
        }
        if ((stateMask & STATE_STENCIL_MASK) != 0) {
            glStencilMask(this.stencilMask);
        }
        if ((stateMask & STATE_STENCIL_OP) != 0) {
            glStencilOp(this.stencilOpFail, this.stencilOpZFail, this.stencilOpZPass);
        }


        if ((stateMask & STATE_UNIFORM_BUFFER) != 0) {
            glBindBuffer(GL_UNIFORM_BUFFER, this.uniformBufferBinding);
        }
        if ((stateMask & STATE_UNPACK) != 0) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, this.unpackAlignment);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, this.unpackRowLength);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, this.unpackSkipPixels);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, this.unpackSkipRows);
        }
        if ((stateMask & STATE_COPY_READ_BUFFER) != 0) {
            glBindBuffer(GL_COPY_READ_BUFFER, this.copyReadBuffer);
        }
        if ((stateMask & STATE_COPY_WRITE_BUFFER) != 0) {
            glBindBuffer(GL_COPY_WRITE_BUFFER, this.copyWriteBuffer);
        }
        if ((stateMask & STATE_PIXEL_PACK_BUFFER) != 0) {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, this.pixelPackBuffer);
        }
        if ((stateMask & STATE_PIXEL_UNPACK_BUFFER) != 0) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pixelUnpackBuffer);
        }
        if ((stateMask & STATE_TRANSFORM_FEEDBACK_BUFFER) != 0) {
            glBindBuffer(GL_TRANSFORM_FEEDBACK_BUFFER, this.transformFeedbackBuffer);
        }
        if ((stateMask & STATE_ATOMIC_COUNTER_BUFFER) != 0) {
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, this.atomicCounterBuffer);
        }
        if ((stateMask & STATE_DISPATCH_INDIRECT_BUFFER) != 0) {
            glBindBuffer(GL_DISPATCH_INDIRECT_BUFFER, this.dispatchIndirectBuffer);
        }
        if ((stateMask & STATE_DRAW_INDIRECT_BUFFER) != 0) {
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.drawIndirectBuffer);
        }
        if ((stateMask & STATE_SHADER_STORAGE_BUFFER) != 0) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.shaderStorageBuffer);
        }
    }

    private void setVertexAttribArray(int index, boolean enabled) {
        if (enabled) {
            glEnableVertexAttribArray(index);
        } else {
            glDisableVertexAttribArray(index);
        }
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