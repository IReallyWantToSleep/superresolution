package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescriptions;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

import java.util.function.Supplier;

public final class GlPipelineJobBuilders {
    private GlPipelineJobBuilders() {
    }

    public static CopyJobBuilder copy() {
        return new CopyJobBuilder();
    }

    public static ClearJobBuilder clear() {
        return new ClearJobBuilder();
    }

    public static ComputeJobBuilder compute(GlShaderProgram program) {
        return new ComputeJobBuilder(program);
    }

    public static GraphicsJobBuilder graphics(GlShaderProgram program) {
        return new GraphicsJobBuilder(program);
    }

    public static final class CopyJobBuilder {
        private ITexture source;
        private ITexture dest;

        private CopyJobBuilder() {
        }

        public CopyJobBuilder from(ITexture source) {
            this.source = source;
            return this;
        }

        public CopyJobBuilder to(ITexture dest) {
            this.dest = dest;
            return this;
        }

        public GlPipelineCopyJob build() {
            validate();
            return new GlPipelineCopyJob(source, dest);
        }

        private void validate() {
            if (source == null) {
                throw new IllegalStateException("Copy source must be specified");
            }
            if (dest == null) {
                throw new IllegalStateException("Copy destination must be specified");
            }
            if (source.getTextureFormat() != dest.getTextureFormat()) {
                throw new IllegalArgumentException(
                        "Source and destination texture formats must match. Source: "
                                + source.getTextureFormat() + ", Dest: " + dest.getTextureFormat()
                );
            }
        }
    }

    public static final class ClearJobBuilder {
        private ITexture target;
        private float[] color = new float[]{0, 0, 0, 0};

        private ClearJobBuilder() {
        }

        public ClearJobBuilder target(ITexture texture) {
            this.target = texture;
            return this;
        }

        public ClearJobBuilder color(float r, float g, float b, float a) {
            this.color = new float[]{r, g, b, a};
            return this;
        }

        public GlPipelineClearJob build() {
            validate();
            return new GlPipelineClearJob(target, color);
        }

        private void validate() {
            if (target == null) {
                throw new IllegalStateException("Clear target must be specified");
            }
            if (color.length != 4) {
                throw new IllegalArgumentException("Clear color must have 4 components");
            }
        }
    }

    public static final class ComputeJobBuilder {
        private final GlShaderProgram program;
        private final GlPipelineResourceDescriptions resources = new GlPipelineResourceDescriptions();
        private Supplier<Vec3> workGroupSizeSupplier = () -> new Vec3(1, 1, 1);

        private ComputeJobBuilder(GlShaderProgram program) {
            if (program == null) {
                throw new IllegalArgumentException("Compute shader program cannot be null");
            }
            this.program = program;
        }

        public ComputeJobBuilder workGroup(int x, int y, int z) {
            this.workGroupSizeSupplier = () -> new Vec3(x, y, z);
            return this;
        }

        public ComputeJobBuilder workGroupSupplier(Supplier<Vec3> workGroupSizeSupplier) {
            this.workGroupSizeSupplier = workGroupSizeSupplier;
            return this;
        }

        public ComputeJobBuilder resource(GlPipelineResourceDescription resource) {
            resources.addResource(resource);
            return this;
        }

        public GlPipelineComputeJob build() {
            GlPipelineComputeJob job = new GlPipelineComputeJob(program, workGroupSizeSupplier);
            job.resourcesMap = resources.clone();
            return job;
        }
    }

    public static final class GraphicsJobBuilder {
        private final GlShaderProgram program;
        private final GlPipelineResourceDescriptions resources = new GlPipelineResourceDescriptions();
        private IFrameBuffer targetFbo;

        private GraphicsJobBuilder(GlShaderProgram program) {
            if (program == null) {
                throw new IllegalArgumentException("Graphics shader program cannot be null");
            }
            this.program = program;
        }

        public GraphicsJobBuilder targetFramebuffer(IFrameBuffer fbo) {
            this.targetFbo = fbo;
            return this;
        }

        public GraphicsJobBuilder resource(GlPipelineResourceDescription resource) {
            resources.addResource(resource);
            return this;
        }

        public GlPipelineGraphicsJob build() {
            GlPipelineGraphicsJob job = new GlPipelineGraphicsJob();
            job.program = program;
            job.targetFrameBuffer = targetFbo;
            job.resourcesMap = resources.clone();
            return job;
        }
    }
}