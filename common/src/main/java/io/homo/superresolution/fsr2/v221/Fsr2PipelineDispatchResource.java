package io.homo.superresolution.fsr2.v221;

public record Fsr2PipelineDispatchResource(
        Fsr2PipelineResources resources,
        Fsr2ContextConfig config,
        Fsr2Dimensions dimensions,
        Fsr2DispatchDescription dispatchDescription
) {

}
