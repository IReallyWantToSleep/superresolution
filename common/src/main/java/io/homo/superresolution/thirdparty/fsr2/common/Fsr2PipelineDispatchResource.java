package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;

public record Fsr2PipelineDispatchResource(
        Fsr2PipelineResources resources,
        Fsr2ContextConfig config,
        Fsr2Dimensions dimensions,
        Fsr2DispatchDescription dispatchDescription,
        ICommandBuffer commandBuffer
) {

}
