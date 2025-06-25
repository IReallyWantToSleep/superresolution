package io.homo.superresolution.core.graphics.impl.pipeline.jobs;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public abstract class PipelineClearTextureJob implements IPipelineJob {
    protected final float[] clearColor = new float[]{0, 0, 0, 1};
    protected float clearDepth = 0;
    protected int clearStencil = 0;
    protected ITexture target = null;

    /**
     * 设置清除颜色
     *
     * @param r 红色分量 (0.0-1.0)
     * @param g 绿色分量 (0.0-1.0)
     * @param b 蓝色分量 (0.0-1.0)
     * @param a 透明度分量 (0.0-1.0)
     */
    public PipelineClearTextureJob clearColor(float r, float g, float b, float a) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
        clearColor[3] = a;
        return this;

    }

    /**
     * 设置清除深度值
     *
     * @param depth 深度值 (0.0-1.0)
     */
    public PipelineClearTextureJob clearDepth(float depth) {
        this.clearDepth = depth;
        return this;

    }

    /**
     * 设置清除模板值
     *
     * @param stencil 模板值 (0-255)
     */
    public PipelineClearTextureJob clearStencil(int stencil) {
        this.clearStencil = stencil;
        return this;

    }

    /**
     * 设置清除目标
     *
     * @param target 清除目标对象
     */
    public PipelineClearTextureJob clearTarget(ITexture target) {
        this.target = target;
        return this;
    }
}
