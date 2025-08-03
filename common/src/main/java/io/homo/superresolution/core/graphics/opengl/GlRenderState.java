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
            case GL11.GL_ZERO -> BlendFactor.ZERO;
            case GL11.GL_ONE -> BlendFactor.ONE;
            case GL11.GL_SRC_COLOR -> BlendFactor.SRC_COLOR;
            case GL11.GL_ONE_MINUS_SRC_COLOR -> BlendFactor.ONE_MINUS_SRC_COLOR;
            case GL11.GL_DST_COLOR -> BlendFactor.DST_COLOR;
            case GL11.GL_ONE_MINUS_DST_COLOR -> BlendFactor.ONE_MINUS_DST_COLOR;
            case GL11.GL_SRC_ALPHA -> BlendFactor.SRC_ALPHA;
            case GL11.GL_ONE_MINUS_SRC_ALPHA -> BlendFactor.ONE_MINUS_SRC_ALPHA;
            case GL11.GL_DST_ALPHA -> BlendFactor.DST_ALPHA;
            case GL11.GL_ONE_MINUS_DST_ALPHA -> BlendFactor.ONE_MINUS_DST_ALPHA;
            default -> BlendFactor.ONE;
        };
    }


    private DepthFunc fromGlDepthFunc(int glFunc) {
        return switch (glFunc) {
            case GL11.GL_NEVER -> DepthFunc.NEVER;
            case GL11.GL_LESS -> DepthFunc.LESS;
            case GL11.GL_EQUAL -> DepthFunc.EQUAL;
            case GL11.GL_LEQUAL -> DepthFunc.LESS_EQUAL;
            case GL11.GL_GREATER -> DepthFunc.GREATER;
            case GL11.GL_NOTEQUAL -> DepthFunc.NOT_EQUAL;
            case GL11.GL_GEQUAL -> DepthFunc.GREATER_EQUAL;
            case GL11.GL_ALWAYS -> DepthFunc.ALWAYS;
            default -> DepthFunc.LESS;
        };
    }

    private int toGlBlendFactor(BlendFactor bf) {
        return switch (bf) {
            case ZERO -> GL11.GL_ZERO;
            case ONE -> GL11.GL_ONE;
            case SRC_COLOR -> GL11.GL_SRC_COLOR;
            case ONE_MINUS_SRC_COLOR -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case DST_COLOR -> GL11.GL_DST_COLOR;
            case ONE_MINUS_DST_COLOR -> GL11.GL_ONE_MINUS_DST_COLOR;
            case SRC_ALPHA -> GL11.GL_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> GL11.GL_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> GL11.GL_ONE_MINUS_DST_ALPHA;
            default -> GL11.GL_ONE;
        };
    }

    private int toGlDepthFunc(DepthFunc func) {
        return switch (func) {
            case NEVER -> GL11.GL_NEVER;
            case LESS -> GL11.GL_LESS;
            case EQUAL -> GL11.GL_EQUAL;
            case LESS_EQUAL -> GL11.GL_LEQUAL;
            case GREATER -> GL11.GL_GREATER;
            case NOT_EQUAL -> GL11.GL_NOTEQUAL;
            case GREATER_EQUAL -> GL11.GL_GEQUAL;
            case ALWAYS -> GL11.GL_ALWAYS;
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