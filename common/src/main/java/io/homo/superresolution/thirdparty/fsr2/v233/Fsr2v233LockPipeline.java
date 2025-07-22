package io.homo.superresolution.thirdparty.fsr2.v233;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233LockPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v233LockPipeline(Fsr2Context context) {
        super(context);
    }

    @Override
    public void resize(Fsr2Dimensions size) {

    }


    @Override
    public void destroy() {
        program.destroy();
    }

    @Override
    public void init() {
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.COMPUTE, "/shader/fsr2v233/ffx_fsr2_lock_pass.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_lock")
                        .build()
        );
        program.compile();
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        GlPipelineJobBuilders.ComputeJobBuilder jobBuilder =
                GlPipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> new Vector3f(
                                (context.dimensions.renderWidth() + (7f)) / 8f,
                                (context.dimensions.renderHeight() + (7f)) / 8f,
                                1
                        ));

        jobBuilder.resource(
                GlPipelineResourceDescription.createUBOResource(
                        "cbFSR2",
                        context.fsr2ConstantsUBO,
                        3
                )
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_INPUT_LUMA)
                        .binding(0)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.NEW_LOCKS)
                        .binding(1)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(2)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_lock", jobBuilder.build());
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }

}
