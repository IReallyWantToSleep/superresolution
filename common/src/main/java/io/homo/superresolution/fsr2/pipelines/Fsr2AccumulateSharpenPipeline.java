package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GpuVendor;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.fsr2.*;

import java.util.HashMap;

public class Fsr2AccumulateSharpenPipeline extends Fsr2BasePipeline {
    private GlShaderProgram program;

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
        shaderDefines.put("FFX_HALF", GraphicsCapabilities.detectGpuVendor() == GpuVendor.NVIDIA ? "0" : "1");
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.COMPUTE, "/shader/fsr2/ffx_fsr2_accumulate_pass.ogl.glsl", true))
                        .addDefines(getShaderDefines(shaderDefines))
                        .name("fsr2_accumulate")
                        .build()
        );
        program.compile();
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        GlPipelineJobBuilders.ComputeJobBuilder jobBuilder =
                GlPipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> new Vector3f(
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
                        .resourceTypeSupplier(
                                context.config.flags.isEnableDisplayResolutionMotionVectors() ?
                                        () -> Fsr2PipelineResourceType.INPUT_MOTION_VECTORS :
                                        () -> context.isOddFrame() ?
                                                Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                                Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1

                        )
                        .binding(15)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2 :
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1
                        )
                        .binding(16)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LOCK_STATUS_2 :
                                        Fsr2PipelineResourceType.LOCK_STATUS_1
                        )
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
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LUMA_HISTORY_2 :
                                        Fsr2PipelineResourceType.LUMA_HISTORY_1
                        )
                        .binding(12)
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
                        .binding(0)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LOCK_STATUS_1 :
                                        Fsr2PipelineResourceType.LOCK_STATUS_2
                        )
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
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LUMA_HISTORY_1 :
                                        Fsr2PipelineResourceType.LUMA_HISTORY_2
                        )
                        .binding(4)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_accumulate_sharpen", jobBuilder.build());
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }

}
