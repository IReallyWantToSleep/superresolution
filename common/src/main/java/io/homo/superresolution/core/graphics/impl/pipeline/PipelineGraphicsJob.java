package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

import java.util.Objects;

public class PipelineGraphicsJob extends GpuComputeJob<PipelineGraphicsJob> implements IPipelineJob {
    protected final float[] viewport = new float[]{-1, -1, -1, -1};
    protected IFrameBuffer frameBuffer;
    protected IShaderProgram<?> program = null;

    public PipelineGraphicsJob viewport(float x, float y, float width, float height) {
        this.viewport[0] = x;
        this.viewport[1] = y;
        this.viewport[2] = width;
        this.viewport[3] = height;
        return this;
    }

    public PipelineGraphicsJob targetFrameBuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = Objects.requireNonNull(frameBuffer, "帧缓冲区不能为null");
        return this;
    }

    public PipelineGraphicsJob graphicsProgram(IShaderProgram<?> program) {
        this.program = Objects.requireNonNull(program, "图形着色器不能为null");
        return this;
    }

    @Override
    public void execute(ICommandBuffer commandBuffer) {
        Objects.requireNonNull(program, "图形着色器未设置");
        Objects.requireNonNull(frameBuffer, "帧缓冲区未设置");

        setupProgramResources(program);
        float[] lastViewport = commandBuffer.getEncoder().renderState().viewport();

        if (isViewportValid()) {
            commandBuffer.getEncoder().renderState().viewport(
                    viewport[0], viewport[1], viewport[2], viewport[3]
            );
        }

        commandBuffer.getEncoder().draw(
                commandBuffer,
                program,
                frameBuffer,
                DrawObject.fullscreenQuad(commandBuffer.getDevice()).once(),
                0,
                DrawObject.fullscreenQuadVertexCount()
        );

        commandBuffer.getEncoder().renderState().viewport(
                lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3]
        );
    }

    private boolean isViewportValid() {
        return viewport[0] >= 0 && viewport[1] >= 0
                && viewport[2] > 0 && viewport[3] > 0;
    }

    @Override
    public void destroy() {
        program = null;
        frameBuffer = null;
    }
}