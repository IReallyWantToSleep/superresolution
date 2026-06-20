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

package io.homo.superresolution.thirdparty.fsr2.v221;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;

public class Fsr2v221ComputeLuminancePyramidPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v221ComputeLuminancePyramidPipeline(Fsr2Context context) {
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
        HashMap<String, String> shaderDefines = new HashMap<>();
        shaderDefines.put("FFX_HALF", "0");
        program = RenderSystems.opengl().device().createShaderProgram(
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v221/ffx_fsr2_compute_luminance_pyramid_pass.ogl.glsl", true))
                        .addDefines(getShaderDefines(shaderDefines))
                        .name("fsr2_compute_luminance_pyramid")
                        .uniformBuffer("cbFSR2", 5, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformBuffer("cbSPD", 6, (int) context.fsr2SpdConstantsUBO.getSize())
                        .uniformSamplerTexture("r_input_color_jittered", 0)
                        .uniformStorageTexture("rw_spd_global_atomic", ShaderResourceAccess.Both, 1)
                        .uniformStorageTexture("rw_img_mip_5", ShaderResourceAccess.Both, 3)
                        .uniformStorageTexture("rw_auto_exposure", ShaderResourceAccess.Both, 4)
                        .build()
        );
        program.compile();
        computePipeline = GlComputePipeline.builder()
                .shader(program)
                .build(RenderSystems.opengl().device());
        workGroupSupplier = (() -> {
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

        uboBindings.put("cbFSR2", context.fsr2ConstantsUBO);
        uboBindings.put("cbSPD", context.fsr2SpdConstantsUBO);

        shaderResourceBindings.put(
                Fsr2PipelineResourceType.INPUT_COLOR.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.INPUT_COLOR)
                        .binding(0)
                        .access(ShaderResourceAccess.Read)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.SPD_ATOMIC_COUNT.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SPD_ATOMIC_COUNT)
                        .binding(1)
                        .access(ShaderResourceAccess.Both)

        );
        //shaderResourceBindings.put(
        //        Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE.uavShaderName(),
        //        new Fsr2ShaderResource()
        //                .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_SHADING_CHANGE)
        //                .binding(2)
        //                .access(PipelineResourceAccess.Both)
        //                
        //);
        GlTexture2D texture2D = ((GlTexture2D) context.resources.resource(Fsr2PipelineResourceType.SCENE_LUMINANCE).getResource());
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.SCENE_LUMINANCE_MIPMAP_5)
                        .binding(3)
                        .access(ShaderResourceAccess.Both)
                        .sampler(GlSampler.create(GlSampler.SamplerType.LinearClamp))

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.AUTO_EXPOSURE.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.AUTO_EXPOSURE)
                        .binding(4)
                        .access(ShaderResourceAccess.Both)

        );
    }

    @Override
    protected Vector2i workGroupSize() {
        return new Vector2i(256, 1);
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        dispatchCompute(dispatchResource.commandBuffer());
    }

}
