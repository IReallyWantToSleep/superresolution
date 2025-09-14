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

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.math.Vector3i;

import java.util.Objects;
import java.util.function.Supplier;

public class PipelineComputeJob extends GpuComputeJob<PipelineComputeJob> implements IPipelineJob {
    protected Supplier<Vector3i> workGroupSizeSupplier = null;
    protected IShaderProgram<?> program = null;

    public PipelineComputeJob workGroupSize(int x, int y, int z) {
        if (x <= 0 || y <= 0 || z <= 0) {
            throw new IllegalArgumentException("工作组大小必须大于0");
        }
        this.workGroupSizeSupplier = () -> new Vector3i(x, y, z);
        return this;
    }

    public PipelineComputeJob computeProgram(IShaderProgram<?> program) {
        this.program = Objects.requireNonNull(program, "计算着色器不能为null");
        return this;
    }

    public PipelineComputeJob workGroupSizeSupplier(Supplier<Vector3i> workGroupSizeSupplier) {
        this.workGroupSizeSupplier = Objects.requireNonNull(workGroupSizeSupplier, "工作组提供器不能为null");
        return this;
    }

    @Override
    public void execute(ICommandBuffer commandBuffer) {
        Objects.requireNonNull(workGroupSizeSupplier, "工作组大小提供器未设置");
        Objects.requireNonNull(program, "计算着色器未设置");

        setupProgramResources(program);
        Vector3i workGroup = workGroupSizeSupplier.get();

        if (workGroup.x <= 0 || workGroup.y <= 0 || workGroup.z <= 0) {
            throw new IllegalStateException("无效的工作组大小: " + workGroup);
        }

        commandBuffer.getEncoder().dispatchCompute(commandBuffer, program, workGroup.x, workGroup.y, workGroup.z);
    }

    @Override
    public void destroy() {
        program = null;
        workGroupSizeSupplier = null;
    }
}