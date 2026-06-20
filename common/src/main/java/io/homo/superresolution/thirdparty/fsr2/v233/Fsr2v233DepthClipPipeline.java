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
        if (program != null) {
            program.destroy();
        }
        program = RenderSystems.opengl().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v233/ffx_fsr2_depth_clip_pass.glsl", true))
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
                        .uniformStorageTexture("rw_dilated_reactive_masks", ShaderResourceAccess.Both, 1)
                        .uniformStorageTexture("rw_prepared_input_color", ShaderResourceAccess.Both, 2)
                        .build()
        );
        program.compile();
        computePipeline = GlComputePipeline.builder()
                .shader(program)
                .build(RenderSystems.opengl().device());
        workGroupSupplier = (() -> new Vector3i(
                calculateDispatchGrid(context.dimensions.renderWidth(), context.dimensions.renderHeight()),
                1
        ));

        uboBindings.put("cbFSR2", context.fsr2ConstantsUBO);


        shaderResourceBindings.put(
                Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(11)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceTypeSupplier(
                                () -> context.isOddFrame() ?
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_2 :
                                        Fsr2PipelineResourceType.INTERNAL_DILATED_MOTION_VECTORS_1
                        )
                        .resourceName(Fsr2PipelineResourceType.DILATED_MOTION_VECTORS.srvShaderName())
                        .binding(12)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.DILATED_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_DEPTH)
                        .binding(13)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_REACTIVE_MASK.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_REACTIVE_MASK)
                        .binding(3)
                        .access(ShaderResourceAccess.Read)

        );

        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_TRANSPARENCY_AND_COMPOSITION_MASK)
                        .binding(4)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .binding(5)
                        .access(ShaderResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))

        );
        shaderResourceBindings.put(
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
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_MOTION_VECTORS.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_MOTION_VECTORS)
                        .binding(7)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(8)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_DEPTH.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_DEPTH)
                        .binding(9)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_EXPOSURE.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_EXPOSURE)
                        .binding(10)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.DILATED_REACTIVE_MASKS)
                        .binding(1)
                        .access(ShaderResourceAccess.Both)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.PREPARED_INPUT_COLOR.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.PREPARED_INPUT_COLOR)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))
                        .binding(2)
                        .access(ShaderResourceAccess.Both)

        );
    }

    @Override
    protected Vector2i workGroupSize() {
        return new Vector2i(8, 8);
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        dispatchCompute(dispatchResource.commandBuffer());
    }

}
