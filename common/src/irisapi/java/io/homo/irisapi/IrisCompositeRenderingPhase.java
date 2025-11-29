package io.homo.irisapi;

import io.homo.irisapi.mixin.composite.IrisRenderingPipelineAccessor;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;

public enum IrisCompositeRenderingPhase {
    Begin,
    Prepare,
    Deferred,
    Composite,
    Unknown;

    public static IrisCompositeRenderingPhase from(WorldRenderingPipeline pipeline, CompositeRenderer compositeRenderer) {
        if (!(pipeline instanceof IrisRenderingPipeline)) return Unknown;
        if (((IrisRenderingPipelineAccessor) pipeline).getCompositeRenderer() == compositeRenderer) {
            return Composite;
        } else if (((IrisRenderingPipelineAccessor) pipeline).getDeferredRenderer() == compositeRenderer) {
            return Deferred;
        } else if (((IrisRenderingPipelineAccessor) pipeline).getPrepareRenderer() == compositeRenderer) {
            return Prepare;
        } else if (((IrisRenderingPipelineAccessor) pipeline).getBeginRenderer() == compositeRenderer) {
            return Begin;
        }
        return Unknown;
    }
}
