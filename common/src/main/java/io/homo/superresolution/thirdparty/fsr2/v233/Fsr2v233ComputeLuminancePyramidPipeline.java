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
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.math.Vector3i;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233ComputeLuminancePyramidPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v233ComputeLuminancePyramidPipeline(Fsr2Context context) {
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
        shaderDefines.put("FFX_HALF", "0");
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.COMPUTE, "/shader/fsr2v233/ffx_fsr2_compute_luminance_pyramid_pass.glsl", true))
                        .addDefines(getShaderDefines(shaderDefines))
                        .name("fsr2_compute_luminance_pyramid_v233")
                        .uniformBuffer("cbFSR2", 5, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformBuffer("cbSPD", 6, (int) context.fsr2SpdConstantsUBO.getSize())
                        .uniformSamplerTexture("r_input_color_jittered", 0)
                        .uniformStorageTexture("rw_spd_global_atomic", ShaderUniformAccess.Both, 1)
                        .uniformStorageTexture("rw_img_mip_5", ShaderUniformAccess.Both, 3)
                        .uniformStorageTexture("rw_auto_exposure", ShaderUniformAccess.Both, 4)
                        .build()
        );
        program.compile();
        PipelineJobBuilders.ComputeJobBuilder jobBuilder =
                PipelineJobBuilders.compute(program)
                        .workGroupSupplier(() -> {
                            int[] dispatchThreadGroupCountXY = new int[2];
                            int[] rectInfo = new int[]{
                                    0,
                                    0,
                                    context.dimensions.renderWidth(),
                                    context.dimensions.renderHeight()
                            };
                            int[] workGroupOffset = new int[]{
                                    rectInfo[0] / 64,
                                    rectInfo[1] / 64
                            };
                            int endIndexX = (rectInfo[0] + rectInfo[2] - 1) / 64;
                            int endIndexY = (rectInfo[1] + rectInfo[3] - 1) / 64;
                            dispatchThreadGroupCountXY[0] = endIndexX + 1 - workGroupOffset[0];
                            dispatchThreadGroupCountXY[1] = endIndexY + 1 - workGroupOffset[1];
                            return new Vector3i(
                                    dispatchThreadGroupCountXY[0],
                                    dispatchThreadGroupCountXY[1],
                                    1
                            );
                        });

        jobBuilder.resource(
                "cbFSR2",
                PipelineJobResource.UniformBuffer.create(context.fsr2ConstantsUBO)
        );
        jobBuilder.resource(
                "cbSPD",
                PipelineJobResource.UniformBuffer.create(context.fsr2SpdConstantsUBO)
        );

        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(0)
                        .access(PipelineResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.SPD_ATOMIC_COUNT.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SPD_ATOMIC_COUNT)
                        .binding(1)
                        .access(PipelineResourceAccess.Both)
                        .getResourceDescription(context)
        );
        //jobBuilder.resource(
        //        Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE.uavShaderName(),
        //        new Fsr2ShaderResource()
        //                .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE)
        //                .binding(2)
        //                .access(PipelineResourceAccess.Both)
        //                .getResourceDescription(context)
        //);
        GlTexture2D texture2D = ((GlTexture2D) context.resources.resource(Fsr2PipelineResourceType.SCENE_LUMINANCE).getResource());
        jobBuilder.resource(
                Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5)
                        .binding(3)
                        .access(PipelineResourceAccess.Both)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.AUTO_EXPOSURE.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.AUTO_EXPOSURE)
                        .binding(4)
                        .access(PipelineResourceAccess.Both)
                        .getResourceDescription(context)
        );
        pipeline.job("fsr2_compute_luminance_pyramid", jobBuilder.build());

    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }

}
