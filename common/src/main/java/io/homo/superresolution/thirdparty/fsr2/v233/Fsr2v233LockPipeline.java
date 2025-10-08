/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineResourceAccess;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import org.joml.Vector3i;
import io.homo.superresolution.thirdparty.fsr2.common.*;

import java.util.HashMap;

public class Fsr2v233LockPipeline extends Fsr2Pipeline {
    private GlShaderProgram program;

    public Fsr2v233LockPipeline(Fsr2Context context) {
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
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/fsr2v233/ffx_fsr2_lock_pass.glsl", true))
                        .addDefines(getShaderDefines(new HashMap<>()))
                        .name("fsr2_lock_v233")
                        .uniformBuffer("cbFSR2", 3, (int) context.fsr2ConstantsUBO.getSize())
                        .uniformSamplerTexture("r_lock_input_luma", 0)
                        .uniformStorageTexture("rw_new_locks", ShaderUniformAccess.Both, 1)
                        .uniformStorageTexture("rw_reconstructed_previous_nearest_depth", ShaderUniformAccess.Both, 2)
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
                Fsr2PipelineResourceType.LOCK_INPUT_LUMA.srvShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.LOCK_INPUT_LUMA)
                        .binding(0)
                        .access(PipelineResourceAccess.Read)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.NEW_LOCKS.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.NEW_LOCKS)
                        .binding(1)
                        .access(PipelineResourceAccess.Both)
                        .getResourceDescription(context)
        );
        jobBuilder.resource(
                Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH.uavShaderName(),
                new Fsr2ShaderResource()
                        .resourceType(Fsr2PipelineResourceType.RECONSTRUCTED_PREVIOUS_NEAREST_DEPTH)
                        .binding(2)
                        .access(PipelineResourceAccess.Both)
                        .getResourceDescription(context)
        );
        pipeline.job("fsr2_lock", jobBuilder.build());
    }

    @Override
    public void execute(Fsr2PipelineDispatchResource dispatchResource) {
        pipeline.execute(dispatchResource.commandBuffer());
    }

}
