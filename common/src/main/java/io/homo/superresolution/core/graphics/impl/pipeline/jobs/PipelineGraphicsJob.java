package io.homo.superresolution.core.graphics.impl.pipeline.jobs;

import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;

public abstract class PipelineGraphicsJob {
    protected final float[] viewport = new float[4];
    protected IFrameBuffer frameBuffer;
    protected IShaderProgram<?> program = null;

    /**
     * 设置视口参数
     *
     * @param x      视口X坐标
     * @param y      视口Y坐标
     * @param width  视口宽度
     * @param height 视口高度
     */
    public PipelineGraphicsJob setViewport(float x, float y, float width, float height) {
        this.viewport[0] = x;
        this.viewport[1] = y;
        this.viewport[2] = width;
        this.viewport[3] = height;
        return this;
    }

    /**
     * 设置帧缓冲区目标
     *
     * @param frameBuffer 帧缓冲区对象
     */
    public PipelineGraphicsJob setTargetFrameBuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
        return this;
    }

    /**
     * 设置图形着色器
     *
     * @param program 图形着色器对象
     */
    public PipelineGraphicsJob setGraphicsProgram(IShaderProgram<?> program) {
        this.program = program;
        return this;
    }

}
