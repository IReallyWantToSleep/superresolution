package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

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
        RenderSystems.opengl().clearTextureRGBA(this.target, clearColor);
    }
}
