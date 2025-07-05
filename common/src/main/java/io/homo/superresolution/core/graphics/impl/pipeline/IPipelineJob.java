package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.system.IRenderSystem;

public interface IPipelineJob {
    /**
     * 执行作业
     *
     * @param renderSystem 执行作业的渲染系统
     */
    void execute(IRenderSystem renderSystem);

    /**
     * 销毁作业资源
     */
    void destroy();
}
