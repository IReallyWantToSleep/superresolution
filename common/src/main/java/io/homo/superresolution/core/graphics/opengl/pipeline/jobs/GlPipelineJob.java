package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescriptions;

import static io.homo.superresolution.core.graphics.opengl.Gl.*;
import static io.homo.superresolution.core.graphics.opengl.GlConst.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;


public abstract class GlPipelineJob {
    public GlPipelineResourceDescriptions resourcesMap;
    public GlPipelineJobType type;
    public GlPipeline pipeline;

    public void bindPipeline(GlPipeline pipeline) {
        this.pipeline = pipeline;
    }

    protected void setupImage2DResource(GlPipelineResourceDescription description) {
        if (description.src() != null) {
            if (description.src().getTextureFormat() != null) {
                int access = switch (description.access()) {
                    case READ -> GL_READ_ONLY;
                    case WRITE -> GL_WRITE_ONLY;
                    case BOTH -> GL_READ_WRITE;
                };
                glBindImageTexture(
                        description.unit(),
                        description.src().handle(),
                        0,
                        false,
                        0,
                        access,
                        description.src().getTextureFormat().gl()
                );
            } else {
                throw new NullPointerException("资源描述的纹理不为空值但纹理格式为空值" + description);
            }
        } else {
            throw new NullPointerException("资源描述的纹理为空值 " + description);
        }
    }

    public GlPipelineResourceDescription getResource(String name) {
        return resourcesMap.resource.get(name);
    }

    protected void setupSampler2DResource(GlPipelineResourceDescription description) {
        if (description.src() != null) {
            int unit = description.unit();
            glBindTextureUnit(unit, description.src().handle());
            if (description.sampler() != null) {
                glBindSampler(unit, description.sampler().id);
            }
        }
    }

    protected void setupUniformBufferResource(GlPipelineResourceDescription description) {
        if (description.ubo() != null) {
            int unit = description.unit();
            description.ubo().bind(unit);
        }
    }

    protected void setupResource() {

        resourcesMap.resource.forEach((name, description) -> {
            switch (description.type()) {
                case Image2D -> setupImage2DResource(description);
                case Sampler2D -> setupSampler2DResource(description);
                case UniformBuffer -> setupUniformBufferResource(description);
            }
        });

    }

    public abstract void schedule(GlPipelineJobDispatchResource dispatchResource);

    public abstract void execute(GlPipelineJobDispatchResource dispatchResource);

}