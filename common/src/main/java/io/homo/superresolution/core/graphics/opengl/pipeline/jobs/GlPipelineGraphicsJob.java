package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.RenderSystems;

public class GlPipelineGraphicsJob extends GlPipelineJob {
    public IFrameBuffer targetFrameBuffer;
    public GlShaderProgram program;

    public void setTargetFrameBuffer(IFrameBuffer frameBuffer) {
        if (this.type != GlPipelineJobType.Graphics) {
            throw new IllegalStateException("Only Graphics jobs can set FrameBuffer targets!");
        }
        this.targetFrameBuffer = frameBuffer;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    @Override
    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        try (GlState ignored = new GlState(
                GlState.STATE_TEXTURE |
                        GlState.STATE_ACTIVE_TEXTURE |
                        GlState.STATE_TEXTURES |
                        GlState.STATE_PROGRAM |
                        GlState.STATE_VERTEX_OPERATIONS |
                        GlState.STATE_DRAW_FBO |
                        GlState.STATE_READ_FBO
        )) {

            RenderSystems.current().draw(
                    program,
                    targetFrameBuffer,
                    DrawObject.fullscreenQuad(RenderSystems.current()).once(),
                    0,
                    DrawObject.fullscreenQuadVertexCount()
            );

        }
    }
}