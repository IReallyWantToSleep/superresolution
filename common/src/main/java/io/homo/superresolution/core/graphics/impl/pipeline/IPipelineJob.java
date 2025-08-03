package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.system.IRenderSystem;

public interface IPipelineJob {
    /**
     * 把作业提交到命令缓冲区
     *
     * @param commandBuffer 目标命令缓冲区
     */
    void execute(ICommandBuffer commandBuffer);

    /**
     * 销毁作业资源
     */
    void destroy();
}
