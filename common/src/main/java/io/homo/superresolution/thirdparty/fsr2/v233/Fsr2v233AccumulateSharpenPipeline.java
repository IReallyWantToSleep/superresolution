/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.thirdparty.fsr2.v233;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GpuVendor;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.grape.GrapeResourceAccess;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import org.joml.Vector2i;
import org.joml.Vector3i;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233AccumulateSharpenPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v233AccumulateSharpenPipeline(Fsr2Context context) {
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
    protected Vector2i workGroupSize() {
        return new Vector2i(16, 16);
    }

    @Override
    public void init() {
        if (program != null) {
            program.destroy();
        }
        HashMap<String, String> shaderDefines = new HashMap<>();
        shaderDefines.put("FFX_FSR2_OPTION_APPLY_SHARPENING", "1");
        shaderDefines.put("FFX_HALF", (!(GraphicsCapabilities.detectGpuVendor() == GpuVendor.Nvidia) && Fsr2DeviceCapabilities.isFp16Supported()) ? "1" : "0");
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v233/ffx_fsr2_accumulate_pass.glsl", true))
                        .addDefines(getShaderDefines(shaderDefines))
                        .name("fsr2_accumulate_v233")
                        .uniformBuffer("cbFSR2", 18, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformSamplerTexture("r_input_exposure", 13)
                        .uniformSamplerTexture("r_dilated_reactive_masks", 14)
                        .uniformSamplerTexture("r_input_motion_vectors", 15)
                        .uniformSamplerTexture("r_internal_upscaled_color", 16)
                        .uniformSamplerTexture("r_lock_status", 17)
                        .uniformSamplerTexture("r_prepared_input_color", 6)
                        .uniformSamplerTexture("r_lanczos_lut", 8)
                        .uniformSamplerTexture("r_upsample_maximum_bias_lut", 9)
                        .uniformSamplerTexture("r_imgMips", 10)
                        .uniformSamplerTexture("r_auto_exposure", 11)
                        .uniformSamplerTexture("r_luma_history", 12)
                        .uniformStorageTexture("rw_internal_upscaled_color", ShaderResourceAccess.Both, 0)
                        .uniformStorageTexture("rw_lock_status", ShaderResourceAccess.Both, 1)
                        .uniformStorageTexture("rw_upscaled_output", ShaderResourceAccess.Both, 2)
                        .uniformStorageTexture("rw_new_locks", ShaderResourceAccess.Both, 3)
                        .uniformStorageTexture("rw_luma_history", ShaderResourceAccess.Both, 4)
                        .build()
        );
        program.compile();
        GlComputePipeline computePipeline = (GlComputePipeline) GlComputePipeline.builder()
                .shader(program)
                .build(RenderSystems.opengl().device());
        GrapeJobBuilders.ComputeJobBuilder jobBuilder =
                GrapeJobBuilders.compute(computePipeline)
                        .workGroupSupplier(() -> new Vector3i(
                                calculateDispatchGrid(context.dimensions.screenWidth(), context.dimensions.screenHeight()),
                                1
                        ));

        jobBuilder.resource(
                "cbFSR2",
                GrapeJobResource.UniformBuffer.create(context.fsr2ConstantsUBO)
        );

        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(13)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS)
                        .binding(14)
                        .access(GrapeResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                !context.config.getFlags().isEnableDisplayResolutionMotionVectors() ?
                        Fsr2PipelineResourceType.INPUT_MOTION_VECTORS.srvShaderName() :
                        Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                !context.config.getFlags().isEnableDisplayResolutionMotionVectors() ?
                                        () -> Fsr2PipelineResourceType.INPUT_MOTION_VECTORS :
                                        () -> context.isOddFrame() ?
                                                Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                                Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1
                        )
                        .resourceName(
                                !context.config.getFlags().isEnableDisplayResolutionMotionVectors() ?
                                        Fsr2PipelineResourceType.INPUT_MOTION_VECTORS.srvShaderName() :
                                        Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName()
                        )
                        .binding(15)
                        .sampler(
                                context.config.getFlags().isEnableDisplayResolutionMotionVectors() ?
                                        GlSampler.create(GlSampler.SamplerType.LinearClamp) :
                                        GlSampler.create(GlSampler.SamplerType.NearestClamp)
                        )
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2 :
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1
                        )
                        .resourceName(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.srvShaderName())
                        .binding(16)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LOCK_STATUS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LOCK_STATUS_2 :
                                        Fsr2PipelineResourceType.LOCK_STATUS_1
                        )
                        .resourceName(Fsr2PipelineResourceType.LOCK_STATUS.srvShaderName())
                        .binding(17)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(6)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LANCZOS_LUT.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LANCZOS_LUT)
                        .binding(8)
                        .access(GrapeResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.UPSAMPLE_MAXIMUM_BIAS_LUT.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.UPSAMPLE_MAXIMUM_BIAS_LUT)
                        .binding(9)
                        .access(GrapeResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.SCENE_LUMINANCE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE)
                        .binding(10)
                        .access(GrapeResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.AUTO_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.AUTO_EXPOSURE)
                        .binding(11)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LUMA_HISTORY.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LUMA_HISTORY_2 :
                                        Fsr2PipelineResourceType.LUMA_HISTORY_1
                        )
                        .resourceName(Fsr2PipelineResourceType.LUMA_HISTORY.srvShaderName())
                        .binding(12)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1 :
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2
                        )
                        .resourceName(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.uavShaderName())
                        .binding(0)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LOCK_STATUS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LOCK_STATUS_1 :
                                        Fsr2PipelineResourceType.LOCK_STATUS_2
                        )
                        .resourceName(Fsr2PipelineResourceType.LOCK_STATUS.uavShaderName())
                        .binding(1)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.UPSCALED_OUTPUT.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.UPSCALED_OUTPUT)
                        .binding(2)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.NEW_LOCKS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.NEW_LOCKS)
                        .binding(3)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LUMA_HISTORY.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.LUMA_HISTORY_1 :
                                        Fsr2PipelineResourceType.LUMA_HISTORY_2
                        )
                        .resourceName(Fsr2PipelineResourceType.LUMA_HISTORY.uavShaderName())
                        .binding(4)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        pipeline.add("fsr2_accumulate_sharpen", jobBuilder.build());
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }

}
