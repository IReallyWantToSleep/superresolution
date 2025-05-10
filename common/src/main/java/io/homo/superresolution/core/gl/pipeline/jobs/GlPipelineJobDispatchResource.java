package io.homo.superresolution.core.gl.pipeline.jobs;

import io.homo.superresolution.core.impl.Vec3;

public record GlPipelineJobDispatchResource(
        Vec3 dimensions
) {
    public static GlPipelineJobDispatchResource nothing() {
        return new GlPipelineJobDispatchResource(new Vec3(0.0F));
    }
}
