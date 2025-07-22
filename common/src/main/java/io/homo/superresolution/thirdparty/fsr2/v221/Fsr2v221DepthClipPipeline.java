package io.homo.superresolution.thirdparty.fsr2.v221;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v221DepthClipPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;


    public Fsr2v221DepthClipPipeline(Fsr2Context context) {
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
                ShaderDescription.compute(new ShaderSource(ShaderType.COMPUTE, "/shader/fsr2v221/ffx_fsr2_depth_clip_pass.ogl.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_depth_clip")
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
                        14
                )
        );


        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(11)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1
                        )
                        .binding(12)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_DEPTH)
                        .binding(13)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK)
                        .binding(3)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );

        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK)
                        .binding(4)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(5)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2
                        )
                        .binding(6)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS)
                        .binding(7)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(8)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_DEPTH)
                        .binding(9)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(10)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS)
                        .binding(1)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(2)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_depth_clip", jobBuilder.build());
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }

}
