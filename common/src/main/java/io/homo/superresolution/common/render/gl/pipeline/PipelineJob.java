package io.homo.superresolution.common.render.gl.pipeline;

import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;

public class PipelineJob {
    protected PipelineResourceDescriptions resourcesMap;
    protected AbstractGlShaderProgram program;
    protected PipelineJobType type;

    public PipelineJobBuilder create() {
        return new PipelineJobBuilder();
    }

    protected void setupImage2DResource(PipelineResourceDescriptions.PipelineResourceDescription description) {

    }

    protected void setupSampler2DResource(PipelineResourceDescriptions.PipelineResourceDescription description) {
    }

    protected void setupResource() {
        resourcesMap.resource.forEach((name, description) -> {
            switch (description.type()) {
                case Image2D -> setupImage2DResource(description);
                case Sampler2D -> setupSampler2DResource(description);
            }
        });
    }

    public void scheduleGraphics() {

    }

    public void executeGraphics() {
    }

    public void scheduleCompute() {

    }

    public void executeCompute() {
    }

    public void schedule() {
        program.use();
        if (type == PipelineJobType.Graphics) {
            scheduleGraphics();
        } else {
            scheduleCompute();
        }
    }

    public void execute() {
        program.use();
        if (type == PipelineJobType.Graphics) {
            executeGraphics();
        } else {
            executeCompute();
        }
    }

    public static class PipelineJobBuilder {
        protected PipelineResourceDescriptions resourcesMap;
        protected AbstractGlShaderProgram program;
        protected PipelineJobType type;

        public PipelineJobBuilder setProgram(AbstractGlShaderProgram program) {
            this.program = program;
            return this;
        }

        public PipelineJobBuilder setType(PipelineJobType type) {
            this.type = type;
            return this;
        }

        public PipelineJobBuilder addResource(PipelineResourceDescriptions descriptions) {
            resourcesMap.resource.putAll(descriptions.resource);
            return this;
        }

        public PipelineJobBuilder addResource(PipelineResourceDescriptions.PipelineResourceDescription description) {
            resourcesMap.addResource(description);
            return this;
        }

        public PipelineJob build() {
            PipelineJob pipelineJob = new PipelineJob();
            pipelineJob.program = program;
            pipelineJob.resourcesMap = resourcesMap.clone();
            pipelineJob.type = type;
            return pipelineJob;
        }
    }
}
