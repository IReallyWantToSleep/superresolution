package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.srapi.SRRenderApiType;

public abstract class D3D12InteropAlgorithm
        extends InteropAlgorithm<D3D12CommandContext> {
    @Override
    protected final SRRenderApiType executionApi() {
        return SRRenderApiType.D3D12;
    }

    @Override
    protected final InteropImplementation<D3D12CommandContext> createInteropImplementation(
            SRRenderApiType inputApi) {
        return switch (inputApi) {
            case OPENGL -> new OpenGLToD3D12Interop();
            case VULKAN -> new VulkanToD3D12Interop();
            default -> throw new UnsupportedOperationException(
                    "Unsupported input API for D3D12 providers: " + inputApi);
        };
    }
}
