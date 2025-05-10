package io.homo.superresolution.core.gl.pipeline;

import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.gl.vertex.GlVertexArray;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;


public class GlPipelineJob {
    protected GlPipelineResourceDescriptions resourcesMap;
    protected AbstractGlShaderProgram program;
    protected GlPipelineJobType type;
    protected GlPipeline pipeline;
    protected IFrameBuffer targetFrameBuffer;

    public static GlPipelineJobBuilder create() {
        return new GlPipelineJobBuilder();
    }

    public void setTargetFrameBuffer(IFrameBuffer frameBuffer) {
        if (this.type != GlPipelineJobType.Graphics) {
            throw new IllegalStateException("Only Graphics jobs can set FrameBuffer targets!");
        }
        this.targetFrameBuffer = frameBuffer;
    }

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

    public GlPipelineResourceDescription getResource(String name) {
        return resourcesMap.resource.get(name);
    }

    protected void setupSampler2DResource(GlPipelineResourceDescription description) {
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

    public void scheduleGraphics(GlPipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    public void executeGraphics(GlPipelineJobDispatchResource dispatchResource) {
        try (GlState ignored = new GlState()) {
            if (targetFrameBuffer != null) {
                targetFrameBuffer.bind(FrameBufferBindPoint.WRITE, true);
            }
            program.use();
            try (GlVertexArray vao = new GlVertexArray();
                 GlVertexBuffer vbo = new GlVertexBuffer()) {
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

    public void scheduleCompute(GlPipelineJobDispatchResource dispatchResource) {
        setupResource();
    }

    public void executeCompute(GlPipelineJobDispatchResource dispatchResource) {
        glDispatchCompute(
                (int) dispatchResource.dimensions().x,
                (int) dispatchResource.dimensions().y,
                (int) dispatchResource.dimensions().z
        );
    }

    public void schedule(GlPipelineJobDispatchResource dispatchResource) {
        program.use();
        if (type == GlPipelineJobType.Graphics) {
            scheduleGraphics(dispatchResource);
        } else {
            scheduleCompute(dispatchResource);
        }
    }

    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        program.use();
        if (type == GlPipelineJobType.Graphics) {
            executeGraphics(dispatchResource);
        } else {
            executeCompute(dispatchResource);
        }
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

    public static class GlPipelineJobBuilder {
        protected GlPipelineResourceDescriptions resourcesMap = new GlPipelineResourceDescriptions();
        protected AbstractGlShaderProgram program;
        protected GlPipelineJobType type;
        private IFrameBuffer targetFrameBuffer;

        public GlPipelineJobBuilder setProgram(AbstractGlShaderProgram program) {
            this.program = program;
            return this;
        }

        public GlPipelineJobBuilder setType(GlPipelineJobType type) {
            this.type = type;
            return this;
        }

        public GlPipelineJobBuilder addResource(GlPipelineResourceDescriptions descriptions) {
            resourcesMap.resource.putAll(descriptions.resource);
            return this;
        }

        public GlPipelineJobBuilder addResource(GlPipelineResourceDescription description) {
            resourcesMap.addResource(description);
            return this;
        }

        public GlPipelineJobBuilder setTargetFrameBuffer(IFrameBuffer frameBuffer) {
            if (this.type != GlPipelineJobType.Graphics) {
                throw new IllegalArgumentException("FrameBuffer can only be set for Graphics jobs!");
            }
            this.targetFrameBuffer = frameBuffer;
            return this;
        }

        public GlPipelineJob build() {
            if (program == null) {
                throw new IllegalStateException("必须指定着色器程序");
            }
            GlPipelineJob pipelineJob = new GlPipelineJob();
            pipelineJob.program = program;
            pipelineJob.resourcesMap = resourcesMap.clone();
            pipelineJob.type = type;
            if (targetFrameBuffer != null) {
                if (type != GlPipelineJobType.Graphics) {
                    throw new IllegalStateException("非图形任务不能设置FrameBuffer");
                }
                pipelineJob.setTargetFrameBuffer(targetFrameBuffer);
            }
            return pipelineJob;
        }
    }
}