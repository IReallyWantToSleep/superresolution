package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.fsr2.Fsr2Context;
import io.homo.superresolution.fsr2.Fsr2Dimensions;
import io.homo.superresolution.fsr2.Fsr2PipelineResources;
import io.homo.superresolution.fsr2.Fsr2PipelineResourcesDescription;

public class Fsr2AccumulatePipeline extends Fsr2BasePipeline {
    private GlComputeShaderProgram program;

    public Fsr2AccumulatePipeline(Fsr2Context resources) {
        super(resources);
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
                .addDefineText(context.config.flags.getShaderDefines())
                .addDefineText("FFX_HALF", "0")
                .setShaderName("fsr2_accumulate")
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
        for (String shaderResourceName : shaderBindingMap.keySet()) {
            Fsr2PipelineResources.Fsr2ResourceEntry pipelineResourcesDescription = getResourcesDescription(shaderResourceName);
            if (pipelineResourcesDescription.getResource() == null) continue;
            Integer binding = shaderBindingMap.get(shaderResourceName);
            if (pipelineResourcesDescription.type() == Fsr2PipelineResources.Fsr2ResourceType.UBO) {
                jobBuilder.resource(
                        GlPipelineResourceDescription.createUBOResource(
                                shaderResourceName,
                                (GlUniformBuffer<?>) pipelineResourcesDescription.getResource(),
                                binding
                        )
                );
            } else {
                jobBuilder.resource(
                        GlPipelineResourceDescription.createTextureResource(
                                shaderResourceName.startsWith("rw") ? GlPipelineResourceType.Image2D : GlPipelineResourceType.Sampler2D,
                                shaderResourceName,
                                (ITexture) pipelineResourcesDescription.getResource(),
                                shaderResourceName.startsWith("rw") ? GlPipelineResourceAccess.BOTH : GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.LinearClamp),
                                binding
                        )
                );
            }
        }
        pipeline.addJob("fsr2_accumulate", jobBuilder.build());
    }

    @Override
    protected void shaderBindings() {
        //SRV
        shaderBindingMap.put("r_input_exposure", 13);
        shaderBindingMap.put("r_dilated_reactive_masks", 14);
        if (context.config.flags.isEnableDisplayResolutionMotionVectors()) {
            shaderBindingMap.put("r_input_motion_vectors", 15);
        } else {
            shaderBindingMap.put("r_dilated_motion_vectors", 15);
        }
        shaderBindingMap.put("r_internal_upscaled_color", 16);
        shaderBindingMap.put("r_lock_status", 17);
        shaderBindingMap.put("r_prepared_input_color", 6);
        shaderBindingMap.put("r_lanczos_lut", 8);
        shaderBindingMap.put("r_upsample_maximum_bias_lut", 9);
        shaderBindingMap.put("r_imgMips", 10);
        shaderBindingMap.put("r_auto_exposure", 11);
        shaderBindingMap.put("r_luma_history", 12);
        //UAV
        shaderBindingMap.put("rw_internal_upscaled_color", 0);
        shaderBindingMap.put("rw_lock_status", 1);
        shaderBindingMap.put("rw_upscaled_output", 2);
        shaderBindingMap.put("rw_new_locks", 3);
        shaderBindingMap.put("rw_luma_history", 4);
        //CB
        //shaderBindingMap.put("cbFSR2", 18);
    }
}
