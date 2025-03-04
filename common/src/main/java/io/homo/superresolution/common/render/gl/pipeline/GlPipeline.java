package io.homo.superresolution.common.render.gl.pipeline;

import java.util.Map;

public class GlPipeline extends AbstractPipeline {
    public Map<String, PipelineResourceDescriptions.PipelineResourceDescription> resource;

    public GlPipeline create(PipelineDescription description) {
        return new GlPipeline();
    }

    public void scheduleJob() {

    }

    public void executeJob() {
    }

}
