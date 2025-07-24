package io.homo.superresolution.thirdparty.asr2;

public record Asr2PipelineDispatchResource(
        Asr2PipelineResources resources,
        Asr2ContextConfig config,
        Asr2Dimensions dimensions,
        Asr2DispatchDescription dispatchDescription
) {

}
