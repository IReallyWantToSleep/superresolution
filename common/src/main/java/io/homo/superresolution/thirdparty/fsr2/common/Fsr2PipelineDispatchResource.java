package io.homo.superresolution.thirdparty.fsr2.common;

public record Fsr2PipelineDispatchResource(
        Fsr2PipelineResources resources,
        Fsr2ContextConfig config,
        Fsr2Dimensions dimensions,
        Fsr2DispatchDescription dispatchDescription
) {

}
