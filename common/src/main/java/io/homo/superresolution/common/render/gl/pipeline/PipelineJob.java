package io.homo.superresolution.common.render.gl.pipeline;

import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;

import java.util.ArrayList;

public class PipelineJob {
    public PipelineJobBuilder create() {
        return new PipelineJobBuilder();
    }

    public static class PipelineJobBuilder {
        protected PipelineResourceDescriptions resourcesMap;
        protected AbstractGlShaderProgram program;

        public PipelineJobBuilder addResource(PipelineResourceDescriptions descriptions) {
            resourcesMap.resource.putAll(descriptions.resource);
            return this;
        }

        public PipelineJobBuilder addResource(PipelineResourceDescriptions.PipelineResourceDescription description) {
            resourcesMap.addResource(description);
            return this;
        }

        public PipelineJob build() {
            return new PipelineJob();
        }
    }
}
