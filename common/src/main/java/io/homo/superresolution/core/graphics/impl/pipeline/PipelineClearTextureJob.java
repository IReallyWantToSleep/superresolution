package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

public class PipelineClearTextureJob implements IPipelineJob {
    protected float[] clearColor = null;
    protected float clearDepth = -1;
    protected int clearStencil = -1;
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
        clearColor = new float[]{0, 0, 0, 0};
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

    @Override
    public void execute(IRenderSystem renderSystem) {
        if (
                clearColor != null &&
                        clearColor.length == target.getTextureFormat().getChannelCount()
        ) {
            renderSystem.clearTextureRGBA(target, clearColor);
        } else if (clearDepth >= 0 && clearDepth <= 1 && target.getTextureFormat().isDepth()) {
            renderSystem.clearTextureDepth(target, clearDepth);
        } else if (clearStencil >= 0 && clearStencil <= 255 && target.getTextureFormat().isStencil()) {
            renderSystem.clearTextureStencil(target, clearStencil);
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public void destroy() {
        clearColor = null;
    }
}
