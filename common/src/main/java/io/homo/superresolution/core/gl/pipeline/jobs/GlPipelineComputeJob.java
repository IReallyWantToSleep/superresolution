package io.homo.superresolution.core.gl.pipeline.jobs;

import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.impl.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.homo.superresolution.core.gl.Gl.glDispatchCompute;
import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;

public class GlPipelineComputeJob extends GlPipelineJob {
    private final AbstractGlShaderProgram program;
    private Supplier<Vec3> workGroupSizeSupplier;

    public GlPipelineComputeJob(AbstractGlShaderProgram program, Supplier<Vec3> workGroupSizeSupplier) {
        this.program = program;
        this.workGroupSizeSupplier = workGroupSizeSupplier;
    }

    public GlPipelineComputeJob(AbstractGlShaderProgram program, Vec3 workGroupSize) {
        this(program, () -> workGroupSize);
    }


    public void setWorkGroupSize(Vec3 workGroupSize) {
        this.workGroupSizeSupplier = () -> workGroupSize;
    }

    public void setWorkGroupSizeSupplier(Supplier<Vec3> workGroupSizeSupplier) {
        this.workGroupSizeSupplier = workGroupSizeSupplier;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource dispatchResource) {
        program.use();
        setupResource();
    }

    @Override
    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        program.use();
        Vec3 workGroupSize = workGroupSizeSupplier.get();
        glDispatchCompute(
                (int) workGroupSize.x,
                (int) workGroupSize.y,
                (int) workGroupSize.z
        );
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

}
