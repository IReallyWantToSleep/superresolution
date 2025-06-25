package io.homo.superresolution.core.graphics.opengl.pipeline.jobs;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3f;

import java.util.function.Supplier;

import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;

public class GlPipelineComputeJob extends GlPipelineJob {
    private final GlShaderProgram program;
    private Supplier<Vector3f> workGroupSizeSupplier;

    public GlPipelineComputeJob(GlShaderProgram program, Supplier<Vector3f> workGroupSizeSupplier) {
        this.program = program;
        this.workGroupSizeSupplier = workGroupSizeSupplier;
    }

    public GlPipelineComputeJob(GlShaderProgram program, Vector3f workGroupSize) {
        this(program, () -> workGroupSize);
    }


    public void setWorkGroupSize(Vector3f workGroupSize) {
        this.workGroupSizeSupplier = () -> workGroupSize;
    }

    public void setWorkGroupSizeSupplier(Supplier<Vector3f> workGroupSizeSupplier) {
        this.workGroupSizeSupplier = workGroupSizeSupplier;
    }

    @Override
    public void schedule(GlPipelineJobDispatchResource dispatchResource) {
        try (GlState state = new GlState(GlState.STATE_PROGRAM)) {
            Gl.glUseProgram(program.handle());
            setupResource();
        }
    }

    @Override
    public void execute(GlPipelineJobDispatchResource dispatchResource) {
        Vector3f workGroupSize = workGroupSizeSupplier.get();
        RenderSystems.opengl().dispatchCompute(
                this.program,
                (int) workGroupSize.x,
                (int) workGroupSize.y,
                (int) workGroupSize.z
        );
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

}
