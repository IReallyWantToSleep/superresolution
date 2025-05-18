package io.homo.superresolution.fsr2.pipelines;

import io.homo.superresolution.core.GpuVendor;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.fsr2.*;

import java.util.HashMap;

public class Fsr2ComputeLuminancePyramidPipeline extends Fsr2BasePipeline {
    private GlComputeShaderProgram program;

    public Fsr2ComputeLuminancePyramidPipeline(Fsr2Context context) {
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

        program = GlComputeShaderProgram.create()
                .addDefineText(getShaderDefines(shaderDefines))
                .setShaderName("fsr2_compute_luminance_pyramid")
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/fsr2/ffx_fsr2_compute_luminance_pyramid_pass.ogl.glsl", true))
                .build()
                .compileShader();

    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        GlPipelineJobBuilders.ComputeJobBuilder jobBuilder =
                GlPipelineJobBuilders.compute(program)
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
                            return new Vec3(
                                    dispatchThreadGroupCountXY[0],
                                    dispatchThreadGroupCountXY[1],
                                    1
                            );
                        });

        jobBuilder.resource(
                GlPipelineResourceDescription.createUBOResource(
                        "cbFSR2",
                        context.fsr2ConstantsUBO,
                        5
                )
        );
        jobBuilder.resource(
                GlPipelineResourceDescription.createUBOResource(
                        "cbSPD",
                        context.fsr2SpdConstantsUBO,
                        6
                )
        );

        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(0)
                        .access(GlPipelineResourceAccess.READ)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SPD_ATOMIC_COUNT)
                        .binding(1)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        //jobBuilder.resource(
        //        new Fsr2ShaderResource()
        //                .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE)
        //                .binding(2)
        //                .access(GlPipelineResourceAccess.BOTH)
        //                .getResourceDescription(context)
        //);
        GlTexture2D texture2D = ((GlTexture2D) context.resources.resource(Fsr2PipelineResourceType.SCENE_LUMINANCE).getResource());
        jobBuilder.resource(
                GlPipelineResourceDescription.createTextureResource(
                        GlPipelineResourceType.Image2D,
                        Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5.uavShaderName(),
                        texture2D.getMipView(Math.min(5, texture2D.getMipmapLevel())),
                        GlPipelineResourceAccess.BOTH,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        3
                )
        );
        jobBuilder.resource(
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.AUTO_EXPOSURE)
                        .binding(4)
                        .access(GlPipelineResourceAccess.BOTH)
                        .getResourceDescription(context)
        );
        pipeline.addJob("fsr2_compute_luminance_pyramid", jobBuilder.build());
        pipeline.scheduleJobs();
        pipeline.executeJobs();
    }

}
