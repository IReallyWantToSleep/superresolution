/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.BlendFactor;
import io.homo.superresolution.core.graphics.impl.DepthFunc;
import io.homo.superresolution.core.graphics.system.IRenderState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Stack;

public class GlRenderState implements IRenderState {
    private final Stack<StateSnapshot> snapshotStack = new Stack<>();
    private final float[] viewport = new float[4];
    private boolean depthTest = false;
    private boolean depthWrite = true;
    private boolean blend = false;
    private boolean cullFace = false;
    private boolean stencilTest = false;
    private boolean colorMaskR = true;
    private boolean colorMaskG = true;
    private boolean colorMaskB = true;
    private boolean colorMaskA = true;
    private BlendFactor blendSrcFactor = BlendFactor.One;
    private BlendFactor blendDstFactor = BlendFactor.Zero;
    private DepthFunc depthFunc = DepthFunc.Less;


    public void updateFromOpenGL() {
        depthTest = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        blend = GL11.glGetBoolean(GL11.GL_BLEND);
        cullFace = GL11.glGetBoolean(GL11.GL_CULL_FACE);
        stencilTest = GL11.glGetBoolean(GL11.GL_STENCIL_TEST);
        int[] colorMask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
        colorMaskR = colorMask[0] == 1;
        colorMaskG = colorMask[1] == 1;
        colorMaskB = colorMask[2] == 1;
        colorMaskA = colorMask[3] == 1;
        int srcRGB = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int dstRGB = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcFactor = fromGlBlendFactor(srcRGB);
        blendDstFactor = fromGlBlendFactor(dstRGB);
        int depthFuncValue = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        depthFunc = fromGlDepthFunc(depthFuncValue);
        int[] viewportInt = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportInt);
        viewport[0] = viewportInt[0];
        viewport[1] = viewportInt[1];
        viewport[2] = viewportInt[2];
        viewport[3] = viewportInt[3];
    }


    private BlendFactor fromGlBlendFactor(int glFactor) {
        return switch (glFactor) {
            case GL11.GL_ZERO -> BlendFactor.Zero;
            case GL11.GL_ONE -> BlendFactor.One;
            case GL11.GL_SRC_COLOR -> BlendFactor.SrcColor;
            case GL11.GL_ONE_MINUS_SRC_COLOR -> BlendFactor.OneMinusSrcColor;
            case GL11.GL_DST_COLOR -> BlendFactor.DstColor;
            case GL11.GL_ONE_MINUS_DST_COLOR -> BlendFactor.OneMinusDstColor;
            case GL11.GL_SRC_ALPHA -> BlendFactor.SrcAlpha;
            case GL11.GL_ONE_MINUS_SRC_ALPHA -> BlendFactor.OneMinusSrcAlpha;
            case GL11.GL_DST_ALPHA -> BlendFactor.DstAlpha;
            case GL11.GL_ONE_MINUS_DST_ALPHA -> BlendFactor.OneMinusDstAlpha;
            default -> BlendFactor.One;
        };
    }


    private DepthFunc fromGlDepthFunc(int glFunc) {
        return switch (glFunc) {
            case GL11.GL_NEVER -> DepthFunc.Never;
            case GL11.GL_LESS -> DepthFunc.Less;
            case GL11.GL_EQUAL -> DepthFunc.Equal;
            case GL11.GL_LEQUAL -> DepthFunc.LessEqual;
            case GL11.GL_GREATER -> DepthFunc.Greater;
            case GL11.GL_NOTEQUAL -> DepthFunc.NotEqual;
            case GL11.GL_GEQUAL -> DepthFunc.GreaterEqual;
            case GL11.GL_ALWAYS -> DepthFunc.Always;
            default -> DepthFunc.Less;
        };
    }

    private int toGlBlendFactor(BlendFactor bf) {
        return switch (bf) {
            case Zero -> GL11.GL_ZERO;
            case One -> GL11.GL_ONE;
            case SrcColor -> GL11.GL_SRC_COLOR;
            case OneMinusSrcColor -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case DstColor -> GL11.GL_DST_COLOR;
            case OneMinusDstColor -> GL11.GL_ONE_MINUS_DST_COLOR;
            case SrcAlpha -> GL11.GL_SRC_ALPHA;
            case OneMinusSrcAlpha -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case DstAlpha -> GL11.GL_DST_ALPHA;
            case OneMinusDstAlpha -> GL11.GL_ONE_MINUS_DST_ALPHA;
            default -> GL11.GL_ONE;
        };
    }

    private int toGlDepthFunc(DepthFunc func) {
        return switch (func) {
            case Never -> GL11.GL_NEVER;
            case Less -> GL11.GL_LESS;
            case Equal -> GL11.GL_EQUAL;
            case LessEqual -> GL11.GL_LEQUAL;
            case Greater -> GL11.GL_GREATER;
            case NotEqual -> GL11.GL_NOTEQUAL;
            case GreaterEqual -> GL11.GL_GEQUAL;
            case Always -> GL11.GL_ALWAYS;
            default -> GL11.GL_LESS;
        };
    }

    @Override
    public float[] viewport() {

        int[] viewportInt = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportInt);
        return new float[]{
                viewportInt[0], viewportInt[1], viewportInt[2], viewportInt[3]
        };
    }

    @Override
    public IRenderState viewport(float x, float y, float w, float h) {
        this.viewport[0] = x;
        this.viewport[1] = y;
        this.viewport[2] = w;
        this.viewport[3] = h;
        GL11.glViewport((int) x, (int) y, (int) w, (int) h);
        return this;
    }

    @Override
    public boolean depthTest() {
        return GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
    }

    @Override
    public boolean depthWrite() {
        return GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    }

    @Override
    public boolean blend() {
        return GL11.glGetBoolean(GL11.GL_BLEND);
    }

    @Override
    public boolean cullFace() {
        return GL11.glGetBoolean(GL11.GL_CULL_FACE);
    }

    @Override
    public boolean stencilTest() {
        return GL11.glGetBoolean(GL11.GL_STENCIL_TEST);
    }

    @Override
    public boolean colorMaskR() {
        int[] mask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, mask);
        return mask[0] == 1;
    }

    @Override
    public boolean colorMaskG() {
        int[] mask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, mask);
        return mask[1] == 1;
    }

    @Override
    public boolean colorMaskB() {
        int[] mask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, mask);
        return mask[2] == 1;
    }

    @Override
    public boolean colorMaskA() {
        int[] mask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, mask);
        return mask[3] == 1;
    }

    @Override
    public BlendFactor blendSrcFactor() {

        int srcFactor = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        return fromGlBlendFactor(srcFactor);
    }

    @Override
    public BlendFactor blendDstFactor() {

        int dstFactor = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        return fromGlBlendFactor(dstFactor);
    }

    @Override
    public DepthFunc depthFunc() {

        int depthFuncValue = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        return fromGlDepthFunc(depthFuncValue);
    }


    @Override
    public IRenderState depthTest(boolean enable) {
        this.depthTest = enable;
        if (enable) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        return this;
    }

    @Override
    public IRenderState depthWrite(boolean enable) {
        this.depthWrite = enable;
        GL11.glDepthMask(enable);
        return this;
    }

    @Override
    public IRenderState blend(boolean enable) {
        this.blend = enable;
        if (enable) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        return this;
    }

    @Override
    public IRenderState cullFace(boolean enable) {
        this.cullFace = enable;
        if (enable) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        return this;
    }

    @Override
    public IRenderState stencilTest(boolean enable) {
        this.stencilTest = enable;
        if (enable) {
            GL11.glEnable(GL11.GL_STENCIL_TEST);
        } else {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
        return this;
    }

    @Override
    public IRenderState colorMaskR(boolean enable) {
        this.colorMaskR = enable;
        GL11.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
        return this;
    }

    @Override
    public IRenderState colorMaskG(boolean enable) {
        this.colorMaskG = enable;
        GL11.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
        return this;
    }

    @Override
    public IRenderState colorMaskB(boolean enable) {
        this.colorMaskB = enable;
        GL11.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
        return this;
    }

    @Override
    public IRenderState colorMaskA(boolean enable) {
        this.colorMaskA = enable;
        GL11.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
        return this;
    }

    @Override
    public IRenderState blendSrcFactor(BlendFactor factor) {
        this.blendSrcFactor = factor;
        GL14.glBlendFunc(toGlBlendFactor(blendSrcFactor), toGlBlendFactor(blendDstFactor));
        return this;
    }

    @Override
    public IRenderState blendDstFactor(BlendFactor factor) {
        this.blendDstFactor = factor;
        GL14.glBlendFunc(toGlBlendFactor(blendSrcFactor), toGlBlendFactor(blendDstFactor));
        return this;
    }

    @Override
    public IRenderState depthFunc(DepthFunc func) {
        this.depthFunc = func;
        GL11.glDepthFunc(toGlDepthFunc(func));
        return this;
    }

    @Override
    public StateSnapshot get() {
        return new StateSnapshot(this);
    }
}