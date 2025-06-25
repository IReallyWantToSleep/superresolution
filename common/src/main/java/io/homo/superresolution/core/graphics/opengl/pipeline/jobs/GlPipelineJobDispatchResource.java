package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.math.Vector3f;

public record GlPipelineJobDispatchResource(
        Vector3f dimensions
) {
    public static GlPipelineJobDispatchResource nothing() {
        return new GlPipelineJobDispatchResource(new Vector3f(0.0F));
    }
}
