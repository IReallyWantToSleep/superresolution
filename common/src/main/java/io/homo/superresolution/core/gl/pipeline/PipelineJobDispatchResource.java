package io.homo.superresolution.core.gl.pipeline;

import io.homo.superresolution.core.impl.Vec3;

public record PipelineJobDispatchResource(
        Vec3 dimensions
) {
    public static PipelineJobDispatchResource nothing() {
        return new PipelineJobDispatchResource(new Vec3(0.0F));
    }
}
