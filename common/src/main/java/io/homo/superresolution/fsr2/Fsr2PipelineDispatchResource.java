package io.homo.superresolution.fsr2;

public record Fsr2PipelineDispatchResource(
        Fsr2PipelineResources resources,
        Fsr2ContextConfig config,
        Fsr2Dimensions dimensions,
        Fsr2DispatchDescription dispatchDescription
) {
    
}
