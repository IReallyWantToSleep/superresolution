package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.fsr2.*;

public class Fsr2RCASPipeline extends Fsr2BasePipeline {
    private GlComputeShaderProgram program;

    public Fsr2RCASPipeline(Fsr2Context context) {
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
        program = GlComputeShaderProgram.create()
                .addDefineText(getShaderDefines(null))
                .setShaderName("fsr2_rcas")
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/fsr2/ffx_fsr2_rcas_pass.ogl.glsl", true))
                .build()
                .compileShader();

    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        GlPipelineJobBuilders.ComputeJobBuilder jobBuilder =
                GlPipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> new Vec3(
                                (context.dimensions.screenWidth() + (15f)) / 16f,
                                (context.dimensions.screenHeight() + (15f)) / 16f,
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
                GlPipelineResourceDescription.createUBOResource(
                        "cbRCAS",
                        context.fsr2RcasConstantsUBO,
                        4
                )
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(0)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1 :
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2
                        )
                        .binding(1)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.UPSCALED_OUTPUT)
                        .binding(2)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_rcas", jobBuilder.build());
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }


}
