package io.homo.superresolution.core.gl.pipeline.jobs;

import io.homo.superresolution.core.impl.texture.ITexture;

import static org.lwjgl.opengl.GL44.*;

public class GlPipelineClearJob extends GlPipelineJob {
    private final ITexture target;
    private final float[] clearColor;

    public GlPipelineClearJob(ITexture target, float[] clearColor) {
        this.target = target;
        this.clearColor = clearColor;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource resource) {
    }

    @Override
    public void execute(GlPipelineJobDispatchResource resource) {
        glClearTexImage(
                target.getTextureId(), 0,
                target.getTextureFormat().gl(),
                GL_FLOAT, clearColor
        );
    }
}
