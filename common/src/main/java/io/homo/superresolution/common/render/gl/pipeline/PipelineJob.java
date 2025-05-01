package io.homo.superresolution.common.render.gl.pipeline;

import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.vertex.VertexBuffer;
import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.common.render.gl.vertex.VertexArray;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;


public class PipelineJob {
    protected PipelineResourceDescriptions resourcesMap;
    protected AbstractGlShaderProgram program;
    protected PipelineJobType type;
    protected GlPipeline pipeline;
    protected IFrameBuffer targetFrameBuffer;

    public static PipelineJobBuilder create() {
        return new PipelineJobBuilder();
    }

    public void setTargetFrameBuffer(IFrameBuffer frameBuffer) {
        if (this.type != PipelineJobType.Graphics) {
            throw new IllegalStateException("Only Graphics jobs can set FrameBuffer targets!");
        }
        this.targetFrameBuffer = frameBuffer;
    }

    public void bindPipeline(GlPipeline pipeline) {
        this.pipeline = pipeline;
    }

    protected void setupImage2DResource(PipelineResourceDescription description) {
        if (description.src() != null) {
            if (description.src().getTextureFormat() != null) {
                int access = switch (description.access()) {
                    case READ -> GL_READ_ONLY;
                    case WRITE -> GL_WRITE_ONLY;
                    case BOTH -> GL_READ_WRITE;
                };
                glBindImageTexture(
                        description.unit(),
                        description.src().getTextureId(),
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

    public PipelineResourceDescription getResource(String name) {
        return resourcesMap.resource.get(name);
    }

    protected void setupSampler2DResource(PipelineResourceDescription description) {
        if (description.src() != null) {
            int unit = description.unit();
            glBindTextureUnit(unit, description.src().getTextureId());
            if (description.sampler() != null) {
                glBindSampler(unit, description.sampler().id);
            }
        }
    }

    protected void setupResource() {
        resourcesMap.resource.forEach((name, description) -> {
            switch (description.type()) {
                case Image2D -> setupImage2DResource(description);
                case Sampler2D -> setupSampler2DResource(description);
            }
        });
    }

    public void scheduleGraphics(PipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    public void executeGraphics(PipelineJobDispatchResource dispatchResource) {
        try (GlState ignored = new GlState()) {
            if (targetFrameBuffer != null) {
                targetFrameBuffer.bind(FrameBufferBindPoint.WRITE, true);
            }
            program.use();
            try (VertexArray vao = new VertexArray();
                 VertexBuffer vbo = new VertexBuffer()) {
                float[] vertices = {
                        -1f, -1f, 0f, 0f,
                        1f, -1f, 1f, 0f,
                        1f, 1f, 1f, 1f,
                        -1f, 1f, 0f, 1f
                };
                vao.bind();
                vbo.bind(GL_ARRAY_BUFFER);
                vbo.uploadData(vertices, GL_STATIC_DRAW);
                int stride = 4 * Float.BYTES;
                glEnableVertexAttribArray(0);
                glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
                glEnableVertexAttribArray(1);
                glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            }
        }
    }

    public void scheduleCompute(PipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    public void executeCompute(PipelineJobDispatchResource dispatchResource) {
        glDispatchCompute(
                (int) dispatchResource.dimensions().x,
                (int) dispatchResource.dimensions().y,
                (int) dispatchResource.dimensions().z
        );
    }

    public void schedule(PipelineJobDispatchResource dispatchResource) {
        program.use();
        if (type == PipelineJobType.Graphics) {
            scheduleGraphics(dispatchResource);
        } else {
            scheduleCompute(dispatchResource);
        }
    }

    public void execute(PipelineJobDispatchResource dispatchResource) {
        program.use();
        if (type == PipelineJobType.Graphics) {
            executeGraphics(dispatchResource);
        } else {
            executeCompute(dispatchResource);
        }
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

    public static class PipelineJobBuilder {
        protected PipelineResourceDescriptions resourcesMap = new PipelineResourceDescriptions();
        protected AbstractGlShaderProgram program;
        protected PipelineJobType type;
        private IFrameBuffer targetFrameBuffer;

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

        public PipelineJobBuilder addResource(PipelineResourceDescription description) {
            resourcesMap.addResource(description);
            return this;
        }

        public PipelineJobBuilder setTargetFrameBuffer(IFrameBuffer frameBuffer) {
            if (this.type != PipelineJobType.Graphics) {
                throw new IllegalArgumentException("FrameBuffer can only be set for Graphics jobs!");
            }
            this.targetFrameBuffer = frameBuffer;
            return this;
        }

        public PipelineJob build() {
            if (program == null) {
                throw new IllegalStateException("必须指定着色器程序");
            }
            PipelineJob pipelineJob = new PipelineJob();
            pipelineJob.program = program;
            pipelineJob.resourcesMap = resourcesMap.clone();
            pipelineJob.type = type;
            if (targetFrameBuffer != null) {
                if (type != PipelineJobType.Graphics) {
                    throw new IllegalStateException("非图形任务不能设置FrameBuffer");
                }
                pipelineJob.setTargetFrameBuffer(targetFrameBuffer);
            }
            return pipelineJob;
        }
    }
}