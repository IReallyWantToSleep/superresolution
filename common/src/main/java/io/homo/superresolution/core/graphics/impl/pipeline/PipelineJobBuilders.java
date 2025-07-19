package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector3i;
import io.homo.superresolution.core.math.Vector4i;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PipelineJobBuilders {
    private PipelineJobBuilders() {
    }

    public static CopyTextureJobBuilder copyTexture() {
        return new CopyTextureJobBuilder();
    }

    public static ClearTextureJobBuilder clearTexture() {
        return new ClearTextureJobBuilder();
    }

    public static ComputeJobBuilder compute(IShaderProgram<?> program) {
        return new ComputeJobBuilder(program);
    }

    public static GraphicsJobBuilder graphics(IShaderProgram<?> program) {
        return new GraphicsJobBuilder(program);
    }

    public static CopyBufferJobBuilder copyBuffer() {
        return new CopyBufferJobBuilder();
    }

    public static class CopyTextureJobBuilder {
        private ITexture source;
        private ITexture destination;
        private Vector4i srcDimensions;
        private Vector4i dstDimensions;

        public CopyTextureJobBuilder from(ITexture source) {
            this.source = source;
            return this;
        }

        public CopyTextureJobBuilder to(ITexture destination) {
            this.destination = destination;
            return this;
        }

        public CopyTextureJobBuilder sourceDimensions(Vector4i src) {
            this.srcDimensions = src;
            return this;
        }

        public CopyTextureJobBuilder destinationDimensions(Vector4i dst) {
            this.dstDimensions = dst;
            return this;
        }

        public PipelineCopyTextureJob build() {
            if (source == null || destination == null) {
                throw new IllegalStateException("Source and destination textures must be specified.");
            }
            return new PipelineCopyTextureJob(source, destination, srcDimensions, dstDimensions);
        }
    }

    public static class ClearTextureJobBuilder {
        private ITexture target;
        private float[] color;
        private Float depth;
        private Integer stencil;

        public ClearTextureJobBuilder target(ITexture texture) {
            this.target = texture;
            return this;
        }

        public ClearTextureJobBuilder color(float r, float g, float b, float a) {
            this.color = new float[]{r, g, b, a};
            return this;
        }

        public ClearTextureJobBuilder depth(float depth) {
            this.depth = depth;
            return this;
        }

        public ClearTextureJobBuilder stencil(int stencil) {
            this.stencil = stencil;
            return this;
        }

        public PipelineClearTextureJob build() {
            if (target == null) throw new IllegalStateException("Clear target must be specified");
            PipelineClearTextureJob job = new PipelineClearTextureJob();
            job.clearTarget(target);
            if (color != null) job.clearColor(color[0], color[1], color[2], color[3]);
            if (depth != null) job.clearDepth(depth);
            if (stencil != null) job.clearStencil(stencil);
            return job;
        }
    }

    public static class ComputeJobBuilder extends GpuComputeJobBuilder<ComputeJobBuilder> {
        private final IShaderProgram<?> program;
        private Supplier<Vector3i> workGroupSizeSupplier = () -> new Vector3i(1, 1, 1);

        public ComputeJobBuilder(IShaderProgram<?> program) {
            if (program == null) throw new IllegalArgumentException("Compute program cannot be null");
            this.program = program;
        }

        public ComputeJobBuilder workGroup(int x, int y, int z) {
            this.workGroupSizeSupplier = () -> new Vector3i(x, y, z);
            return this;
        }

        public ComputeJobBuilder workGroupSupplier(Supplier<Vector3i> supplier) {
            this.workGroupSizeSupplier = supplier;
            return this;
        }

        public PipelineComputeJob build() {
            PipelineComputeJob job = new PipelineComputeJob();
            job.computeProgram(program);
            job.workGroupSizeSupplier(workGroupSizeSupplier);
            job.resources.putAll(resources);
            return job;
        }
    }

    public static class GraphicsJobBuilder extends GpuComputeJobBuilder<GraphicsJobBuilder> {
        private final IShaderProgram<?> program;
        private IFrameBuffer targetFbo;
        private float[] viewport = null;

        public GraphicsJobBuilder(IShaderProgram<?> program) {
            if (program == null) throw new IllegalArgumentException("Graphics program cannot be null");
            this.program = program;
        }

        public GraphicsJobBuilder targetFramebuffer(IFrameBuffer fbo) {
            this.targetFbo = fbo;
            return this;
        }

        public GraphicsJobBuilder viewport(float x, float y, float w, float h) {
            this.viewport = new float[]{x, y, w, h};
            return this;
        }

        public PipelineGraphicsJob build() {
            PipelineGraphicsJob job = new PipelineGraphicsJob();
            job.graphicsProgram(program);
            if (targetFbo != null) job.targetFrameBuffer(targetFbo);
            if (viewport != null) {
                job.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            } else if (targetFbo != null) {
                job.viewport(0, 0, targetFbo.getWidth(), targetFbo.getHeight());
            }
            job.resources.putAll(resources);
            return job;
        }
    }

    public static class CopyBufferJobBuilder {
        private IBuffer source;
        private IBuffer destination;
        private long srcOffset = -1, dstOffset = -1, size = -1;

        public CopyBufferJobBuilder from(IBuffer source) {
            this.source = source;
            return this;
        }

        public CopyBufferJobBuilder to(IBuffer destination) {
            this.destination = destination;
            return this;
        }

        public CopyBufferJobBuilder region(long srcOffset, long dstOffset, long size) {
            this.srcOffset = srcOffset;
            this.dstOffset = dstOffset;
            this.size = size;
            return this;
        }

        public PipelineCopyBufferJob build() {
            return new PipelineCopyBufferJob(source, destination, srcOffset, dstOffset, size);
        }
    }

    public static abstract class GpuComputeJobBuilder<SELF> {
        protected Map<String, PipelineJobResource<?>> resources = new HashMap<>();

        public SELF resource(String key, PipelineJobResource<?> resource) {
            resources.put(key, resource);
            return (SELF) this;
        }
    }
}