package io.homo.superresolution.thirdparty.fsr2.v233;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineResourceAccess;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import io.homo.superresolution.core.math.Vector3i;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233DepthClipPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;


    public Fsr2v233DepthClipPipeline(Fsr2Context context) {
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
                ShaderDescription.compute(new ShaderSource(ShaderType.COMPUTE, "/shader/fsr2v233/ffx_fsr2_depth_clip_pass.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_depth_clip_v233")
                        .uniformBuffer("cbFSR2", 14, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformSamplerTexture("r_reconstructed_previous_nearest_depth", 11)
                        .uniformSamplerTexture("r_dilated_motion_vectors", 12)
                        .uniformSamplerTexture("r_dilatedDepth", 13)
                        .uniformSamplerTexture("r_reactive_mask", 3)
                        .uniformSamplerTexture("r_transparency_and_composition_mask", 4)
                        .uniformSamplerTexture("r_prepared_input_color", 5)
                        .uniformSamplerTexture("r_dilated_motion_vectors", 6)
                        .uniformSamplerTexture("r_input_motion_vectors", 7)
                        .uniformSamplerTexture("r_input_color_jittered", 8)
                        .uniformSamplerTexture("r_input_depth", 9)
                        .uniformSamplerTexture("r_input_exposure", 10)
                        .uniformStorageTexture("rw_dilated_reactive_masks", ShaderUniformAccess.Both, 1)
                        .uniformStorageTexture("rw_prepared_input_color", ShaderUniformAccess.Both, 2)
                        .build()
        );
        program.compile();
        PipelineJobBuilders.ComputeJobBuilder jobBuilder =
                PipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> new Vector3i(
                                (context.dimensions.renderWidth() + (7)) / 8,
                                (context.dimensions.renderHeight() + (7)) / 8,
                                1
                        ));

        jobBuilder.resource(
                "cbFSR2",
                PipelineJobResource.UniformBuffer.create(context.fsr2ConstantsUBO)
        );


        jobBuilder.resource(
                Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(11)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1
                        )
                        .resourceName(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName())
                        .binding(12)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_DEPTH)
                        .binding(13)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_REACTIVE_MASK.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK)
                        .binding(3)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );

        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK)
                        .binding(4)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(5)
                        .access(PipelineResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2
                        )
                        .resourceName(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName())
                        .binding(6)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS)
                        .binding(7)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(8)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_DEPTH)
                        .binding(9)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(10)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS)
                        .binding(1)
                        .access(PipelineResourceAccess.Both)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .binding(2)
                        .access(PipelineResourceAccess.Both)
                        .getResourceDescription(context)
        );
        pipeline.job("fsr2_depth_clip", jobBuilder.build());
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }

}
