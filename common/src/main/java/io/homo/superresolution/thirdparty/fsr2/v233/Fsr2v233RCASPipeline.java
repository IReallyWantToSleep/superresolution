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
import org.joml.Vector2i;
import org.joml.Vector3i;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233RCASPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v233RCASPipeline(Fsr2Context context) {
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
        return new Vector2i(64, 1);
    }

    @Override
    public void init() {
        if (program != null) {
            program.destroy();
        }
        program = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v233/ffx_fsr2_rcas_pass.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_rcas_v233")
                        .uniformBuffer("cbFSR2", 3, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformBuffer("cbRCAS", 4, (int) context.fsr2RcasConstantsUBO.getSize())
                        .uniformSamplerTexture("r_input_exposure", 0)
                        .uniformSamplerTexture("r_internal_upscaled_color", 1)
                        .uniformStorageTexture("rw_upscaled_output", ShaderResourceAccess.Both, 2)
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
                "cbRCAS",
                GrapeJobResource.UniformBuffer.create(context.fsr2RcasConstantsUBO)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INPUT_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(0)
                        .access(GrapeResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_1 :
                                        Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR_2
                        )
                        .resourceName(Fsr2PipelineResourceType.INTERNAL_UPSCALED_COLOR.srvShaderName())
                        .binding(1)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(GrapeResourceAccess.Read)
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
        pipeline.add("fsr2_rcas", jobBuilder.build());
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }


}
