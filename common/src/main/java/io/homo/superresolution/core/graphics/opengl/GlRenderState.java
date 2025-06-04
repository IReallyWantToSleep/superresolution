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
    private BlendFactor blendSrcFactor = BlendFactor.ONE;
    private BlendFactor blendDstFactor = BlendFactor.ZERO;
    private DepthFunc depthFunc = DepthFunc.LESS;

    private int toGlBlendFactor(BlendFactor bf) {
        switch (bf) {
            case ZERO:
                return GL11.GL_ZERO;
            case ONE:
                return GL11.GL_ONE;
            case SRC_COLOR:
                return GL11.GL_SRC_COLOR;
            case ONE_MINUS_SRC_COLOR:
                return GL11.GL_ONE_MINUS_SRC_COLOR;
            case DST_COLOR:
                return GL11.GL_DST_COLOR;
            case ONE_MINUS_DST_COLOR:
                return GL11.GL_ONE_MINUS_DST_COLOR;
            case SRC_ALPHA:
                return GL11.GL_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA:
                return GL11.GL_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA:
                return GL11.GL_ONE_MINUS_DST_ALPHA;
            default:
                return GL11.GL_ONE;
        }
    }

    private int toGlDepthFunc(DepthFunc func) {
        switch (func) {
            case NEVER:
                return GL11.GL_NEVER;
            case LESS:
                return GL11.GL_LESS;
            case EQUAL:
                return GL11.GL_EQUAL;
            case LESS_EQUAL:
                return GL11.GL_LEQUAL;
            case GREATER:
                return GL11.GL_GREATER;
            case NOT_EQUAL:
                return GL11.GL_NOTEQUAL;
            case GREATER_EQUAL:
                return GL11.GL_GEQUAL;
            case ALWAYS:
                return GL11.GL_ALWAYS;
            default:
                return GL11.GL_LESS;
        }
    }

    @Override
    public float[] viewport() {
        return viewport.clone();
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
        return depthTest;
    }

    @Override
    public boolean depthWrite() {
        return depthWrite;
    }

    @Override
    public boolean blend() {
        return blend;
    }

    @Override
    public boolean cullFace() {
        return cullFace;
    }

    @Override
    public boolean stencilTest() {
        return stencilTest;
    }

    @Override
    public boolean colorMaskR() {
        return colorMaskR;
    }

    @Override
    public boolean colorMaskG() {
        return colorMaskG;
    }

    @Override
    public boolean colorMaskB() {
        return colorMaskB;
    }

    @Override
    public boolean colorMaskA() {
        return colorMaskA;
    }

    @Override
    public BlendFactor blendSrcFactor() {
        return blendSrcFactor;
    }

    @Override
    public BlendFactor blendDstFactor() {
        return blendDstFactor;
    }

    @Override
    public DepthFunc depthFunc() {
        return depthFunc;
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
    public IRenderState save() {
        snapshotStack.push(new StateSnapshot(this));
        return this;
    }

    @Override
    public void restore() {
        if (!snapshotStack.isEmpty()) {
            StateSnapshot snap = snapshotStack.pop();
            this.depthTest(snap.depthTest);
            this.depthWrite(snap.depthWrite);
            this.blend(snap.blend);
            this.cullFace(snap.cullFace);
            this.stencilTest(snap.stencilTest);
            this.colorMaskR(snap.colorMaskR);
            this.colorMaskG(snap.colorMaskG);
            this.colorMaskB(snap.colorMaskB);
            this.colorMaskA(snap.colorMaskA);
            this.blendSrcFactor(snap.blendSrcFactor);
            this.blendDstFactor(snap.blendDstFactor);
            this.depthFunc(snap.depthFunc);
        }
    }
}