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

package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Fsr2Pipeline {
    protected final Fsr2Context context;
    protected final Map<String, IBuffer> uboBindings = new LinkedHashMap<>();
    protected final Map<String, Fsr2ShaderResource> shaderResourceBindings = new LinkedHashMap<>();
    protected ComputePipeline computePipeline;
    protected Supplier<Vector3i> workGroupSupplier;

    public Fsr2Pipeline(Fsr2Context context) {
        this.context = context;
    }


    public abstract void resize(Fsr2Dimensions size);

    public abstract void destroy();

    public abstract void init();

    protected Vector2i workGroupSize() {
        return new Vector2i(8, 8);
    }

    protected Vector2i calculateDispatchGrid(int w, int h) {
        Vector2i workGroupSize = workGroupSize();
        return new Vector2i((w + workGroupSize.x - 1) / workGroupSize.x, (h + workGroupSize.y - 1) / workGroupSize.y);
    }

    protected Fsr2PipelineResources.Fsr2ResourceEntry getResourcesDescription(String name) {
        if (!context.resources.shaderNameMap().containsKey(name)) {
            throw new RuntimeException(name);
        }
        return context.resources.resource(context.resources.shaderNameMap().get(name));
    }

    private String bool(boolean b) {
        return String.valueOf(b ? 1 : 0);
    }

    public Map<String, String> getShaderDefines(Map<String, String> override) {
        Map<String, String> defines = new HashMap<>();
        defines.put("FFX_FSR2_OPTION_HDR_COLOR_INPUT", bool(context.config.flags.isEnableHighDynamicRange()));
        defines.put("FFX_FSR2_OPTION_LOW_RESOLUTION_MOTION_VECTORS", bool(!context.config.flags.isEnableDisplayResolutionMotionVectors()));
        defines.put("FFX_FSR2_OPTION_JITTERED_MOTION_VECTORS", bool(context.config.flags.isEnableMotionVectorsJitterCancellation()));
        defines.put("FFX_FSR2_OPTION_INVERTED_DEPTH", bool(context.config.flags.isEnableDepthInverted()));

        defines.put("FFX_FSR2_OPTION_APPLY_SHARPENING", bool(false));
        defines.put("FFX_FSR2_OPTION_REPROJECT_USE_LANCZOS_TYPE", bool(false));

        defines.put("FFX_FSR2_OPTION_UPSAMPLE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_ACCUMULATE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_REPROJECT_SAMPLERS_USE_DATA_HALF", "1");
        defines.put("FFX_FSR2_OPTION_POSTPROCESSLOCKSTATUS_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_UPSAMPLE_USE_LANCZOS_TYPE", "2");
        defines.put("FFX_GLSL", bool(true));
        defines.put("FFX_GPU", bool(true));
        defines.put("FFX_HALF", bool(Fsr2DeviceCapabilities.isFp16Supported()));

        Vector2i workGroupSize = workGroupSize();
        defines.put("FFX_FSR2_THREAD_GROUP_WIDTH", String.valueOf(workGroupSize.x));
        defines.put("FFX_FSR2_THREAD_GROUP_HEIGHT", String.valueOf(workGroupSize.y));

        if (override != null) {
            defines.putAll(override);
        }
        return defines;
    }

    public abstract void execute(Fsr2PipelineDispatchResource dispatchResource);

    protected void dispatchCompute(ICommandBuffer commandBuffer) {
        for (var entry : uboBindings.entrySet()) {
            computePipeline.descriptorSet().uniformBuffer(entry.getKey(), entry.getValue());
        }
        for (var entry : shaderResourceBindings.entrySet()) {
            entry.getValue().bindToDescriptorSet(entry.getKey(), computePipeline.descriptorSet(), context);
        }
        computePipeline.descriptorSet().update();
        Vector3i wg = workGroupSupplier.get();
        commandBuffer.bindPipeline(computePipeline);
        commandBuffer.dispatch(wg.x, wg.y, wg.z);
    }
}

