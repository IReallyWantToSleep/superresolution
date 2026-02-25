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
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;

public class Fsr2v233ReconstructPreviousDepthPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;


    public Fsr2v233ReconstructPreviousDepthPipeline(Fsr2Context context) {
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
        if (program != null) {
            program.destroy();
        }
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v233/ffx_fsr2_reconstruct_previous_depth_pass.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_reconstruct_previous_depth_v233")
                        .uniformBuffer("cbFSR2", 12, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformSamplerTexture("r_input_motion_vectors", 7)
                        .uniformSamplerTexture("r_input_depth", 8)
                        .uniformSamplerTexture("r_input_color_jittered", 9)
                        .uniformSamplerTexture("r_input_exposure", 10)
                        .uniformSamplerTexture("r_luma_history", 11)
                        .uniformStorageTexture("rw_reconstructed_previous_nearest_depth", ShaderResourceAccess.Both, 0)
                        .uniformStorageTexture("rw_dilated_motion_vectors", ShaderResourceAccess.Both, 1)
                        .uniformStorageTexture("rw_dilatedDepth", ShaderResourceAccess.Both, 2)
                        .uniformStorageTexture("rw_prepared_input_color", ShaderResourceAccess.Both, 3)
                        .uniformStorageTexture("rw_luma_history", ShaderResourceAccess.Both, 4)
                        .uniformStorageTexture("rw_lock_input_luma", ShaderResourceAccess.Both, 6)
                        .build()
        );
        program.compile();
        GlComputePipeline computePipeline = (GlComputePipeline) GlComputePipeline.builder()
                .shader(program)
                .build(RenderSystems.opengl().device());
        GrapeJobBuilders.ComputeJobBuilder jobBuilder =
                GrapeJobBuilders.compute(computePipeline)
                        .workGroupSupplier(() -> new Vector3i(
                                calculateDispatchGrid(context.dimensions.renderWidth(), context.dimensions.renderHeight()),
                                1
                        ));

        jobBuilder.resource(
                "cbFSR2",
                GrapeJobResource.UniformBuffer.create(context.fsr2ConstantsUBO)
        );

        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS)
                        .binding(7)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_DEPTH)
                        .binding(8)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(9)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(10)
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
                        .binding(11)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(0)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1
                        )
                        .resourceName(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.uavShaderName())
                        .binding(1)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.DILATED_DEPTH.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_DEPTH)
                        .binding(2)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(3)
                        .access(GrapeResourceAccess.Both)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
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
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.LOCK_INPUT_LUMA.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_INPUT_LUMA)
                        .binding(6)
                        .access(GrapeResourceAccess.Both)
                        .getResourceDescription(context)
        );
        pipeline.add("fsr2_reconstruct_previous_depth", jobBuilder.build());

    }

    @Override
    public Vector2i workGroupSize() {
        return new Vector2i(8, 8);
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }

}
