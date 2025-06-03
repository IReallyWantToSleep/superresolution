package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import static org.lwjgl.opengl.GL43.*;

public class GlPipelineCopyJob extends GlPipelineJob {
    private final ITexture source;
    private final ITexture dest;

    public GlPipelineCopyJob(ITexture source, ITexture dest) {
        this.source = source;
        this.dest = dest;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource resource) {
    }

    @Override
    public void execute(GlPipelineJobDispatchResource resource) {
        glCopyImageSubData(
                source.getTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0,
                dest.getTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0,
                source.getWidth(), source.getHeight(), 1
        );
    }
}
