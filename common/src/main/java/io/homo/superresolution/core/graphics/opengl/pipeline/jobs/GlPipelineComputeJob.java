package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Vec3;

import java.util.function.Supplier;

import static io.homo.superresolution.core.graphics.opengl.Gl.glDispatchCompute;
import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;

public class GlPipelineComputeJob extends GlPipelineJob {
    private final GlShaderProgram program;
    private Supplier<Vec3> workGroupSizeSupplier;

    public GlPipelineComputeJob(GlShaderProgram program, Supplier<Vec3> workGroupSizeSupplier) {
        this.program = program;
        this.workGroupSizeSupplier = workGroupSizeSupplier;
    }

    public GlPipelineComputeJob(GlShaderProgram program, Vec3 workGroupSize) {
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
        Gl.glUseProgram(program.handle);
        setupResource();
    }

    @Override
    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        Gl.glUseProgram(program.handle);
        Vec3 workGroupSize = workGroupSizeSupplier.get();
        glDispatchCompute(
                (int) workGroupSize.x,
                (int) workGroupSize.y,
                (int) workGroupSize.z
        );
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

}
