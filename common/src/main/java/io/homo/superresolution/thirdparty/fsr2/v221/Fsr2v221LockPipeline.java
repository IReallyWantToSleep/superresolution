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
import io.homo.superresolution.thirdparty.fsr2.common.*;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;

public class Fsr2v221LockPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v221LockPipeline(Fsr2Context context) {
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
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v221/ffx_fsr2_lock_pass.ogl.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_lock")
                        .uniformBuffer("cbFSR2", 3, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformSamplerTexture("r_lock_input_luma", 0)
                        .uniformStorageTexture("rw_new_locks", ShaderResourceAccess.Both, 1)
                        .uniformStorageTexture("rw_reconstructed_previous_nearest_depth", ShaderResourceAccess.Both, 2)
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
                Fsr2PipelineResourceType.LOCK_INPUT_LUMA.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_INPUT_LUMA)
                        .binding(0)
                        .access(ShaderResourceAccess.Read)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.NEW_LOCKS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.NEW_LOCKS)
                        .binding(1)
                        .access(ShaderResourceAccess.Both)

        );
        shaderResourceBindings.put(
                Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(2)
                        .access(ShaderResourceAccess.Both)

        );
    }

    @Override
    public Vector2i workGroupSize() {
        return new Vector2i(16, 16);
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        dispatchCompute(dispatchResource.commandBuffer());
    }

}
