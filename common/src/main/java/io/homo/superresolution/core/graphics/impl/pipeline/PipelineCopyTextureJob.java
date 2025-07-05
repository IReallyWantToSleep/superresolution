package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.math.Vector4f;
import io.homo.superresolution.core.math.Vector4i;

public class PipelineCopyTextureJob implements IPipelineJob {
    protected Vector4i sourceDimensions;
    protected Vector4i destinationDimensions;
    protected ITexture source;
    protected ITexture destination;

    protected PipelineCopyTextureJob(
            ITexture source,
            ITexture destination,
            Vector4i sourceDimensions,
            Vector4i destinationDimensions
    ) {
        this.sourceDimensions = sourceDimensions;
        this.destinationDimensions = destinationDimensions;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public void execute(IRenderSystem renderSystem) {
        renderSystem.copyTexture(
                source,
                destination,
                sourceDimensions.x, sourceDimensions.y,
                sourceDimensions.z, sourceDimensions.w,
                0,
                destinationDimensions.x, destinationDimensions.y,
                destinationDimensions.z, destinationDimensions.w,
                0
        );
    }

    @Override
    public void destroy() {

    }
}
