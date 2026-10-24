package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class VulkanToVulkanInterop implements InteropImplementation<VulkanCommandContext> {
    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Vulkan-to-Vulkan interop is not implemented yet");
    }

    @Override
    public SRRenderApiType inputApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public SRRenderApiType executionApi() {
        return SRRenderApiType.VULKAN;
    }

    @Override
    public void initialize(InitializationDescription description,
                           List<InteropResourceRequirement> requirements) {
        throw unsupported();
    }

    @Override
    public InteropResourceAccess resourceAccess() {
        throw unsupported();
    }

    @Override
    public boolean validateInputResources(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public Map<InteropResourceType, InteropResourceDescription> resourceDescriptions() {
        return Map.of();
    }

    @Override
    public boolean resourcesNeedRebuild(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public void rebuildForDispatch(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public InteropDispatchResource<VulkanCommandContext> beginDispatch(DispatchResource resource) {
        throw unsupported();
    }

    @Override
    public void completeDispatch(InteropDispatchResource<VulkanCommandContext> resource, boolean success) {
        throw unsupported();
    }

    @Override
    public void executeCommands(Consumer<VulkanCommandContext> recorder) {
        throw unsupported();
    }

    @Override
    public ITexture outputTexture() {
        return null;
    }

    @Override
    public void resize(int width, int height) {
        throw unsupported();
    }

    @Override
    public void awaitUsers() {
    }

    @Override
    public void destroy() {
    }
}
