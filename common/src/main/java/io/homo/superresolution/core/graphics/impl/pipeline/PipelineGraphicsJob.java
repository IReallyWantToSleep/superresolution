package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

public class PipelineGraphicsJob implements IPipelineJob {
    protected final float[] viewport = new float[]{-1, -1, -1, -1};
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
    public PipelineGraphicsJob viewport(float x, float y, float width, float height) {
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
    public PipelineGraphicsJob targetFrameBuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
        return this;
    }

    /**
     * 设置图形着色器
     *
     * @param program 图形着色器对象
     */
    public PipelineGraphicsJob graphicsProgram(IShaderProgram<?> program) {
        this.program = program;
        return this;
    }

    @Override
    public void execute(IRenderSystem renderSystem) {
        renderSystem.renderState().save();
        if (viewport[0] > -1 && viewport[1] > -1 && viewport[2] > -1 && viewport[3] > -1
        ) {
            renderSystem.renderState().viewport(
                    viewport[0],
                    viewport[1],
                    viewport[2],
                    viewport[3]
            );
        }
        renderSystem.draw(
                program,
                frameBuffer,
                DrawObject.fullscreenQuad(renderSystem),
                0,
                DrawObject.fullscreenQuadVertexCount()
        );
        renderSystem.renderState().restore();
    }

    @Override
    public void destroy() {

    }
}
