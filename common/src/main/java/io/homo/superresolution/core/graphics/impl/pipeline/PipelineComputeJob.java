package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.math.Vector3i;

import java.util.function.Supplier;

public class PipelineComputeJob extends GpuComputeJob<PipelineComputeJob> implements IPipelineJob {
    protected Supplier<Vector3i> workGroupSizeSupplier = null;
    protected IShaderProgram<?> program = null;

    /**
     * 设置计算工作组大小
     *
     * @param x X维度工作组大小
     * @param y Y维度工作组大小
     * @param z Z维度工作组大小
     */
    public PipelineComputeJob workGroupSize(int x, int y, int z) {
        this.workGroupSizeSupplier = () -> new Vector3i(x, y, z);
        return this;
    }


    /**
     * 设置计算着色器
     *
     * @param program 计算着色器对象
     */
    public PipelineComputeJob computeProgram(IShaderProgram<?> program) {
        this.program = program;
        return this;
    }

    /**
     * 设置工作组大小提供器
     *
     * @param workGroupSizeSupplier 工作组大小提供函数
     */
    public PipelineComputeJob workGroupSizeSupplier(Supplier<Vector3i> workGroupSizeSupplier) {
        this.workGroupSizeSupplier = workGroupSizeSupplier;
        return this;
    }

    @Override
    public void execute(IRenderSystem renderSystem) {
        setupProgramResources(program);
        Vector3i workGroup = this.workGroupSizeSupplier.get();
        renderSystem.dispatchCompute(
                program,
                workGroup.x,
                workGroup.y,
                workGroup.z
        );
    }

    @Override
    public void destroy() {

    }
}
