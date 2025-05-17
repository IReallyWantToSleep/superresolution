package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.fsr2.*;

import java.util.HashMap;

public class Fsr2AccumulateSharpenPipeline extends Fsr2BasePipeline {
    private GlComputeShaderProgram program;

    public Fsr2AccumulateSharpenPipeline(Fsr2Context context) {
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
        HashMap<String, String> shaderDefines = new HashMap<>();
        shaderDefines.put("FFX_FSR2_OPTION_APPLY_SHARPENING", "1");
        program = GlComputeShaderProgram.create()
                .addDefineText(getShaderDefines(shaderDefines))
                .setShaderName("fsr2_accumulate_sharpen")
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/fsr2/ffx_fsr2_accumulate_pass.ogl.glsl", true))
                .build()
                .compileShader();
        GlPipelineJobBuilders.ComputeJobBuilder jobBuilder =
                GlPipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> new Vec3(
                                (float) (context.dimensions.screenWidth() + (7)) / 8,
                                (float) (context.dimensions.screenHeight() + (7)) / 8,
                                1
                        ));
        jobBuilder.resource(
                GlPipelineResourceDescription.createUBOResource(
                        "cbFSR2",
                        context.fsr2ConstantsUBO,
                        18
                )
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(13)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS)
                        .binding(14)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(
                                context.config.flags.isEnableDisplayResolutionMotionVectors() ?
                                        Fsr2PipelineResourceType.INPUT_MOTION_VECTORS :
                                        Fsr2PipelineResourceType.DILATED_MOTION_VECTORS
                        )
                        .binding(15)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR)
                        .binding(16)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_STATUS)
                        .binding(17)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(6)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LANCZOS_LUT)
                        .binding(8)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.UPSAMPLE_MAXIMUM_BIAS_LUT)
                        .binding(9)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE)
                        .binding(10)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.AUTO_EXPOSURE)
                        .binding(11)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LUMA_HISTORY)
                        .binding(12)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR)
                        .binding(0)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_STATUS)
                        .binding(1)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.UPSCALED_OUTPUT)
                        .binding(2)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.NEW_LOCKS)
                        .binding(3)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LUMA_HISTORY)
                        .binding(4)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_accumulate_sharpen", jobBuilder.build());
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {

    }

}
