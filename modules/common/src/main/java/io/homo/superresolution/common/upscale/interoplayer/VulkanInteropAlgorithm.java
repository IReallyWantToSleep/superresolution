package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.srapi.SRRenderApiType;

public abstract class VulkanInteropAlgorithm
        extends InteropAlgorithm<VulkanCommandContext> {
    @Override
    protected final SRRenderApiType executionApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    protected final InteropImplementation<VulkanCommandContext> createInteropImplementation(
            SRRenderApiType inputApi) {
        return switch (inputApi) {
            case OPENGL -> new OpenGLToVulkanInterop();
            case VULKAN -> new VulkanToVulkanInterop();
            default -> throw new UnsupportedOperationException(
                    "Unsupported input API for Vulkan providers: " + inputApi);
        };
    }
}
